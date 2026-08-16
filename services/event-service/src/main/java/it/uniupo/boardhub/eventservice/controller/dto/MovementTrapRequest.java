package it.uniupo.boardhub.eventservice.controller.dto;

public record MovementTrapRequest(
        String trapId,
        String cell,
        String visibility,
        boolean armed,
        String lifecyclePolicy,
        String saveAbility,
        Integer saveDc,
        String rollMode,
        String damageExpression,
        String successDamage,
        String successMovement,
        String failureDamage,
        String failureMovement
) {
}
