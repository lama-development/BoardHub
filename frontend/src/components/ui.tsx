import type {
  ButtonHTMLAttributes,
  HTMLAttributes,
  ReactNode,
} from "react";

function joinClassNames(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(" ");
}

type ButtonVariant = "primary" | "secondary" | "danger" | "success" | "ghost";
type ButtonSize = "compact" | "default";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
};

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: "bg-[#111111] text-white hover:bg-[#ff3b9d] hover:text-[#111111]",
  secondary: "bg-white text-[#111111] hover:bg-[#ffd400]",
  danger: "bg-[#ff3b9d] text-[#111111] hover:bg-[#ff74b9]",
  success: "bg-[#9be33a] text-[#111111] hover:bg-[#b5ef69]",
  ghost: "border-transparent bg-transparent text-[#111111] shadow-none hover:border-[#111111] hover:bg-white hover:shadow-[2px_2px_0_#111111]",
};

const BUTTON_SIZES: Record<ButtonSize, string> = {
  compact: "h-9 px-3 text-sm",
  default: "h-10 px-4 text-sm",
};

export function Button({
  variant = "secondary",
  size = "default",
  icon,
  className,
  children,
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      className={joinClassNames(
        "inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-[2px] border-2 border-[#111111] font-extrabold uppercase tracking-[0.045em] shadow-[3px_3px_0_#111111] transition-all duration-150 hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-[2px_2px_0_#111111] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9165ff] focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none disabled:hover:translate-x-0 disabled:hover:translate-y-0",
        BUTTON_VARIANTS[variant],
        BUTTON_SIZES[size],
        className,
      )}
      type={type}
      {...props}
    >
      {icon}
      {children}
    </button>
  );
}

type StatusTone = "neutral" | "info" | "success" | "warning" | "danger";

type StatusChipProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: StatusTone;
  dot?: boolean;
};

const STATUS_TONES: Record<StatusTone, { container: string; dot: string }> = {
  neutral: {
    container: "bg-white text-[#111111]",
    dot: "bg-[#111111]",
  },
  info: {
    container: "bg-[#8fe8f4] text-[#111111]",
    dot: "bg-[#04aeca]",
  },
  success: {
    container: "bg-[#b8ee72] text-[#111111]",
    dot: "bg-[#4f8a00]",
  },
  warning: {
    container: "bg-[#ffd400] text-[#111111]",
    dot: "bg-[#111111]",
  },
  danger: {
    container: "bg-[#ff8bc7] text-[#111111]",
    dot: "bg-[#c80063]",
  },
};

export function StatusChip({
  tone = "neutral",
  dot = false,
  className,
  children,
  ...props
}: StatusChipProps) {
  const config = STATUS_TONES[tone];
  return (
    <span
      className={joinClassNames(
        "inline-flex min-h-7 w-fit items-center gap-2 rounded-[2px] border-2 border-[#111111] px-2.5 py-1 text-[12px] font-extrabold uppercase leading-none tracking-[0.04em] shadow-[2px_2px_0_#111111]",
        config.container,
        className,
      )}
      {...props}
    >
      {dot ? <span className={joinClassNames("h-1.5 w-1.5 rounded-full", config.dot)} aria-hidden="true" /> : null}
      {children}
    </span>
  );
}

type AlertBannerProps = HTMLAttributes<HTMLDivElement> & {
  tone?: "info" | "success" | "warning" | "danger";
  icon?: ReactNode;
};

const ALERT_TONES = {
  info: "bg-[#d8f7fb] text-[#111111]",
  success: "bg-[#e2f9c5] text-[#111111]",
  warning: "bg-[#fff2a7] text-[#111111]",
  danger: "bg-[#ffd3e9] text-[#111111]",
};

export function AlertBanner({
  tone = "info",
  icon,
  className,
  children,
  ...props
}: AlertBannerProps) {
  return (
    <div
      className={joinClassNames(
        "flex items-start gap-2.5 rounded-[2px] border-2 border-[#111111] px-3.5 py-3 text-sm leading-5 shadow-[3px_3px_0_#111111]",
        ALERT_TONES[tone],
        className,
      )}
      {...props}
    >
      {icon ? <span className="mt-0.5 shrink-0" aria-hidden="true">{icon}</span> : null}
      <div className="min-w-0">{children}</div>
    </div>
  );
}

type PageHeaderIdentityProps = {
  icon: ReactNode;
  eyebrow: string;
  title: ReactNode;
  accentClassName: string;
};

export function PageHeaderIdentity({
  icon,
  eyebrow,
  title,
  accentClassName,
}: PageHeaderIdentityProps) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <span
        className={joinClassNames(
          "grid h-10 w-10 shrink-0 place-items-center rounded-[2px] border-2 border-[#111111] text-[#111111] shadow-[2px_2px_0_#111111]",
          accentClassName,
        )}
        aria-hidden="true"
      >
        {icon}
      </span>
      <div className="flex min-h-10 min-w-0 flex-col justify-center">
        <p className="truncate text-[12px] font-bold uppercase leading-none tracking-[0.04em] text-slate-600">
          {eyebrow}
        </p>
        <h1 className="mt-1.5 truncate text-2xl">{title}</h1>
      </div>
    </div>
  );
}

export type SummaryAccent = "pink" | "yellow" | "cyan" | "purple" | "lime";

type SummaryCardProps = {
  icon: ReactNode;
  label: string;
  accent?: SummaryAccent;
  children: ReactNode;
};

const SUMMARY_ACCENTS: Record<SummaryAccent, string> = {
  pink: "bh-card-icon--pink",
  yellow: "bh-card-icon--yellow",
  cyan: "bh-card-icon--cyan",
  purple: "bh-card-icon--purple",
  lime: "bh-card-icon--lime",
};

export function SummaryCard({
  icon,
  label,
  accent = "yellow",
  children,
}: SummaryCardProps) {
  return (
    <div className="bh-surface grid min-h-18 grid-cols-[auto_minmax(0,1fr)] items-center gap-3 p-4">
      <span className={joinClassNames("bh-card-icon", SUMMARY_ACCENTS[accent])} aria-hidden="true">
        {icon}
      </span>
      <div className="min-w-0">
        <p className="text-[12px] font-bold uppercase leading-[1.25] tracking-[0.035em] text-slate-700">
          {label}
        </p>
        <div className="mt-0.5 flex min-w-0 items-center">{children}</div>
      </div>
    </div>
  );
}

type SurfaceProps = HTMLAttributes<HTMLElement> & {
  as?: "section" | "header" | "aside" | "div";
};

export function Surface({
  as: Element = "section",
  className,
  ...props
}: SurfaceProps) {
  return (
    <Element
      className={joinClassNames(
        "rounded-[2px] border-2 border-[#111111] bg-white shadow-[4px_4px_0_#111111]",
        className,
      )}
      {...props}
    />
  );
}
