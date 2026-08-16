package it.uniupo.boardhub.eventservice.model.grid;

public enum TrapVisibility {
    HIDDEN,
    REVEALED,
    KEEP_DETAILS_HIDDEN,
    // Alias accettato sui vecchi contratti; in persistenza viene normalizzato.
    ALWAYS_HIDDEN;

    public TrapVisibility normalized() {
        return this == ALWAYS_HIDDEN ? KEEP_DETAILS_HIDDEN : this;
    }
}
