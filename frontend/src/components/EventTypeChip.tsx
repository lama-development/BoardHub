type EventTypeChipProps = {
  type: string;
  count?: number;
  className?: string;
};

const EVENT_TYPE_COLORS: Record<string, string> = {
  SESSION_START: "bg-[#8fe8f4]",
  MOVE: "bg-[#ffd400]",
  MOVE_CONFIRMED: "bg-[#b8ee72]",
  SPAWN_MONSTER: "bg-[#ff8bc7]",
  ATTACK: "bg-[#ffb45c]",
  DAMAGE: "bg-[#ff7272]",
  ROUND_END: "bg-[#c8b1ff]",
};

export function eventTypeColor(eventType: string) {
  return EVENT_TYPE_COLORS[eventType] ?? "bg-[#f7f1e4]";
}

export function EventTypeChip({ type, count, className = "" }: EventTypeChipProps) {
  return (
    <span
      className={`inline-flex min-h-7 w-fit items-center gap-2 rounded-[2px] border-2 border-[#111111] px-2.5 py-1 text-[12px] font-extrabold uppercase leading-none tracking-[0.035em] text-[#111111] ${eventTypeColor(type)} ${className}`}
    >
      <span className="wrap-break-word">{type}</span>
      {count === undefined ? null : (
        <b className="border-l-2 border-[#111111] pl-2 font-black">{count}</b>
      )}
    </span>
  );
}
