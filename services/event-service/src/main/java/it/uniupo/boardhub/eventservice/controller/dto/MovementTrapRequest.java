package it.uniupo.boardhub.eventservice.controller.dto;

public record MovementTrapRequest(String trapId, String cell, String visibility, boolean armed) {
}
