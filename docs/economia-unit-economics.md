## 7. Economía del negocio (unit economics) — actualizada v3 (corrige v2)

**Corrección respecto a la versión anterior:** el cálculo de v2 omitió el costo del número de WhatsApp dedicado por tenant. Twilio no solo cobra por mensaje — cobra un arriendo mensual fijo por número ($10 USD/mes para un número móvil chileno, "Twilio Leased Clean Mobile Number"). Ese costo no estaba en ningún lado de la planilla anterior, y al ser un costo fijo por cliente, pega fuerte justo en el plan más barato. Con esto corregido, el margen real es bastante menor al ~80-87% que decía v2 — sigue siendo un negocio viable, pero hay que venderlo con el número real, no el optimista.

TC usado: ≈ $928 CLP/USD (mismo de v2, revalidar antes de cerrar precios).

### Plan Básico ($19.990 CLP/mes)

| Concepto | Estimado mensual |
|---|---|
| Número dedicado Twilio (fijo) | ~$9.280 CLP |
| Mensajería (500 msj × $0.005 USD Twilio) | ~$2.320 CLP |
| Costo Meta (reactivo, dentro de ventana 24h) | $0 |
| Costo Instagram (directo) | $0 |
| Costo LLM (700 msj, económico + cache) | ~$300 – $1.300 CLP |
| **Costo total Básico** | **~$11.900 – $12.900 CLP** |
| Precio | $19.990 CLP |
| **Margen bruto real** | **~36% – 40%** (no 82-87%) |

### Plan Pro ($24.990 CLP/mes)

**Dato verificado directo en la fuente oficial** (calculadora de tarifas de Meta, `whatsappbusiness.com/products/platform-pricing`, Mercado=Chile, Divisa=USD): la tarifa real de Meta para plantillas Utility y Authentication en Chile es **$0.0200 USD/mensaje**, no $0.0034 como se había tomado de la calculadora de Twilio (ese número resultó ser incorrecto/desactualizado — la calculadora de Gupshup mostraba el mismo $0.0200, y la fuente oficial de Meta lo confirma). Se corrige el cálculo con el dato verificado.

| Concepto | Estimado mensual |
|---|---|
| Todo lo de Básico (incluye número) | ~$11.900 – $12.900 CLP |
| 100-150 recordatorios × ($0.0200 Meta + $0.005 Twilio) | ~$2.320 – $3.480 CLP |
| **Costo total Pro** | **~$14.220 – $16.380 CLP** |
| Precio | $24.990 CLP |
| **Margen bruto real** | **~35% – 43%** |

### Escenario 30 clientes (18 Básico, 9 Pro, 3 Catálogo ~$45.000 CLP promedio)

Recalculado de abajo hacia arriba con los números corregidos por plan (Catálogo tratado como costo de mensajería equivalente a Pro, ya que la sincronización de catálogo en sí no agrega costo de WhatsApp):

- 18 Básico × ~$12.400 CLP (punto medio) ≈ $223.200 CLP
- 9 Pro × ~$15.300 CLP (punto medio) ≈ $137.700 CLP
- 3 Catálogo × ~$15.300 CLP (estimado, equivalente a Pro) ≈ $45.900 CLP
- **Costo total: ≈ $406.800 CLP/mes**
- Ingreso total: ≈ $720.000 CLP/mes (igual que v2, no cambia)
- **Margen bruto real: ≈ 43,5%**
- **Utilidad: ≈ $313.200 CLP/mes**, antes de tiempo propio, hosting fijo, soporte y marketing.

No es un negocio inviable — 35-43% de margen bruto sigue siendo sano para un SaaS a este volumen — pero es un número bastante distinto al que se venía usando para proyectar, y el plan Básico es el que queda más ajustado porque el número fijo pesa proporcionalmente más contra un ticket chico.

---

## 7.1 Alternativas investigadas para bajar el costo del número (nuevo)

Se evaluaron 3 alternativas a Twilio como proveedor de mensajería/número para WhatsApp:

**360dialog** — descartado a esta escala. Su plan de API individual cobra €49/número/mes (más caro que el $10 USD de Twilio), y su "Partner Platform" (pensado justo para SaaS multi-tenant como este) parte en €250/mes base + €49 por canal adicional — mucho más caro que Twilio mientras el negocio tenga pocos clientes. Podría volverse competitivo recién con volumen alto, no en esta etapa.

**Gupshup** — candidato fuerte, requiere validar directo con ellos antes de migrar. Según su propia documentación: modelo 100% pago por uso, sin arriendo mensual por número, sin compromiso mínimo. Su fee es $0.001 USD por mensaje (session y template) — 5 veces más barato que el $0.005 de Twilio — más lo que cobre Meta aparte. Si el costo del número efectivamente desaparece o baja mucho, el impacto en el plan Básico sería enorme:

| Concepto (Básico, con Gupshup, estimado) | Monto |
|---|---|
| Mensajería (500 msj × $0.001 USD) | ~$464 CLP |
| Costo LLM | ~$300 – $1.300 CLP |
| Número (a confirmar con Gupshup) | por validar |
| **Margen estimado si el número es ~$0** | **~91% – 96%** |

Antes de migrar: confirmar con Gupshup el costo real de habilitar un número para Chile (la documentación pública no distingue bien costo de número vs costo de mensaje), y el esfuerzo de reintegración — hoy `MessagingService`/`TwilioTemplateService` están acoplados al SDK de Twilio, migrar de BSP implica reescribir esa capa (no el resto del sistema).

**Meta Cloud API directo (sin BSP)** — ya estaba planeado como "Fase 2" en la sección 5.5 del documento original, y sigue siendo la opción más barata a largo plazo: acceso a la API es gratis, se paga solo la tarifa de Meta (sin markup de Twilio ni de ningún BSP), y el número no depende de un arriendo tipo Twilio (se registra un número propio vía verificación SMS/voz). El proyecto ya tiene experiencia concreta con este patrón: Instagram ya integra directo contra la Graph API de Meta (`InstagramWebhookController`, `InstagramOAuthCallbackController`), así que extender el mismo enfoque a WhatsApp no es un salto grande en términos de arquitectura, aunque sí implica más trabajo que usar un BSP (manejo propio de templates, verificación de negocio, embedded signup para dar de alta clientes nuevos).

**Recomendación:** dado el impacto en margen, vale la pena adelantar la validación de Gupshup como reemplazo rápido de Twilio (cambio acotado a la capa de mensajería) antes de meterse a construir "Fase 2" Meta-directo, que da el mejor margen posible pero es más trabajo. Gupshup como paso intermedio, Meta directo como destino final si el volumen lo justifica.
