"""Test dei componenti della coda locale del nodo edge.

Coprono gli invarianti: scrittura locale prima dell'invio, sequenze non
ripetute, chiusura solo con conferma applicativa, acknowledgement ripetuto
senza effetti.
"""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from boardhub_edge.outbox import ACKNOWLEDGED, CONFLICT, PENDING, PUBLISHED, REJECTED, Outbox


class OutboxTest(unittest.TestCase):

    def setUp(self):
        self._directory = tempfile.TemporaryDirectory()
        self.path = os.path.join(self._directory.name, "outbox.sqlite3")
        self.outbox = Outbox(self.path)

    def tearDown(self):
        self.outbox.close()
        self._directory.cleanup()

    def _enqueue(self, session="session-test-001", event_type="MOVE"):
        return self.outbox.enqueue(
            session_id=session,
            event_type=event_type,
            payload={"from": "A1", "to": "B1"},
            venue_id="venue-01",
            table_id="table-04",
            source="EDGE",
            topic="boardhub/v1/venues/venue-01/tables/table-04/events",
        )

    def test_evento_salvato_localmente_prima_di_ogni_invio(self):
        event = self._enqueue()

        self.assertEqual(self.outbox.counters()[PENDING], 1)
        self.assertTrue(event["eventId"].startswith("evt-"))
        self.assertEqual(event["sequenceNumber"], 1)

    def test_sequenze_monotone_per_sessione_e_sorgente(self):
        primo = self._enqueue()
        secondo = self._enqueue()
        altra_sessione = self._enqueue(session="session-test-002")

        self.assertEqual(primo["sequenceNumber"], 1)
        self.assertEqual(secondo["sequenceNumber"], 2)
        # Una sessione diversa riparte da uno: i contatori sono indipendenti.
        self.assertEqual(altra_sessione["sequenceNumber"], 1)

    def test_il_puback_non_chiude_l_elemento(self):
        event = self._enqueue()

        self.outbox.mark_published(event["eventId"])

        counters = self.outbox.counters()
        self.assertEqual(counters[PUBLISHED], 1)
        self.assertEqual(counters[ACKNOWLEDGED], 0)

    def test_l_elemento_si_chiude_con_la_conferma_applicativa(self):
        event = self._enqueue()
        self.outbox.mark_published(event["eventId"])

        chiuso = self.outbox.mark_acknowledged(event["eventId"], "PERSISTED")

        self.assertTrue(chiuso)
        self.assertEqual(self.outbox.counters()[ACKNOWLEDGED], 1)

    def test_duplicate_e_un_esito_di_successo(self):
        event = self._enqueue()

        self.outbox.mark_acknowledged(event["eventId"], "DUPLICATE")

        # Il fatto risulta registrato una sola volta lato backend: la coda si chiude.
        self.assertEqual(self.outbox.counters()[ACKNOWLEDGED], 1)

    def test_acknowledgement_ripetuto_e_innocuo(self):
        event = self._enqueue()
        self.outbox.mark_acknowledged(event["eventId"], "PERSISTED")

        secondo = self.outbox.mark_acknowledged(event["eventId"], "PERSISTED")

        self.assertFalse(secondo)
        self.assertEqual(self.outbox.counters()[ACKNOWLEDGED], 1)

    def test_esiti_di_rifiuto_e_conflitto(self):
        rifiutato = self._enqueue()
        conflitto = self._enqueue()

        self.outbox.mark_acknowledged(rifiutato["eventId"], "REJECTED", "payload non valido")
        self.outbox.mark_acknowledged(conflitto["eventId"], "CONFLICT", "sessione conclusa")

        counters = self.outbox.counters()
        self.assertEqual(counters[REJECTED], 1)
        self.assertEqual(counters[CONFLICT], 1)

    def test_esito_sconosciuto_non_chiude_l_elemento(self):
        event = self._enqueue()

        self.assertFalse(self.outbox.mark_acknowledged(event["eventId"], "QUALCOSA_ALTRO"))
        self.assertEqual(self.outbox.counters()[PENDING], 1)

    def test_il_riavvio_non_perde_gli_elementi_aperti(self):
        self._enqueue()
        pubblicato = self._enqueue()
        self.outbox.mark_published(pubblicato["eventId"])
        self.outbox.close()

        riaperta = Outbox(self.path)
        try:
            counters = riaperta.counters()
            self.assertEqual(counters[PENDING], 1)
            self.assertEqual(counters[PUBLISHED], 1)
            self.assertEqual(len(riaperta.open_items()), 2)
        finally:
            riaperta.close()
            self.outbox = Outbox(self.path)

    def test_pubblicato_senza_conferma_torna_in_coda(self):
        event = self._enqueue()
        self.outbox.mark_published(event["eventId"])

        riaperti = self.outbox.reopen_stale_published("9999-12-31T23:59:59Z")

        self.assertEqual(riaperti, 1)
        self.assertEqual(self.outbox.counters()[PENDING], 1)

    def test_backoff_persistito_rinvia_il_tentativo(self):
        event = self._enqueue()

        self.outbox.schedule_retry(event["eventId"], 0, 1.0, 30.0, 1000.0, "broker assente")

        self.assertEqual(len(self.outbox.claim_due(1000.0, 10)), 0)
        self.assertEqual(len(self.outbox.claim_due(1002.0, 10)), 1)

    def test_gli_elementi_sono_consegnati_in_ordine_di_sequenza(self):
        eventi = [self._enqueue() for _ in range(3)]

        sequenze = []
        for evento in eventi:
            dovuti = self.outbox.claim_due(9_999_999_999.0, 10)
            self.assertEqual(len(dovuti), 1)
            sequenze.append(dovuti[0]["sequence_number"])
            self.outbox.mark_acknowledged(evento["eventId"], "PERSISTED")

        self.assertEqual(sequenze, [1, 2, 3])

    def test_una_sequenza_in_backoff_blocca_le_successive(self):
        primo = self._enqueue()
        self._enqueue()
        self.outbox.schedule_retry(
            primo["eventId"], 0, 10.0, 30.0, 1000.0, "broker assente"
        )

        self.assertEqual(self.outbox.claim_due(1001.0, 10), [])
        dovuti = self.outbox.claim_due(1011.0, 10)
        self.assertEqual([row["sequence_number"] for row in dovuti], [1])
        self.outbox.mark_acknowledged(primo["eventId"], "PERSISTED")
        self.assertEqual(
            [row["sequence_number"] for row in self.outbox.claim_due(1011.0, 10)],
            [2],
        )

    def test_un_puback_senza_ack_applicativo_blocca_le_successive(self):
        primo = self._enqueue()
        self._enqueue()
        self.outbox.mark_published(primo["eventId"])

        self.assertEqual(self.outbox.claim_due(9_999_999_999.0, 10), [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
