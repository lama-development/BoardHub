package it.uniupo.boardhub.eventservice.controller.dto;

// Contratto pubblico usato dalla pagina aperta tramite QR.
public record PublicTableStatusResponse(
        String tablePublicId,
        int tableNumber,
        String tableDisplayName,
        String status,
        PublicActiveSessionResponse activeSession
) {
}
