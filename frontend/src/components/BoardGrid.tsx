import * as React from "react";
import type { BoardToken } from "../types";

const columns = ["A", "B", "C", "D", "E"];
const rows = [5, 4, 3, 2, 1];

type BoardGridProps = {
  tokens: BoardToken[];
};

export function BoardGrid({ tokens }: BoardGridProps) {
  const [selectedCell, setSelectedCell] = React.useState<string | null>(null);
  const tokensByCell = new Map<string, BoardToken[]>();

  for (const token of tokens) {
    tokensByCell.set(token.cell, [...(tokensByCell.get(token.cell) ?? []), token]);
  }

  const selectedTokens = selectedCell ? (tokensByCell.get(selectedCell) ?? []) : [];

  return (
    <div className="p-3 sm:p-4">
      <div className="mx-auto grid max-w-90 grid-cols-[20px_repeat(5,minmax(30px,1fr))] gap-1 sm:max-w-105 sm:grid-cols-[22px_repeat(5,minmax(38px,1fr))]">
        <div />
        {columns.map((column) => (
          <div className="text-center text-xs font-normal text-slate-500" key={column}>
            {column}
          </div>
        ))}

        {rows.map((row) => (
          <GridRow
            key={row}
            onSelectCell={setSelectedCell}
            row={row}
            selectedCell={selectedCell}
            tokensByCell={tokensByCell}
          />
        ))}
      </div>

      <div className="mt-3 flex flex-col gap-2 border-t border-slate-100 pt-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-3 text-xs font-normal text-slate-500">
          <LegendToken kind="character" label="Personaggio" />
          <LegendToken kind="monster" label="Mostro" />
        </div>
        <div className="text-xs text-slate-500">
          {selectedCell ? (
            <span>
              <span className="font-medium text-slate-800">{selectedCell}</span>
              {selectedTokens.length > 0 ? ` - ${selectedTokens.map((token) => token.id).join(", ")}` : " - vuota"}
            </span>
          ) : (
            <span>Seleziona una cella</span>
          )}
        </div>
      </div>
    </div>
  );
}

function GridRow({
  onSelectCell,
  row,
  selectedCell,
  tokensByCell,
}: {
  onSelectCell: (cell: string) => void;
  row: number;
  selectedCell: string | null;
  tokensByCell: Map<string, BoardToken[]>;
}) {
  return (
    <>
      <div className="flex items-center justify-center text-xs font-normal text-slate-500">{row}</div>
      {columns.map((column) => {
        const cell = `${column}${row}`;
        const tokens = tokensByCell.get(cell) ?? [];

        return (
          <button
            className={`flex aspect-square min-h-8 cursor-pointer items-center justify-center rounded-md border p-0.5 transition-colors sm:min-h-10 sm:p-1 ${
              selectedCell === cell
                ? "border-slate-900 bg-white"
                : "border-slate-200 bg-slate-50 hover:border-slate-400 hover:bg-white"
            }`}
            key={cell}
            onClick={() => onSelectCell(cell)}
            title={cell}
            type="button"
          >
            <div className="flex flex-wrap items-center justify-center gap-1">
              {tokens.map((token) => (
                <BoardPiece cell={cell} key={token.id} token={token} />
              ))}
            </div>
          </button>
        );
      })}
    </>
  );
}

function BoardPiece({ cell, token }: { cell: string; token: BoardToken }) {
  const title = `${token.id} - ${token.kind === "character" ? "Personaggio" : "Mostro"} - ${cell}`;

  return (
    <span
      className={`h-4 w-4 rounded-full sm:h-5 sm:w-5 ${token.kind === "character" ? "bg-slate-900" : "bg-rose-600"}`}
      title={title}
    />
  );
}

function LegendToken({ kind, label }: { kind: BoardToken["kind"]; label: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      {kind === "character" ? (
        <span className="h-2.5 w-2.5 rounded-full bg-slate-900" />
      ) : (
        <span className="h-2.5 w-2.5 rounded-full bg-rose-600" />
      )}
      {label}
    </span>
  );
}
