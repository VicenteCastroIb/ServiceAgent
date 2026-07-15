export type BloqueContenido =
  | { tipo: "parrafo"; texto: string }
  | { tipo: "subtitulo"; texto: string }
  | { tipo: "enlace-interno"; texto: string; enlaceTexto: string; href: string };

export interface BlogPost {
  slug: string;
  categoria: string;
  titulo: string;
  extracto: string;
  minutos: number;
  fecha: string;
  destacado?: boolean;
  contenido: BloqueContenido[];
}

export const BLOG_POSTS: BlogPost[] = [
  {
    slug: "como-automatizar-atencion-whatsapp",
    categoria: "Guías",
    titulo: "Cómo automatizar la atención de tu negocio por WhatsApp",
    extracto:
      "Guía práctica para automatizar la atención al cliente por WhatsApp: qué automatizar, cómo hacerlo con IA y errores a evitar. Para negocios en Chile.",
    minutos: 6,
    fecha: "18 de junio de 2026",
    destacado: true,
    contenido: [
      {
        tipo: "parrafo",
        texto:
          "Si tu negocio atiende clientes por WhatsApp, seguramente ya viviste esto: un mensaje que llega a las 23:40 preguntando por un horario, y que recién ves al otro día, cuando el cliente ya resolvió el problema en otro lado. Automatizar la atención no es reemplazarte, es cubrir el horario en el que vos no podés estar mirando el teléfono.",
      },
      { tipo: "subtitulo", texto: "Qué conviene automatizar primero" },
      {
        tipo: "parrafo",
        texto:
          "No hace falta automatizar todo de una. Las preguntas que más se repiten (horarios, ubicación, precios, disponibilidad) son el punto de partida más rentable: son las que más tiempo te quitan y las que menos necesitan tu criterio personal para responder.",
      },
      {
        tipo: "parrafo",
        texto:
          "Recién después de eso conviene sumar acciones más complejas, como agendar una cita o cerrar una venta dentro de la misma conversación, sin que el cliente tenga que salir de WhatsApp.",
      },
      { tipo: "subtitulo", texto: "Por qué un menú de opciones no alcanza" },
      {
        tipo: "parrafo",
        texto:
          "Los bots de 'escriba 1 para...' fueron el primer intento de automatizar WhatsApp, pero se sienten robóticos y frustran al cliente que solo quiere preguntar algo simple. La diferencia de usar un asistente con inteligencia artificial es que entiende lenguaje natural: el cliente escribe como le escribiría a una persona, y así se contesta.",
      },
      {
        tipo: "enlace-interno",
        texto: "Si tu negocio vive de las horas agendadas, este es justo el primer lugar donde conviene arrancar:",
        enlaceTexto: "mirá cómo funciona el agendamiento automático",
        href: "/agendamiento",
      },
      { tipo: "subtitulo", texto: "Errores comunes al automatizar" },
      {
        tipo: "parrafo",
        texto:
          "El error más común es dejar que el bot invente información cuando no está seguro: un horario que no existe, un precio viejo, un producto que ya no tenés. Un buen asistente tiene que apoyarse en el catálogo y contexto real de tu negocio, y derivar a una persona cuando la situación se complica en vez de improvisar.",
      },
    ],
  },
  {
    slug: "cuanto-cuesta-chatbot-en-chile",
    categoria: "Precios",
    titulo: "Cuánto cuesta un chatbot en Chile",
    extracto: "Cuánto cuesta un chatbot para WhatsApp en Chile: rangos de precio, qué incluye cada opción y cómo elegir sin pagar de más. Guía honesta para pymes.",
    minutos: 5,
    fecha: "18 de junio de 2026",
    contenido: [
      {
        tipo: "parrafo",
        texto:
          "El precio de un chatbot para WhatsApp en Chile varía muchísimo según qué tan simple o completo sea. Desde plantillas gratuitas con respuestas fijas, hasta asistentes con inteligencia artificial que agendan horas y venden un catálogo completo, con planes que rondan los $20.000 a $30.000 CLP mensuales para pymes.",
      },
      { tipo: "subtitulo", texto: "Qué determina el precio" },
      {
        tipo: "parrafo",
        texto:
          "Tres cosas mueven el precio hacia arriba: que el bot entienda lenguaje natural (no solo menús), que agende citas de verdad conectado a una agenda real, y que venda un catálogo con pagos integrados. Cuantas más de estas funciones incluya, más caro (y más útil) suele ser.",
      },
      {
        tipo: "parrafo",
        texto:
          "También influye si el número de WhatsApp es propio del negocio o compartido, y si hay soporte humano de respaldo cuando el bot no puede resolver algo.",
      },
      {
        tipo: "enlace-interno",
        texto: "Para que te hagas una idea concreta con planes reales, en vez de rangos generales:",
        enlaceTexto: "mirá los precios de ServiceAgent",
        href: "/precios",
      },
      { tipo: "subtitulo", texto: "Cómo elegir sin pagar de más" },
      {
        tipo: "parrafo",
        texto:
          "Si tu negocio no agenda horas ni vende catálogo, no tiene sentido pagar por esas funciones. Empezá por el plan más simple que resuelva el problema que más te duele (clientes sin respuesta) y subí de plan solo cuando de verdad lo necesites.",
      },
    ],
  },
  {
    slug: "whatsapp-business-api-vs-app-normal",
    categoria: "WhatsApp",
    titulo: "WhatsApp Business API vs la app normal: cuál necesitas",
    extracto:
      "Diferencias entre WhatsApp Business app y la API oficial: cuándo necesitas cada una, qué permite automatizar y cuánto cuesta. Explicado para negocios en Chile.",
    minutos: 6,
    fecha: "18 de junio de 2026",
    contenido: [
      {
        tipo: "parrafo",
        texto:
          "WhatsApp Business (la app gratuita que se instala en un celular) y la API de WhatsApp Business son dos cosas distintas, aunque se llamen parecido. La app sirve para que una persona atienda manualmente desde su teléfono. La API está pensada para que un sistema (como un asistente con IA) responda automáticamente, sin depender de un celular prendido.",
      },
      { tipo: "subtitulo", texto: "Cuándo alcanza con la app" },
      {
        tipo: "parrafo",
        texto:
          "Si atendés vos mismo, con bajo volumen de mensajes, la app gratuita puede alcanzar. El límite aparece cuando el volumen crece: no se puede automatizar respuestas reales, no hay varios agentes atendiendo a la vez de forma prolija, y todo depende de que alguien esté mirando el teléfono.",
      },
      { tipo: "subtitulo", texto: "Cuándo conviene la API" },
      {
        tipo: "parrafo",
        texto:
          "Cuando querés que un asistente responda 24/7, agende citas o cierre ventas sin intervención manual, necesitás la API. Es la puerta de entrada técnica que usan las plataformas de automatización (como la nuestra) para conectar tu número de WhatsApp con inteligencia artificial real.",
      },
      {
        tipo: "enlace-interno",
        texto: "En la práctica, esto se traduce en un asistente que atiende solo desde el primer día:",
        enlaceTexto: "mirá cómo funciona ServiceAgent",
        href: "/",
      },
    ],
  },
  {
    slug: "que-es-whatsapp-business",
    categoria: "WhatsApp",
    titulo: "WhatsApp Business: qué es y para qué sirve",
    extracto:
      "Qué es WhatsApp Business, en qué se diferencia del WhatsApp normal y cómo usarlo para atender clientes y vender. Guía clara para negocios en Chile.",
    minutos: 4,
    fecha: "18 de junio de 2026",
    contenido: [
      {
        tipo: "parrafo",
        texto:
          "WhatsApp Business es la versión gratuita de WhatsApp pensada para negocios chicos: agrega un perfil con horario de atención, dirección y catálogo básico de productos, además de respuestas rápidas y mensajes automáticos simples de bienvenida o ausencia.",
      },
      { tipo: "subtitulo", texto: "Qué NO resuelve WhatsApp Business" },
      {
        tipo: "parrafo",
        texto:
          "No entiende lenguaje natural más allá de esos mensajes automáticos fijos, no agenda citas de verdad y no puede atender a varios clientes en simultáneo de forma automatizada. Para eso hace falta un asistente construido sobre la API oficial de WhatsApp, no la app.",
      },
      {
        tipo: "enlace-interno",
        texto: "Si tu negocio ya usa WhatsApp Business y sentís que se quedó corto, este es el siguiente paso natural:",
        enlaceTexto: "conocé el agendamiento automático",
        href: "/agendamiento",
      },
    ],
  },
  {
    slug: "que-es-un-chatbot",
    categoria: "Conceptos",
    titulo: "Qué es un chatbot y cómo le sirve a tu negocio",
    extracto:
      "Qué es un chatbot, cómo funciona y de qué forma un bot con IA en WhatsApp puede agendar horas y atender ventas en tu negocio. Explicado claro y sin tecnicismos.",
    minutos: 5,
    fecha: "18 de junio de 2026",
    contenido: [
      {
        tipo: "parrafo",
        texto:
          "Un chatbot es un programa que conversa con una persona, generalmente por texto, simulando una charla real. Los hay muy simples (menús con opciones fijas) y muy avanzados (los que usan inteligencia artificial para entender lo que se les escribe, sin importar cómo esté redactado).",
      },
      { tipo: "subtitulo", texto: "La diferencia de uno con IA" },
      {
        tipo: "parrafo",
        texto:
          "Un chatbot tradicional necesita que el usuario elija entre opciones predefinidas. Uno con inteligencia artificial entiende preguntas escritas de forma libre ('¿tienen hora para mañana?', '¿cuánto sale el corte?') y responde apoyándose en el contexto real del negocio: catálogo, precios, horarios.",
      },
      {
        tipo: "parrafo",
        texto:
          "Eso es justamente lo que permite que agende una cita o arme un carrito de compra dentro de la misma conversación, sin que el cliente sienta que está hablando con un menú telefónico.",
      },
      {
        tipo: "enlace-interno",
        texto: "Si querés ver esto funcionando con un ejemplo real de conversación:",
        enlaceTexto: "mirá una demo del asistente",
        href: "/",
      },
    ],
  },
];

export function buscarPost(slug: string): BlogPost | undefined {
  return BLOG_POSTS.find((post) => post.slug === slug);
}
