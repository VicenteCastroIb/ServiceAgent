# Prompts para Leonardo AI — imágenes faltantes de la landing

Estilo base a repetir en cada prompt (ya incluido abajo en cada uno) para mantener consistencia visual en todo el sitio: fotografía realista, luz natural cálida, paleta crema/mostaza/rosa empolvado (#faf7f1, #b9862f, #c9788f), estética de negocio local chileno (tienda de ropa, estética, taller, restaurante), sin texto ni logos en la imagen, composición limpia con espacio negativo para overlay de UI.

Modelo sugerido en Leonardo: **Leonardo Phoenix 1.0** o **PhotoReal**. Negative prompt sugerido para todas: `text, watermark, logo, blurry, distorted hands, extra fingers, cartoon, illustration, oversaturated, stock photo watermark`.

## Proporciones reales (medidas en el sitio a 1440px de ancho)

Cada tarjeta de imagen tiene **alto fijo** y **ancho fluido** (se adapta al viewport), así que estas son las proporciones tal como se ven hoy en desktop. Los 24 slots se repiten en 4 tipos exactos a lo largo de las 3 páginas — anotado en cada prompt cuál le corresponde:

| Tipo de tarjeta | Dónde aparece | Medida renderizada | Proporción | Ancho recomendado para generar en Leonardo |
|---|---|---|---|---|
| **A — Paso (Steps)** | 3 por página (Inicio, Agendamiento, Ecommerce) = 9 imgs | 322×150 px | **~2.15:1** | Custom **1536×715** (o el preset 16:9 y recortar) |
| **B — Feature grande** | 1 por página = 3 imgs | 492×160 px | **~3.1:1** (banner ancho) | Custom **1536×500** |
| **C — Feature normal (angosta)** | 2 por página = 6 imgs | 241×130 px | **~1.85:1** | Custom **1200×650** (o preset 16:9) |
| **D — Feature normal (ancha)** | 2 por página = 6 imgs | 521×130 px | **~4:1** (banner muy ancho) | Custom **1536×384** |

Nota: Leonardo permite ancho/alto custom (no solo los presets 1:1, 4:3, 16:9, etc.) en modelos como Phoenix. Si preferís usar un preset fijo, generá en **16:9** y luego recortá al alto que corresponda — como el layout usa `object-fit` tipo recorte, sobra imagen es mejor que falte.

---

## Página Inicio (`page.tsx`)

**1. Omnicanal real** — Tipo B (492×160, ~3.1:1)
Placeholder: dueño de negocio respondiendo WhatsApp e Instagram desde el celular
> Photorealistic candid photo of a small business owner in their 30s standing behind a counter in a cozy clothing or beauty shop, smiling while typing on a smartphone, WhatsApp-style chat bubbles subtly visible on screen, warm natural window light, cream and terracotta tones in the background, shallow depth of field, shot on 50mm lens, editorial lifestyle photography style.

**2. Respuestas al instante, 24/7** — Tipo C (241×130, ~1.85:1)
Placeholder: notificación de celular a la madrugada, respondida al instante
> Photorealistic close-up of a smartphone screen glowing in a dark room at night on a nightstand, showing an incoming chat notification, soft blue-warm screen glow illuminating the surface, blurred cozy bedroom background, cinematic low-light photography, shallow depth of field.

**3. Deriva a un humano cuando hace falta** — Tipo C (241×130, ~1.85:1)
Placeholder: persona del equipo tomando una llamada o revisando el panel
> Photorealistic photo of a friendly staff member sitting at a small desk in a shop, wearing a headset, looking at a laptop screen with a chat dashboard, warm cream-toned interior, natural daylight, candid customer-service moment, editorial lifestyle photography.

**4. Integración panel/CRM** — Tipo D (521×130, ~4:1)
Placeholder: pantalla mostrando integración entre panel y planilla/CRM
> Photorealistic over-the-shoulder shot of hands on a laptop keyboard, screen showing a clean dashboard interface with chat list and a spreadsheet-style table side by side, warm cream desk setup, soft natural light, shallow depth of field, modern minimal office photography.

**5. Catálogo de productos** — Tipo D (521×130, ~4:1)
Placeholder: catálogo de productos con precios reales sobre un mostrador
> Photorealistic top-down photo of neatly arranged small retail products (clothing items or handmade goods) on a wooden counter with small handwritten price tags, warm natural light, cream and terracotta color palette, flat lay composition, editorial product photography.

**6. Paso 1 — Conecta tus canales** — Tipo A (322×150, ~2.15:1)
Placeholder: celular vinculando WhatsApp Business e Instagram
> Photorealistic close-up of a hand holding a smartphone showing the WhatsApp Business and Instagram app icons being set up, soft warm indoor lighting, shallow depth of field, cream-toned background, clean lifestyle tech photography.

**7. Paso 2 — Entrena la IA con tu negocio** — Tipo A (322×150, ~2.15:1)
Placeholder: dueño de negocio cargando catálogo y precios en el panel
> Photorealistic photo of a small business owner sitting at a counter typing into a laptop, product boxes and a notebook with handwritten prices nearby, warm afternoon light through a shop window, candid editorial lifestyle photography, cream and mustard tones.

**8. Paso 3 — Deja que atienda y venda solo** — Tipo A (322×150, ~2.15:1)
Placeholder: negocio funcionando de noche, celular con chat activo sobre el mostrador
> Photorealistic photo of a small shop interior at night, lights dimmed, a smartphone lying on the counter with an active chat screen glowing, city lights visible faintly through the window, moody warm-toned night photography, shallow depth of field.

---

## Página Agendamiento (`agendamiento/page.tsx`)

**9. Conversación de WhatsApp entrando** — Tipo A (322×150, ~2.15:1)
Placeholder: celular mostrando una conversación de WhatsApp entrando
> Photorealistic close-up of a smartphone screen held in hand, showing an incoming WhatsApp chat conversation, warm soft lighting, cream tones, shallow depth of field, clean lifestyle tech photography.

**10. Agenda con horas disponibles** — Tipo A (322×150, ~2.15:1)
Placeholder: agenda o calendario con horas disponibles en una pantalla
> Photorealistic photo of a laptop or tablet screen showing a clean weekly calendar interface with available time slots highlighted, resting on a wooden desk with a coffee cup nearby, warm natural light, minimal modern office photography.

**11. Recordatorio de cita en el celular** — Tipo A (322×150, ~2.15:1)
Placeholder: persona recibiendo un recordatorio de cita en el celular
> Photorealistic candid photo of a person smiling while looking at their smartphone outdoors or in a café, warm natural daylight, casual lifestyle photography, shallow depth of field, cream and warm tones.

**12. Negocio de noche, mostrador cerrado** — Tipo B (492×160, ~3.1:1)
Placeholder: negocio con celular activo de noche, mostrador cerrado
> Photorealistic photo of a small salon or clinic storefront at night, closed sign visible, a smartphone glowing on the reception counter with an active chat, warm ambient light, moody editorial night photography.

**13. Recordatorio de cita en pantalla** — Tipo C (241×130, ~1.85:1)
Placeholder: recordatorio de cita en la pantalla de un celular
> Photorealistic close-up of a smartphone lock screen showing a calendar appointment reminder notification, resting on a wooden table, soft warm natural light, shallow depth of field, clean product photography.

**14. Cliente sonriendo al confirmar cita** — Tipo C (241×130, ~1.85:1)
Placeholder: cliente sonriendo al recibir confirmación de su cita
> Photorealistic candid portrait of a happy customer looking at their phone with a warm smile, sitting in a bright waiting area of a salon or clinic, natural daylight, warm cream tones, editorial lifestyle photography.

**15. Persona del equipo tomando control de una conversación** — Tipo D (521×130, ~4:1)
Placeholder: persona del equipo tomando el control de una conversación
> Photorealistic photo of a staff member at a front desk picking up a smartphone to reply personally, warm attentive expression, soft natural light, cream-toned reception area background, candid editorial photography.

**16. Persona escribiendo mensaje relajado** — Tipo D (521×130, ~4:1)
Placeholder: persona escribiendo un mensaje relajado en el celular
> Photorealistic candid photo of a person casually typing a message on their smartphone while relaxed, sitting on a couch or at a café table, warm soft natural light, shallow depth of field, lifestyle photography.

---

## Página Ecommerce (`ecommerce/page.tsx`)

**17. Cliente escribiendo por Instagram** — Tipo A (322×150, ~2.15:1)
Placeholder: cliente escribiendo por Instagram desde su celular
> Photorealistic close-up of a hand holding a smartphone with the Instagram DM interface open, warm natural lighting, cream tones, shallow depth of field, clean lifestyle tech photography.

**18. Productos del catálogo sobre mostrador** — Tipo A (322×150, ~2.15:1)
Placeholder: productos del catálogo ordenados sobre un mostrador
> Photorealistic flat lay photo of small retail products neatly arranged on a wooden counter, warm natural light, cream and terracotta palette, minimal styled product photography.

**19. Link de pago confirmado** — Tipo A (322×150, ~2.15:1)
Placeholder: celular mostrando un link de pago confirmado
> Photorealistic close-up of a smartphone screen showing a payment confirmation checkmark screen, held in hand, warm soft lighting, shallow depth of field, clean fintech lifestyle photography.

**20. Tienda respondiendo pedidos de madrugada** — Tipo B (492×160, ~3.1:1)
Placeholder: tienda con celular activo respondiendo pedidos de madrugada
> Photorealistic photo of a small retail store interior at night with dim ambient lighting, a smartphone glowing on the counter showing an active order chat, moody warm night photography, shallow depth of field.

**21. Repisa de productos por categoría** — Tipo C (241×130, ~1.85:1)
Placeholder: repisa de productos ordenados por categoría
> Photorealistic photo of a neatly organized retail shelf with products grouped by category, warm natural light streaming in, cream and wood tones, editorial retail photography, shallow depth of field.

**22. Etiqueta de precio y stock** — Tipo C (241×130, ~1.85:1)
Placeholder: etiqueta de precio y stock sobre un producto real
> Photorealistic close-up macro photo of a handwritten or printed price tag attached to a retail product, soft natural light, warm tones, shallow depth of field, product detail photography.

**23. Productos combinados tipo combo** — Tipo D (521×130, ~4:1)
Placeholder: dos productos combinados, tipo outfit o combo
> Photorealistic styled flat lay photo of two complementary retail products (like a matching outfit or product bundle) arranged together on a warm neutral background, soft natural light, minimal editorial product photography.

**24. Carrito de compra en el chat** — Tipo D (521×130, ~4:1)
Placeholder: celular mostrando un carrito de compra en el chat
> Photorealistic close-up of a smartphone screen held in hand showing a shopping cart summary inside a chat conversation interface, warm soft lighting, shallow depth of field, clean lifestyle tech photography.

---

## Cómo usarlos
1. Pega cada prompt en Leonardo AI con el negative prompt de arriba.
2. Generá 2-4 variaciones por imagen y elegí la más consistente con las demás en tono de luz y color.
3. Exportá en la proporción de su Tipo (A/B/C/D, ver tabla arriba) y reemplazá el bloque `photoLabel` correspondiente en el código por el `<Image>` real.
