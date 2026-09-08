import * as React from "react";
import type { BoardToken } from "../types";

const columns = ["A", "B", "C", "D", "E"];
const rows = [5, 4, 3, 2, 1];

type BoardGridProps = {
  tokens: BoardToken[];
  selectedCell?: string | null;
  onSelectCell?: (cell: string) => void;
  selectableCells?: Iterable<string>;
  disabledCells?: Iterable<string>;
  selectionHint?: string;
};

export function BoardGrid({
  tokens,
  selectedCell: controlledSelectedCell,
  onSelectCell,
  selectableCells,
  disabledCells,
  selectionHint,
}: BoardGridProps) {
  const [uncontrolledSelectedCell, setUncontrolledSelectedCell] =
    React.useState<string | null>(null);
  const selectedCell = controlledSelectedCell ?? uncontrolledSelectedCell;
  const selectableCellSet = React.useMemo(
    () => (selectableCells ? new Set(selectableCells) : null),
    [selectableCells],
  );
  const disabledCellSet = React.useMemo(
    () => new Set(disabledCells),
    [disabledCells],
  );
  const tokensByCell = new Map<string, BoardToken[]>();

  for (const token of tokens) {
    tokensByCell.set(token.cell, [
      ...(tokensByCell.get(token.cell) ?? []),
      token,
    ]);
  }

  const selectedTokens = selectedCell
    ? (tokensByCell.get(selectedCell) ?? [])
    : [];

  return (
    <div className="p-3 sm:p-4">
      <div className="mx-auto grid max-w-90 grid-cols-[20px_repeat(5,minmax(30px,1fr))] gap-1 sm:max-w-105 sm:grid-cols-[22px_repeat(5,minmax(38px,1fr))]">
        <div />
        {columns.map((column) => (
          <div
            className="text-center text-xs font-normal text-muted"
            key={column}
          >
            {column}
          </div>
        ))}

        {rows.map((row) => (
          <GridRow
            key={row}
            onSelectCell={(cell) => {
              if (onSelectCell) {
                onSelectCell(cell);
              } else {
                setUncontrolledSelectedCell(cell);
              }
            }}
            row={row}
            selectedCell={selectedCell}
            selectableCells={selectableCellSet}
            disabledCells={disabledCellSet}
            tokensByCell={tokensByCell}
          />
        ))}
      </div>

      <div className="mt-3 flex flex-col gap-2 border-t-2 border-ink pt-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-3 text-xs font-normal text-muted">
          <LegendToken kind="character" label="Personaggio" />
          <LegendToken kind="monster" label="Mostro" />
        </div>
        <div className="text-xs text-muted">
          {selectionHint ??
            (selectedCell ? (
              <span>
                <span className="font-medium text-ink">
                  {selectedCell}
                </span>
                {selectedTokens.length > 0
                  ? ` - ${selectedTokens.map((token) => token.id).join(", ")}`
                  : " - vuota"}
              </span>
            ) : (
              <span>Seleziona una cella</span>
            ))}
        </div>
      </div>
    </div>
  );
}

function GridRow({
  onSelectCell,
  row,
  selectedCell,
  selectableCells,
  disabledCells,
  tokensByCell,
}: {
  onSelectCell: (cell: string) => void;
  row: number;
  selectedCell: string | null;
  selectableCells: Set<string> | null;
  disabledCells: Set<string>;
  tokensByCell: Map<string, BoardToken[]>;
}) {
  return (
    <>
      <div className="flex items-center justify-center text-xs font-normal text-muted">
        {row}
      </div>
      {columns.map((column) => {
        const cell = `${column}${row}`;
        const tokens = tokensByCell.get(cell) ?? [];
        const isSelectable =
          selectableCells?.has(cell) ?? !disabledCells.has(cell);

        return (
          <button
            aria-label={`Cella ${cell}${isSelectable ? "" : ", non disponibile"}`}
            aria-pressed={selectedCell === cell}
            className={`flex aspect-square min-h-8 items-center justify-center rounded-xs border-2 border-ink p-0.5 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent sm:min-h-10 sm:p-1 ${
              selectedCell === cell
                ? "-translate-y-0.5 bg-warning shadow-[3px_3px_0_var(--color-ink)]"
                : isSelectable
                  ? "cursor-pointer bg-canvas hover:bg-info/20 hover:shadow-[2px_2px_0_var(--color-ink)]"
                  : "cursor-not-allowed bg-muted/10 opacity-45"
            }`}
            disabled={!isSelectable}
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
      className={`h-4 w-4 rounded-full border border-ink shadow-[1px_1px_0_var(--color-ink)] sm:h-5 sm:w-5 ${token.kind === "character" ? "bg-info" : "bg-primary"}`}
      title={title}
    />
  );
}

function LegendToken({
  kind,
  label,
}: {
  kind: BoardToken["kind"];
  label: string;
}) {
  return (
    <span className="inline-flex items-center gap-2">
      {kind === "character" ? (
        <span className="h-2.5 w-2.5 rounded-full border border-ink bg-info" />
      ) : (
        <span className="h-2.5 w-2.5 rounded-full border border-ink bg-primary" />
      )}
      {label}
    </span>
  );
}
