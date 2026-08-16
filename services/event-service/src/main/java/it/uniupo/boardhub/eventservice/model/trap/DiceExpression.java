package it.uniupo.boardhub.eventservice.model.trap;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DiceExpression(int count, int sides, int modifier) {

    private static final Pattern PATTERN = Pattern.compile("^(\\d{1,2})d(\\d{1,4})([+-]\\d{1,4})?$", Pattern.CASE_INSENSITIVE);

    public DiceExpression {
        if (count < 1 || count > 20) {
            throw new IllegalArgumentException("Il numero di dadi deve essere compreso tra 1 e 20.");
        }
        if (sides < 2 || sides > 1000) {
            throw new IllegalArgumentException("Le facce del dado devono essere comprese tra 2 e 1000.");
        }
        if (modifier < -1000 || modifier > 1000) {
            throw new IllegalArgumentException("Il modificatore deve essere compreso tra -1000 e 1000.");
        }
    }

    public static DiceExpression parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("damageExpression e obbligatoria.");
        }
        Matcher matcher = PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("damageExpression deve usare il formato NdS, NdS+M oppure NdS-M.");
        }
        int modifier = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new DiceExpression(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                modifier
        );
    }
}
