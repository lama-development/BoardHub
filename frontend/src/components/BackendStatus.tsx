import type { HealthStatus } from "../types";

type BackendStatusProps = {
  status: HealthStatus;
};

export function BackendStatus({ status }: BackendStatusProps) {
  const config = {
    UP: {
      label: "Backend attivo",
      dotClassName: "bg-emerald-500",
      className: "border-emerald-200 bg-emerald-50 text-emerald-700",
    },
    DOWN: {
      label: "Backend non attivo",
      dotClassName: "bg-red-500",
      className: "border-red-200 bg-red-50 text-red-700",
    },
    UNKNOWN: {
      label: "Backend incerto",
      dotClassName: "bg-amber-500",
      className: "border-amber-200 bg-amber-50 text-amber-700",
    },
  }[status];

  return (
    <div className={`inline-flex h-7 w-fit items-center gap-2 rounded-md border px-2.5 text-xs font-medium ${config.className}`}>
      <span className={`h-2 w-2 rounded-full ${config.dotClassName}`} aria-hidden="true" />
      <span>{config.label}</span>
    </div>
  );
}
