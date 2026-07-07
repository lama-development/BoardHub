import * as React from "react";
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
    <section className="overflow-hidden rounded-md border border-slate-200 bg-white">
      <div className="flex min-h-11 flex-col gap-2 border-b border-slate-200 px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between sm:px-4">
        <h2 className="text-base font-medium text-slate-950">Sessione</h2>
        <div className="grid w-full grid-cols-2 rounded-md border border-slate-200 bg-slate-50 p-0.5 sm:inline-grid sm:w-auto">
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
      className={`h-8 cursor-pointer rounded px-3 text-sm transition-colors ${
        active ? "bg-white text-slate-950" : "text-slate-500 hover:text-slate-900"
      }`}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}
