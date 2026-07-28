package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * NUEVO (doc sección 5.2). Sincroniza el catálogo del Tenant con su tienda
 * WooCommerce (plan Catálogo/Ecommerce, doc secciones 3 y 5.3): trae
 * productos (nombre, precio, foto, link de compra, stock) vía la REST API
 * pública de WooCommerce (v3) y los deja en la tabla local Product, que es lo
 * que después usa AiResponseService para recomendar productos y PaymentService
 * para armar el carrito del link de pago.
 *
 * Autenticación: WooCommerce REST API acepta HTTP Basic Auth con el
 * consumer key/secret del comercio cuando se consulta por HTTPS (que es lo
 * esperado en producción). Las credenciales son del propio comercio, cargadas
 * por el dueño desde el panel (ver Tenant.wooCommerceUrl/ConsumerKey/Secret) -
 * nunca credenciales nuestras.
 *
 * Sincronización manual por ahora (botón "Sincronizar" en el panel, ver
 * CatalogController) - no hay job automático todavía. La sincronización hace
 * upsert por externalId (id del producto en WooCommerce) y desactiva
 * (active=false) los productos que ya dejaron de venir en la tienda, sin
 * borrarlos (para no perder el historial de qué se vendió con ese producto).
 */
@Slf4j
@Service
public class CatalogSyncService {

    private static final int PAGE_SIZE = 50;
    // Tope defensivo: evita loops infinitos si una tienda devolviera páginas
    // "de mentira" sin fin (bug del lado de WooCommerce/plugin raro).
    private static final int MAX_PAGINAS = 40;

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CatalogSyncService(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Trae el catálogo completo desde WooCommerce y lo deja sincronizado en
     * la tabla Product de este tenant.
     *
     * @return cantidad de productos traídos (creados o actualizados).
     * @throws CatalogSyncException si el plan no es CATALOGO, no hay
     *                               credenciales cargadas, o falla la conexión
     *                               con la tienda (mensaje pensado para
     *                               mostrarse tal cual en el panel).
     */
    @Transactional
    public int sincronizar(Tenant tenant) {
        if (tenant.getPlan() != TenantPlan.CATALOGO) {
            throw new CatalogSyncException("La sincronización de catálogo solo está disponible en el plan Catálogo.");
        }
        if (!tenant.isWooCommerceConfigurado()) {
            throw new CatalogSyncException("Este negocio todavía no configuró su tienda WooCommerce.");
        }

        String baseUrl = tenant.getWooCommerceUrl().replaceAll("/+$", "");
        String credenciales = Base64.getEncoder().encodeToString(
                (tenant.getWooCommerceConsumerKey() + ":" + tenant.getWooCommerceConsumerSecret())
                        .getBytes(StandardCharsets.UTF_8));

        Set<Long> idsVistos = new HashSet<>();
        int total = 0;

        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            JsonNode productos = pedirPagina(baseUrl, credenciales, pagina, tenant.getId());
            if (!productos.isArray() || productos.isEmpty()) {
                break;
            }

            for (JsonNode nodo : productos) {
                total++;
                idsVistos.add(guardarProducto(tenant, nodo));
            }

            if (productos.size() < PAGE_SIZE) {
                break;
            }
        }

        desactivarNoVistos(tenant, idsVistos);
        log.info("Catálogo sincronizado para tenant {}: {} productos", tenant.getId(), total);
        return total;
    }

    private JsonNode pedirPagina(String baseUrl, String credenciales, int pagina, Long tenantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/wp-json/wc/v3/products?per_page=" + PAGE_SIZE + "&page=" + pagina))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + credenciales)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new CatalogSyncException("La tienda rechazó las credenciales. Revisá la consumer key y el secret.");
            }
            if (response.statusCode() == 404) {
                throw new CatalogSyncException("No se encontró la API de WooCommerce en esa URL. Revisá que sea la URL base de la tienda.");
            }
            if (response.statusCode() != 200) {
                log.warn("WooCommerce respondió {} para tenant {} (página {}): {}",
                        response.statusCode(), tenantId, pagina, response.body());
                throw new CatalogSyncException("La tienda respondió con un error. Probá de nuevo en unos minutos.");
            }

