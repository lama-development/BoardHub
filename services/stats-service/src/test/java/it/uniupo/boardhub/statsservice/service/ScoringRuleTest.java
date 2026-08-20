package it.uniupo.boardhub.statsservice.service;

import it.uniupo.boardhub.statsservice.model.PlayerResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringRuleTest {

    private PlayerResult player(boolean survived, int savesSucceeded) {
        return new PlayerResult(
                "session-001", "player-device-01", "Andrea", "Elaria", "Ladro", "Elfo",
                3, survived, 0, 0, 0, savesSucceeded, 0, 0, 0
        );
    }

    @Test
    void sopravvivereSenzaTiriValeCinquePunti() {
        // 3 per la sessione completata piu 2 per la sopravvivenza.
        assertThat(ScoringRule.pointsFor(player(true, 0))).isEqualTo(5);
    }

    @Test
    void ogniTiroSalvezzaRiuscitoAggiungeUnPunto() {
        assertThat(ScoringRule.pointsFor(player(true, 3))).isEqualTo(8);
    }

    @Test
    void essereAbbattutoSottraeUnPunto() {
        // 3 per la sessione completata meno 1 per essere stato abbattuto.
        assertThat(ScoringRule.pointsFor(player(false, 0))).isEqualTo(2);
    }

    @Test
    void laRegolaEDichiarataInFormaLeggibile() {
        assertThat(ScoringRule.describe()).contains("sessione completata", "sopravvissuto", "abbattuto");
    }
}
