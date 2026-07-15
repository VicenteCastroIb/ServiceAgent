import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { BLOG_POSTS, buscarPost } from "@/lib/blogPosts";
import { ClockIcon } from "@/lib/icons";

interface PageProps {
  params: Promise<{ slug: string }>;
}

export function generateStaticParams() {
  return BLOG_POSTS.map((post) => ({ slug: post.slug }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const post = buscarPost(slug);
  if (!post) {
    return {};
  }
  return {
    title: `${post.titulo} — ServiceAgent`,
    description: post.extracto,
  };
}

export default async function BlogArticlePage({ params }: PageProps) {
  const { slug } = await params;
  const post = buscarPost(slug);
  if (!post) {
    notFound();
  }

  return (
    <article className="mx-auto max-w-2xl px-6 py-16">
      <Link href="/blog" className="text-sm font-medium text-slate-500 transition hover:text-slate-900">
        ← Volver al blog
      </Link>

      <div className="mt-6 flex flex-wrap items-center gap-3 text-xs text-slate-500">
        <span className="rounded-full bg-emerald-50 px-2.5 py-1 font-medium text-emerald-700">{post.categoria}</span>
        <span className="flex items-center gap-1">
          <ClockIcon width={14} height={14} /> {post.minutos} min
        </span>
        <span>{post.fecha}</span>
      </div>

      <h1 className="mt-4 font-heading text-3xl leading-tight font-semibold text-slate-900 sm:text-4xl">
        {post.titulo}
      </h1>

      <div className="mt-8 space-y-5">
        {post.contenido.map((bloque, i) => {
          if (bloque.tipo === "subtitulo") {
            return (
              <h2 key={i} className="pt-3 font-heading text-xl font-semibold text-slate-900">
                {bloque.texto}
              </h2>
            );
          }
          if (bloque.tipo === "enlace-interno") {
            return (
              <p key={i} className="leading-[27px] text-slate-600">
                {bloque.texto}{" "}
                <Link href={bloque.href} className="font-medium text-emerald-600 underline underline-offset-2">
                  {bloque.enlaceTexto}
                </Link>
              </p>
            );
          }
          return (
            <p key={i} className="leading-[27px] text-slate-600">
              {bloque.texto}
            </p>
          );
        })}
      </div>

      <div className="mt-12 border-t border-slate-100 pt-8">
        <Link href="/blog" className="text-sm font-medium text-emerald-600 underline underline-offset-2">
          ← Ver todos los artículos
        </Link>
      </div>
    </article>
  );
}
