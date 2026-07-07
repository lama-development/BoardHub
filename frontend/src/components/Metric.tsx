import type { ReactNode } from "react";

type MetricProps = {
  icon: ReactNode;
  label: string;
  value: string;
};

export function Metric({ icon, label, value }: MetricProps) {
  return (
    <div className="flex min-h-14 items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2">
      <span
        className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-slate-100 text-slate-600"
        aria-hidden="true"
      >
        {icon}
      </span>
      <span className="min-w-0">
        <small className="block text-xs font-normal text-slate-500">{label}</small>
        <strong className="mt-0.5 block truncate text-sm font-medium leading-tight text-slate-950">{value}</strong>
      </span>
    </div>
  );
}
