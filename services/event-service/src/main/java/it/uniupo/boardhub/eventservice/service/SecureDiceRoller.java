package it.uniupo.boardhub.eventservice.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureDiceRoller implements DiceRoller {

    private final SecureRandom random = new SecureRandom();

    @Override
    public int roll(int sides) {
        if (sides < 2) {
            throw new IllegalArgumentException("Il dado deve avere almeno due facce.");
        }
        return random.nextInt(sides) + 1;
    }
}
