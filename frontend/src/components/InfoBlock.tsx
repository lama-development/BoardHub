import type { ReactNode } from "react";

type InfoBlockProps = {
  title: string;
  children: ReactNode;
};

export function InfoBlock({ title, children }: InfoBlockProps) {
  return (
    <section className="border-b border-slate-200 px-4 py-3 last:border-b-0">
      <h3 className="mb-2 text-xs font-normal uppercase text-slate-500">{title}</h3>
      <div className="wrap-break-word text-sm text-slate-800">{children}</div>
    </section>
  );
}