            return objectMapper.readTree(response.body());
        } catch (CatalogSyncException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de conexión sincronizando catálogo de tenant {}", tenantId, e);
            throw new CatalogSyncException("No se pudo conectar con la tienda. Revisá la URL.");
        }
    }

    private Long guardarProducto(Tenant tenant, JsonNode nodo) {
        long externalId = nodo.path("id").asLong();

        Product producto = productRepository.findByTenantAndExternalId(tenant, externalId)
                .orElseGet(Product::new);
        producto.setTenant(tenant);
        producto.setExternalId(externalId);
        producto.setName(nodo.path("name").asText(""));
        producto.setPrice(parsePrecio(nodo.path("price").asText("")));
        producto.setPurchaseUrl(textoONull(nodo.path("permalink")));

        // WooCommerce no distingue categoría/subcategoría en el endpoint de
        // productos (son todas "categories" planas, sin jerarquía resuelta) -
        // se usa la primera como category (ver Javadoc de Product). subcategory
        // no se toca acá: si el dueño ya la había cargado a mano, se conserva.
        JsonNode categorias = nodo.path("categories");
        if (categorias.isArray() && !categorias.isEmpty()) {
            producto.setCategory(textoONull(categorias.get(0).path("name")));
        }

        JsonNode imagenes = nodo.path("images");
        producto.setImageUrl(imagenes.isArray() && !imagenes.isEmpty()
                ? textoONull(imagenes.get(0).path("src")) : null);

        JsonNode stock = nodo.path("stock_quantity");
        producto.setStockQuantity(stock.isNumber() ? stock.asInt() : null);

        producto.setActive("publish".equals(nodo.path("status").asText()));
        producto.setUpdatedAt(Instant.now());

        productRepository.save(producto);
        return externalId;
    }

    private void desactivarNoVistos(Tenant tenant, Set<Long> idsVistos) {
        for (Product producto : productRepository.findByTenant(tenant)) {
            if (producto.getExternalId() != null && producto.isActive() && !idsVistos.contains(producto.getExternalId())) {
                producto.setActive(false);
                productRepository.save(producto);
            }
        }
    }

    private BigDecimal parsePrecio(String precio) {
        if (precio == null || precio.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(precio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String textoONull(JsonNode nodo) {
        return (nodo == null || nodo.isMissingNode() || nodo.isNull()) ? null : nodo.asText();
    }

    public List<Product> listarProductos(Tenant tenant) {
        return productRepository.findByTenant(tenant);
    }

    public List<Product> listarProductosActivos(Tenant tenant) {
        return productRepository.findByTenantAndActiveTrue(tenant);
    }

    /** Ver AiResponseService: decide si el catálogo completo entra en el prompt o si hay que usar buscar_productos. */
    public long contarProductosActivos(Tenant tenant) {
        return productRepository.countByTenantAndActiveTrue(tenant);
    }

    /** Categorías activas del tenant (para orientar al modelo antes de que llame a buscar_productos). */
    public List<String> listarCategorias(Tenant tenant) {
        return productRepository.listarCategoriasDistintas(tenant);
    }

    /**
     * Búsqueda filtrada del catálogo (ver AiResponseService.ejecutarBuscarProductos)
     * para catálogos grandes: en vez de volcar todos los productos al prompt,
     * el modelo llama a esta tool con categoría/subcategoría/texto para traer
     * solo lo relevante. limite acota cuántas filas puede traer como máximo -
     * si el filtro queda demasiado amplio, se corta ahí en vez de mandarle al
     * modelo un catálogo entero disfrazado de "búsqueda".
     */
    public List<Product> buscarProductos(Tenant tenant, String categoria, String subcategoria, String texto, int limite) {
        return productRepository.buscar(
                tenant,
                blankANull(categoria),
                blankANull(subcategoria),
                blankANull(texto),
                PageRequest.of(0, limite));
    }

    private String blankANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /**
     * Alta manual de un producto (ver CatalogController) - para negocios sin
     * tienda online (WooCommerce) que igual quieren usar el catálogo del plan
     * Catálogo, o para completar/corregir productos puntuales entre
     * sincronizaciones. externalId queda null (no viene de WooCommerce), así
     * que una sincronización posterior nunca lo toca ni lo desactiva (ver
     * sincronizar/desactivarNoVistos, que solo operan sobre externalId != null).
     */
    @Transactional
    public Product crearProductoManual(Tenant tenant, String name, BigDecimal price, String category, String subcategory, Integer stockQuantity) {
        Product producto = new Product();
        producto.setTenant(tenant);
        producto.setName(name);
        producto.setPrice(price);
        producto.setCategory(blankANull(category));
        producto.setSubcategory(blankANull(subcategory));
        producto.setStockQuantity(stockQuantity);
        producto.setActive(true);
        producto.setUpdatedAt(Instant.now());
        return productRepository.save(producto);
    }

    /**
     * Edita un producto ya existente de este tenant (manual o sincronizado -
     * si es sincronizado, la próxima sincronización puede pisar name/price/etc.
     * con lo que traiga WooCommerce, pero subcategory nunca lo toca la
     * sincronización, ver Javadoc de Product).
     *
     * @throws NoSuchElementException si el producto no existe o no es de este tenant.
     */
    @Transactional
    public Product actualizarProducto(Tenant tenant, Long productId, String name, BigDecimal price, String category, String subcategory, Integer stockQuantity, boolean active) {
        Product producto = productRepository.findById(productId)
                .filter(p -> p.getTenant().getId().equals(tenant.getId()))
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productId));
        producto.setName(name);
        producto.setPrice(price);
        producto.setCategory(blankANull(category));
        producto.setSubcategory(blankANull(subcategory));
        producto.setStockQuantity(stockQuantity);
        producto.setActive(active);
        producto.setUpdatedAt(Instant.now());
        return productRepository.save(producto);
    }

    /**
     * @throws NoSuchElementException si el producto no existe o no es de este tenant.
     */
    @Transactional
    public void eliminarProducto(Tenant tenant, Long productId) {
        Product producto = productRepository.findById(productId)
                .filter(p -> p.getTenant().getId().equals(tenant.getId()))
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productId));
        productRepository.delete(producto);
    }
}
