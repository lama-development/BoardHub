import type { ReactNode } from "react";

type InfoBlockProps = {
  title: string;
  children: ReactNode;
};

export function InfoBlock({ title, children }: InfoBlockProps) {
  return (
    <section className="border-b-2 border-[#111111] px-4 py-3.5 last:border-b-0">
      <p className="mb-2 text-[12px] font-bold uppercase leading-tight tracking-[0.035em] text-slate-600">
        {title}
      </p>
      <div className="wrap-break-word text-sm text-slate-800">{children}</div>
    </section>
  );
}
