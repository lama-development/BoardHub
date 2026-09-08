type EventTypeChipProps = {
  type: string;
  count?: number;
  className?: string;
};

const EVENT_TYPE_COLORS: Record<string, string> = {
  SESSION_START: "bg-info/40",
  MOVE: "bg-warning",
  MOVE_CONFIRMED: "bg-success/40",
  SPAWN_MONSTER: "bg-primary/40",
  ATTACK: "bg-warning/40",
  DAMAGE: "bg-danger/40",
  ROUND_END: "bg-accent/40",
};

export function eventTypeColor(eventType: string) {
  return EVENT_TYPE_COLORS[eventType] ?? "bg-muted/10";
}

export function EventTypeChip({
  type,
  count,
  className = "",
}: EventTypeChipProps) {
  return (
    <span
      className={`inline-flex min-h-7 w-fit items-center gap-2 rounded-xs border-2 border-ink px-2.5 py-1 text-[12px] font-extrabold uppercase leading-none tracking-[0.035em] text-ink ${eventTypeColor(type)} ${className}`}
    >
      <span className="wrap-break-word">{type}</span>
      {count === undefined ? null : (
        <b className="border-l-2 border-ink pl-2 font-black">{count}</b>
      )}
    </span>
  );
}
