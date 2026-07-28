import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Política de Privacidad — ServiceAgent",
  description:
    "Cómo ServiceAgent recolecta, usa y protege los datos personales de los negocios que contratan el servicio y de sus clientes finales.",
};

const ACTUALIZADO = "27 de julio de 2026";

export default function PoliticaPrivacidadPage() {
  return (
    <section className="mx-auto max-w-[820px] px-5 py-[clamp(48px,7vw,72px)] sm:px-10">
      <h1 className="text-[clamp(28px,3.6vw,40px)] font-extrabold tracking-[-0.02em] text-ink">
        Política de Privacidad
      </h1>
      <p className="mt-2 text-[14px] text-ink/45">Última actualización: {ACTUALIZADO}</p>

      <div className="prose-legal mt-10 flex flex-col gap-8 text-[15px] leading-relaxed text-ink/75">
        <p>
          ServiceAgent (&quot;nosotros&quot;, &quot;la plataforma&quot;) es un servicio de agente de inteligencia
          artificial para atención automatizada de WhatsApp e Instagram, operado por Vicente Castro Ibarra, persona
          natural, con domicilio en Chile. Contacto: <a className="text-violet-light underline" href="mailto:vicentecastroibarra@gmail.com">vicentecastroibarra@gmail.com</a>.
        </p>
        <p>
          Esta política describe cómo tratamos los datos personales al ofrecer el servicio a negocios y locales
          (&quot;el Cliente&quot;) y, a través de ellos, a las personas que le escriben por WhatsApp o Instagram
          (&quot;el cliente final&quot;). Cumplimos con la Ley N.º 19.628 sobre Protección de la Vida Privada de Chile.
        </p>

        <div>
          <h2 className="text-[19px] font-bold text-ink">1. Qué datos recolectamos</h2>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5">
            <li>
              <strong>Del Cliente (dueño del negocio):</strong> nombre del negocio, email, credenciales de acceso al
              panel, plan contratado, datos de facturación (a través de la pasarela de pago Flow, nunca vemos el
              número de tarjeta), y las credenciales propias que cargue para conectar su tienda online o su cuenta de
              Instagram.
            </li>
            <li>
              <strong>Del cliente final:</strong> número de WhatsApp o identificador de Instagram, el contenido de
              los mensajes que envía, y cualquier dato que entregue voluntariamente en la conversación (por ejemplo,
              al agendar una hora o hacer una compra).
            </li>
            <li>
              <strong>De navegación en este sitio:</strong> datos de uso agregados a través de Google Analytics y
              Meta Pixel (ver sección 5), si estos están activos.
            </li>
          </ul>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">2. Para qué usamos estos datos</h2>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5">
            <li>Generar respuestas automáticas contextualizadas al negocio del Cliente.</li>
            <li>Agendar, confirmar, reagendar o cancelar citas (plan Pro y Catálogo).</li>
            <li>Generar links de pago y procesar compras (plan Catálogo).</li>
            <li>Derivar la conversación a una persona del negocio cuando corresponda.</li>
            <li>Facturar la suscripción mensual del Cliente y notificarle sobre su cuenta.</li>
            <li>Mejorar el servicio y medir el resultado de nuestras campañas de publicidad.</li>
          </ul>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">3. Con quién compartimos datos</h2>
          <p className="mt-3">
            Para operar el servicio, ciertos datos pasan por proveedores externos que actúan como encargados de
            tratamiento, bajo sus propias políticas de privacidad:
          </p>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5">
            <li><strong>Twilio</strong> (Estados Unidos) — envío y recepción de mensajes de WhatsApp.</li>
            <li><strong>Meta / Instagram</strong> — mensajería de Instagram.</li>
            <li><strong>Anthropic (Claude)</strong> — generación de las respuestas del agente de IA. El contenido de
              los mensajes se envía a su API para procesar cada conversación.</li>
            <li><strong>Flow</strong> — procesamiento de pagos (suscripción del Cliente y, en el plan Catálogo,
              cobros a los clientes finales).</li>
            <li><strong>Google Analytics y Meta Pixel</strong> — medición de tráfico y campañas en este sitio, si el
              Cliente potencial no rechaza las cookies correspondientes.</li>
          </ul>
          <p className="mt-3">
            No vendemos datos personales a terceros. No compartimos el contenido de las conversaciones de un negocio
            con otros negocios de la plataforma.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">4. Cuánto tiempo conservamos los datos</h2>
          <p className="mt-3">
            Conservamos el historial de conversaciones mientras la cuenta del Cliente esté activa, para que pueda
            revisarlo desde el panel. Si el Cliente cancela su suscripción y solicita la eliminación de su cuenta,
            borramos sus datos y los de sus conversaciones dentro de un plazo razonable, salvo que la ley exija
            conservar registros de facturación por más tiempo.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">5. Cookies y analítica de este sitio</h2>
          <p className="mt-3">
            Este sitio (la landing de ServiceAgent) puede usar Google Analytics y Meta Pixel para entender cómo se
            usa el sitio y medir el resultado de campañas de publicidad. Estas herramientas usan cookies y pueden
            recolectar información como tu dirección IP, el navegador que usás y las páginas que visitás. Podés
            bloquear estas cookies desde la configuración de tu navegador.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">6. Tus derechos</h2>
          <p className="mt-3">
            Como titular de tus datos, tenés derecho a acceder, rectificar, cancelar y oponerte al tratamiento de tus
            datos personales (derechos ARCO), conforme a la Ley N.º 19.628. Para ejercerlos, escribinos a{" "}
            <a className="text-violet-light underline" href="mailto:vicentecastroibarra@gmail.com">vicentecastroibarra@gmail.com</a>.
            Si sos cliente final de uno de los negocios que usa ServiceAgent y querés que se elimine tu historial de
            conversación, podés pedírselo directamente al negocio o escribirnos a nosotros y lo coordinamos con él.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">7. Seguridad</h2>
          <p className="mt-3">
            Las credenciales sensibles que carga el Cliente (por ejemplo, las de su pasarela de pago o su tienda
            online) se guardan cifradas en nuestra base de datos. El acceso al panel requiere usuario y contraseña
            propios de cada negocio.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">8. Cambios a esta política</h2>
          <p className="mt-3">
            Podemos actualizar esta política ocasionalmente. Si hacemos cambios importantes, lo vamos a indicar en
            esta misma página con la fecha de la última actualización.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">9. Contacto</h2>
          <p className="mt-3">
            Para cualquier consulta sobre esta política o tus datos, escribinos a{" "}
            <a className="text-violet-light underline" href="mailto:vicentecastroibarra@gmail.com">vicentecastroibarra@gmail.com</a>.
          </p>
        </div>
      </div>
    </section>
  );
}
