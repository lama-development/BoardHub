import * as React from "react";
import { LayoutGrid } from "lucide-react";
import type { BoardToken, GameEvent } from "../types";
import { BoardGrid } from "./BoardGrid";
import { EventLog } from "./EventLog";

type ActiveTab = "grid" | "events";

type SessionTabsProps = {
  events: GameEvent[];
  isLoading: boolean;
  tokens: BoardToken[];
};

export function SessionTabs({ events, isLoading, tokens }: SessionTabsProps) {
  const [activeTab, setActiveTab] = React.useState<ActiveTab>("grid");

  return (
    <section className="bh-surface overflow-hidden">
      <div className="flex min-h-14 flex-col gap-3 border-b-2 border-[#111111] bg-[#ff8bc7] px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <span className="bh-card-icon"><LayoutGrid size={18} /></span>
          <h2 className="text-xl">Tavolo di gioco</h2>
        </div>
        <div className="grid w-full grid-cols-2 rounded-[2px] border-2 border-[#111111] bg-white p-1 sm:inline-grid sm:w-auto">
          <TabButton active={activeTab === "grid"} onClick={() => setActiveTab("grid")}>
            Griglia
          </TabButton>
          <TabButton active={activeTab === "events"} onClick={() => setActiveTab("events")}>
            Eventi
          </TabButton>
        </div>
      </div>

      {activeTab === "grid" ? <BoardGrid tokens={tokens} /> : <EventLog events={events} isLoading={isLoading} />}
    </section>
  );
}

function TabButton({
  active,
  children,
  onClick,
}: {
  active: boolean;
  children: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      className={`h-8 cursor-pointer rounded-[1px] px-3 text-xs font-extrabold uppercase tracking-[0.05em] transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9165ff] ${
        active ? "bg-[#ff3b9d] text-[#111111]" : "text-slate-600 hover:bg-[#fff2a7] hover:text-[#111111]"
      }`}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}
