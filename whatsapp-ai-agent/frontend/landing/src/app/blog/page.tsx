import type { Metadata } from "next";
import Link from "next/link";
import { BLOG_POSTS } from "@/lib/blogPosts";
import { ClockIcon } from "@/lib/icons";

export const metadata: Metadata = {
  title: "Blog — ServiceAgent",
  description: "Guías directas, sin tecnicismos, para automatizar la atención de tu negocio por WhatsApp.",
};

export default function BlogPage() {
  const destacado = BLOG_POSTS.find((post) => post.destacado);
  const resto = BLOG_POSTS.filter((post) => !post.destacado);

  return (
    <div className="bg-white">
    <div className="mx-auto max-w-6xl px-6 py-16">
      <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
        Blog
      </span>
      <h1 className="mt-4 font-heading text-[36px] font-semibold text-slate-900 sm:text-[44px]">
        WhatsApp, chatbots e IA, explicados para tu negocio
      </h1>
      <p className="mt-4 max-w-xl text-slate-500">
        Guías directas, sin tecnicismos, para que entiendas cómo automatizar la atención de tu negocio y
        dejar de perder clientes.
      </p>

      {destacado && (
        <Link
          href={`/blog/${destacado.slug}`}
          className="relative mt-10 block overflow-hidden rounded-3xl bg-emerald-50 p-10"
        >
          <span
            aria-hidden="true"
            className="pointer-events-none absolute -right-6 -bottom-10 font-heading text-[140px] leading-none font-semibold text-emerald-100 select-none"
          >
            ServiceAgent.
          </span>
          <span className="relative inline-flex items-center rounded-full bg-white px-3 py-1 text-xs font-medium text-emerald-700">
            {destacado.categoria}
          </span>
          <h2 className="relative mt-4 max-w-xl font-heading text-2xl font-semibold text-slate-900 sm:text-3xl">
            {destacado.titulo}
          </h2>
          <p className="relative mt-3 max-w-lg text-sm text-slate-600">{destacado.extracto}</p>
          <p className="relative mt-4 flex items-center gap-1.5 text-xs text-slate-500">
            <ClockIcon width={14} height={14} /> {destacado.minutos} min · {destacado.fecha}
          </p>
          <span className="relative mt-4 inline-block text-sm font-medium text-emerald-700">Leer artículo →</span>
        </Link>
      )}

      <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {resto.map((post) => (
          <Link
            key={post.slug}
            href={`/blog/${post.slug}`}
            className="rounded-2xl border border-slate-100 p-6 transition hover:shadow-md"
          >
            <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
              {post.categoria}
            </span>
            <h3 className="mt-3 font-heading text-lg font-semibold text-slate-900">{post.titulo}</h3>
            <p className="mt-2 text-sm text-slate-500">{post.extracto}</p>
            <p className="mt-4 flex items-center gap-1.5 text-xs text-slate-400">
              <ClockIcon width={14} height={14} /> {post.minutos} min · {post.fecha}
            </p>
          </Link>
        ))}
      </div>
    </div>
    </div>
  );
}
