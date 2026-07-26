package it.uniupo.boardhub.eventservice.controller.dto;

// Dati minimi inviati dal dispositivo del giocatore dopo la scansione del QR.
public record CreateJoinRequestRequest(String playerReference, String displayName) {
}
