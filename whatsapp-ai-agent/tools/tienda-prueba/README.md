# Tienda de prueba (300 productos)

Dataset y script para simular una tienda real de ropa/accesorios ("Tienda Andes") y probar cómo responde el bot con un catálogo grande - pensado para validar la tool `buscar_productos` (ver `AiResponseService`) antes de salir a vender.

Contenido:

- `productos.json` — 300 productos generados con variaciones de color/talla, repartidos en 3 categorías y 10 subcategorías (Ropa: Poleras/Pantalones/Chaquetas/Vestidos; Calzado: Zapatillas/Botines; Accesorios: Gorros/Bufandas/Mochilas/Cinturones).
- `contexto-negocio.txt` — texto listo para pegar en el campo "Contexto del negocio" del panel (horarios, envíos, cambios, tono).
- `cargar-productos.mjs` — script Node que crea los 300 productos vía la API (uno por uno, con el endpoint manual de alta de productos - no hace falta WooCommerce).

## Pasos

1. **Crear el tenant de prueba** desde el panel (Negocios → Nuevo): nombre "Tienda Andes", plan **Catálogo**, y un número de WhatsApp (podés usar el mismo sandbox de Twilio si es el único tenant de prueba que vas a correr a la vez - el sandbox es un solo número compartido, así que solo puede resolver a UN tenant por vez mientras no tengas números dedicados).
2. Copiar `contexto-negocio.txt` y pegarlo en "Contexto del negocio" del tenant, desde el panel.
3. Anotar el `id` del tenant recién creado (aparece en la URL del panel, `/tenants/<id>/...`).
4. Correr el script de carga:

   ```bash
   cd whatsapp-ai-agent/tools/tienda-prueba
   API_URL=https://tu-backend.onrender.com \
   ADMIN_USERNAME=admin \
   ADMIN_PASSWORD=tu-panel-password \
   TENANT_ID=<el-id-del-paso-3> \
   node cargar-productos.mjs
   ```

   Tarda un par de minutos (300 requests uno por uno). Al terminar, revisá en el panel (`/tenants/<id>/catalogo`) que aparezcan los 300 productos.

5. **Probar por WhatsApp** (sandbox, ver `DEPLOY.md` sección 6): mandale al bot cosas como:
   - "Hola, tienen poleras negras?" → debería usar `buscar_productos` con categoría/texto y traer solo poleras negras, no las 300.
   - "Busco zapatillas talla 42" → categoría Calzado, subcategoría Zapatillas.
   - "Qué categorías tienen?" → el bot ya conoce la lista de categorías (se la pasamos en el prompt) sin tener que buscar.
   - Armar un carrito y pedir el link de pago (necesita credenciales de Flow cargadas en el tenant para funcionar de punta a punta - si no las tenés, alcanza con ver que arma bien el carrito con los ids correctos antes de fallar en el paso de Flow).
   - Preguntar por algo que no vendés (ej. "tienen notebooks?") → debería decir que no, sin inventar.

6. **Qué mirar**: que el bot elija bien la categoría/subcategoría antes de buscar (no debería traer zapatillas cuando piden poleras), que no invente productos que no están en el JSON, y que la respuesta sea rápida (con 300 productos activos ya está en modo "catálogo grande" - `UMBRAL_CATALOGO_COMPLETO = 60` en `AiResponseService` - así que nunca se manda el catálogo completo al prompt).

7. Cuando termines de probar, podés borrar el tenant de prueba desde el panel (Negocios → Eliminar) - se lleva todo (productos, conversaciones, etc.).

## Regenerar el dataset

Si querés otra combinación de categorías/cantidades, `productos.json` se generó con un script Python simple (combinatoria de modelo × color × talla, recortado a 300 al azar). Pedime que te lo regenere con otra distribución si hace falta.
