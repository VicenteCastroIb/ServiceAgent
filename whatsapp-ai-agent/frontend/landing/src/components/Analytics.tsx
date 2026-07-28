import Script from "next/script";

/**
 * Google Analytics 4 y Meta Pixel, para poder medir conversión de campañas de
 * ads (registro → pago) antes de gastar presupuesto en publicidad.
 *
 * Ambos son opcionales a propósito: si NEXT_PUBLIC_GA_ID / NEXT_PUBLIC_META_PIXEL_ID
 * no están cargadas (todavía no se crearon las cuentas), simplemente no se
 * renderiza el script correspondiente - no rompe nada, no hace falta tocar
 * código para activarlos más adelante, solo cargar la variable de entorno en
 * Railway (ver DEPLOY.md) y volver a desplegar (NEXT_PUBLIC_* se hornea en
 * build time, no en runtime).
 *
 * strategy="afterInteractive": carga después de que la página sea interactiva,
 * para no competir con el contenido por el ancho de banda inicial (mejor LCP).
 */
export default function Analytics() {
  const gaId = process.env.NEXT_PUBLIC_GA_ID;
  const metaPixelId = process.env.NEXT_PUBLIC_META_PIXEL_ID;

  return (
    <>
      {gaId && (
        <>
          <Script src={`https://www.googletagmanager.com/gtag/js?id=${gaId}`} strategy="afterInteractive" />
          <Script id="ga4-init" strategy="afterInteractive">
            {`
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());
              gtag('config', '${gaId}');
            `}
          </Script>
        </>
      )}

      {metaPixelId && (
        <Script id="meta-pixel-init" strategy="afterInteractive">
          {`
            !function(f,b,e,v,n,t,s)
            {if(f.fbq)return;n=f.fbq=function(){n.callMethod?
            n.callMethod.apply(n,arguments):n.queue.push(arguments)};
            if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';
            n.queue=[];t=b.createElement(e);t.async=!0;
            t.src=v;s=b.getElementsByTagName(e)[0];
            s.parentNode.insertBefore(t,s)}(window, document,'script',
            'https://connect.facebook.net/en_US/fbevents.js');
            fbq('init', '${metaPixelId}');
            fbq('track', 'PageView');
          `}
        </Script>
      )}
    </>
  );
}
