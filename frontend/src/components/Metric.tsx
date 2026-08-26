import type { ReactNode } from "react";
import { SummaryCard, type SummaryAccent } from "./ui";

type MetricProps = {
  icon: ReactNode;
  label: string;
  value: string;
  accent?: SummaryAccent;
};

export function Metric({ icon, label, value, accent }: MetricProps) {
  return (
    <SummaryCard accent={accent} icon={icon} label={label}>
      <strong className="block truncate text-base font-extrabold leading-tight text-[#111111]">
        {value}
      </strong>
    </SummaryCard>
  );
}
