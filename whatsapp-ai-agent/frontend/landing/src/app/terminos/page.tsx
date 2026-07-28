import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Términos y Condiciones — ServiceAgent",
  description: "Condiciones de uso del servicio ServiceAgent para negocios que contratan la atención automatizada por WhatsApp e Instagram.",
};

const ACTUALIZADO = "27 de julio de 2026";

export default function TerminosPage() {
  return (
    <section className="mx-auto max-w-[820px] px-5 py-[clamp(48px,7vw,72px)] sm:px-10">
      <h1 className="text-[clamp(28px,3.6vw,40px)] font-extrabold tracking-[-0.02em] text-ink">
        Términos y Condiciones
      </h1>
      <p className="mt-2 text-[14px] text-ink/45">Última actualización: {ACTUALIZADO}</p>

      <div className="prose-legal mt-10 flex flex-col gap-8 text-[15px] leading-relaxed text-ink/75">
        <p>
          Estos Términos y Condiciones regulan el uso de ServiceAgent, un servicio de suscripción mensual operado por
          Vicente Castro Ibarra, persona natural, con domicilio en Chile (&quot;nosotros&quot;, &quot;la
          plataforma&quot;). Al registrarte o contratar un plan, aceptás estos términos.
        </p>

        <div>
          <h2 className="text-[19px] font-bold text-ink">1. Qué es el servicio</h2>
          <p className="mt-3">
            ServiceAgent es un agente de inteligencia artificial que responde automáticamente los mensajes de
            WhatsApp e Instagram de tu negocio, usando el contexto (catálogo, precios, horarios, tono) que vos
            cargás en el panel. Según el plan contratado, también puede agendar citas, enviar recordatorios y generar
            links de pago. El servicio deriva la conversación a una persona cuando corresponde, y siempre podés
            pausar el bot y responder manualmente desde el panel (&quot;modo híbrido&quot;).
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">2. Planes, precios y facturación</h2>
          <p className="mt-3">
            Los planes disponibles, sus precios y qué incluye cada uno están publicados en la página de precios de
            este sitio y pueden actualizarse con aviso previo. El plan Catálogo/Ecommerce es a medida y se cotiza
            directamente. La suscripción se cobra mensualmente por adelantado, de forma automática mediante tarjeta
            (a través de Flow) o por transferencia manual, según el método que hayas elegido. Si un cobro automático
            es rechazado, tu cuenta puede quedar marcada como morosa y el agente de IA deja de responder
            automáticamente a tus clientes hasta que regularices el pago (podés seguir viendo y respondiendo
            manualmente desde el panel mientras tanto).
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">3. Tu responsabilidad como Cliente</h2>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5">
            <li>El contenido que cargás (catálogo, precios, horarios, contexto del negocio) es tu responsabilidad. El
              agente responde en base a lo que vos le entregás.</li>
            <li>Si usás el plan Catálogo, la pasarela de pago (Flow u otra) que conectás es tu propia cuenta —
              nosotros solo generamos el link de pago con tus credenciales, no intermediamos el dinero ni somos
              responsables ante tus clientes por esos cobros.</li>
            <li>Sos responsable de que el número de WhatsApp o la cuenta de Instagram que conectás te pertenezcan y
              cumplan con las políticas de Meta.</li>
            <li>Es tu responsabilidad revisar periódicamente las conversaciones derivadas a un humano y responderlas
              a tiempo.</li>
          </ul>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">4. Límites del servicio</h2>
          <p className="mt-3">
            El agente de IA puede cometer errores o no entender correctamente un mensaje. Hacemos nuestro mejor
            esfuerzo para que derive a una persona cuando no está seguro, pero no garantizamos que la respuesta sea
            siempre exacta. No somos responsables por pérdidas de venta, reclamos de clientes finales, o cualquier
            perjuicio derivado de una respuesta incorrecta del agente, en la medida máxima permitida por la ley.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">5. Disponibilidad del servicio</h2>
          <p className="mt-3">
            Hacemos un esfuerzo razonable para mantener el servicio disponible, pero puede haber interrupciones por
            mantenimiento, fallas de nuestros proveedores (Twilio, Meta, Anthropic, Flow, el proveedor de hosting) o
            causas fuera de nuestro control. No garantizamos disponibilidad ininterrumpida.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">6. Cancelación</h2>
          <p className="mt-3">
            Podés cancelar tu suscripción cuando quieras escribiéndonos. La cancelación aplica desde el próximo
            período de facturación; no hacemos devoluciones proporcionales de un período ya cobrado, salvo que la ley
            aplicable indique lo contrario. Podemos suspender o cancelar una cuenta que incumpla estos términos o que
            use el servicio para fines ilegales, spam, o contenido engañoso hacia sus clientes.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">7. Propiedad de los datos</h2>
          <p className="mt-3">
            El contenido de tu catálogo y el historial de conversaciones de tu negocio te pertenecen. Podés pedir una
            copia o la eliminación de tus datos en cualquier momento (ver nuestra{" "}
            <a className="text-violet-light underline" href="/privacidad">Política de Privacidad</a>).
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">8. Cambios a estos términos</h2>
          <p className="mt-3">
            Podemos actualizar estos términos ocasionalmente. Si el cambio es significativo, te avisamos por email o
            desde el panel antes de que entre en vigencia.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">9. Ley aplicable</h2>
          <p className="mt-3">
            Estos términos se rigen por las leyes de Chile. Cualquier disputa se resolverá ante los tribunales
            competentes de Chile.
          </p>
        </div>

        <div>
          <h2 className="text-[19px] font-bold text-ink">10. Contacto</h2>
          <p className="mt-3">
            ¿Dudas sobre estos términos? Escribinos a{" "}
            <a className="text-violet-light underline" href="mailto:vicentecastroibarra@gmail.com">vicentecastroibarra@gmail.com</a>.
          </p>
        </div>
      </div>
    </section>
  );
}
