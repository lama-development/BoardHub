import { AlertCircle, Check, Dices, LoaderCircle, Play } from "lucide-react";
import type { TrapResolution, TrapRollResult } from "../types";
import { AlertBanner, Button, StatusChip } from "./ui";

type TrapResolutionCardProps = {
  resolution: TrapResolution;
  roll: TrapRollResult | null;
  isBusy: boolean;
  onRoll: () => void;
  onContinue: () => void;
};

export function TrapResolutionCard({
  resolution,
  roll,
  isBusy,
  onRoll,
  onContinue,
}: TrapResolutionCardProps) {
  const canRoll = resolution.status === "AWAITING_ROLL";
  const canContinue =
    resolution.status === "AWAITING_CONTINUATION" &&
    roll?.movementDecision === "CONTINUE";

  return (
    <section className="bh-surface mt-5 overflow-hidden" aria-live="polite">
      <div className="flex flex-col gap-3 border-b-2 border-ink bg-warning/40 p-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="bh-card-icon">
            <AlertCircle size={19} aria-hidden="true" />
          </span>
          <div>
            <p className="text-xs font-bold uppercase tracking-wide text-muted">
              Movimento interrotto
            </p>
            <h2 className="mt-1 text-xl">
              Trappola attivata in {resolution.triggerCell}
            </h2>
          </div>
        </div>
        <StatusChip tone="warning">
          {resolution.movementRemaining} mov. residui
        </StatusChip>
      </div>
      <div className="p-4">
        {canRoll ? (
          <p className="text-sm leading-6 text-muted">
            Il server deve eseguire il tiro salvezza prima che il movimento
            possa proseguire.
          </p>
        ) : null}
        {roll ? (
          <AlertBanner
            className="mt-1"
            tone={roll.success ? "success" : "danger"}
            icon={
              roll.success ? <Check size={18} /> : <AlertCircle size={18} />
            }
          >
            <strong>
              {roll.success
                ? "Tiro salvezza riuscito."
                : "Tiro salvezza fallito."}
            </strong>{" "}
            D20 {roll.selectedD20} + {roll.saveBonus} = {roll.saveTotal}. Danni:{" "}
            {roll.damageTotal}; PF attuali: {roll.hpCurrent}.
          </AlertBanner>
        ) : null}
        {roll?.movementDecision === "STOP" ? (
          <p className="mt-3 text-sm font-semibold text-muted">
            Il movimento termina qui per l&apos;esito della trappola.
          </p>
        ) : null}
        <div className="mt-4 flex flex-wrap gap-2">
          {canRoll ? (
            <Button
              variant="danger"
              disabled={isBusy}
              onClick={onRoll}
              icon={
                isBusy ? (
                  <LoaderCircle className="animate-spin" size={17} />
                ) : (
                  <Dices size={17} />
                )
              }
            >
              Esegui tiro salvezza
            </Button>
          ) : null}
          {canContinue ? (
            <Button
              variant="primary"
              disabled={isBusy}
              onClick={onContinue}
              icon={
                isBusy ? (
                  <LoaderCircle className="animate-spin" size={17} />
                ) : (
                  <Play size={17} />
                )
              }
            >
              Prosegui movimento
            </Button>
          ) : null}
        </div>
      </div>
    </section>
  );
}
