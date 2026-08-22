import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from boardhub_edge.config import EdgeConfig
from boardhub_edge.outbox import Outbox
from boardhub_edge.runner import EdgeNode


class EdgeNodeQosTest(unittest.TestCase):

    def setUp(self):
        self._directory = tempfile.TemporaryDirectory()
        self.outbox = Outbox(str(Path(self._directory.name) / "outbox.sqlite3"))
        self.config = EdgeConfig(edge_id="edge-test", qos=1, status_qos=0)
        self.node = EdgeNode(self.config, self.outbox)
        self.client = Mock()
        self.node._client = self.client

    def tearDown(self):
        self.outbox.close()
        self._directory.cleanup()

    def test_status_e_retained_con_qos_zero(self):
        self.node._connected.set()

        self.node.publish_status()

        topic, payload = self.client.publish.call_args.args
        self.assertEqual(topic, self.config.status_topic)
        self.assertEqual(json.loads(payload)["edgeId"], "edge-test")
        self.assertEqual(self.client.publish.call_args.kwargs, {"qos": 0, "retain": True})

    def test_ack_e_comandi_restano_sottoscritti_con_qos_affidabile(self):
        self.node._on_connect(self.client, None, None, 0)

        self.client.subscribe.assert_any_call(self.config.acks_topic, qos=1)
        self.client.subscribe.assert_any_call(self.config.commands_topic, qos=1)

    def test_eventi_restano_pubblicati_con_qos_affidabile(self):
        event = self.node.observe(
            "session-test-001",
            "MOVE",
            {"characterId": "adv-01", "from": "A1", "to": "B1"},
        )
        row = self.outbox.all_items()[0]
        publish_info = Mock(rc=0)
        self.client.publish.return_value = publish_info

        published = self.node._publish_row(row)

        self.assertTrue(published)
        self.client.publish.assert_called_once_with(
            self.config.events_topic,
            row["payload_json"],
            qos=1,
        )
        publish_info.wait_for_publish.assert_called_once_with(timeout=5)
        self.assertEqual(event["eventId"], row["event_id"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
