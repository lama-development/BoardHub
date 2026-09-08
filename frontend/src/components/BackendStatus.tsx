import type { HealthStatus } from "../types";
import { StatusChip } from "./ui";

type BackendStatusProps = {
  status: HealthStatus;
};

export function BackendStatus({ status }: BackendStatusProps) {
  const config = {
    UP: {
      label: "Backend online",
      tone: "success" as const,
    },
    DOWN: {
      label: "Backend offline",
      tone: "danger" as const,
    },
    UNKNOWN: {
      label: "Backend incerto",
      tone: "warning" as const,
    },
  }[status];

  return (
    <StatusChip tone={config.tone} dot>
      {config.label}
    </StatusChip>
  );
}
