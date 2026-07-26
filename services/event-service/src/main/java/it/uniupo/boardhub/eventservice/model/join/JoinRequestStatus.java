package it.uniupo.boardhub.eventservice.model.join;

// Stati ammessi per una richiesta di ingresso sottoposta al DM.
public enum JoinRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
