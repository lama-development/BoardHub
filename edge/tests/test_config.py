import os
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from boardhub_edge.config import EdgeConfig


class EdgeConfigTest(unittest.TestCase):

    def test_qos_predefiniti_separano_eventi_e_stato(self):
        with patch.dict(os.environ, {}, clear=True):
            config = EdgeConfig.from_environment()

        self.assertEqual(config.qos, 1)
        self.assertEqual(config.status_qos, 0)

    def test_qos_possono_essere_configurati_indipendentemente(self):
        environment = {
            "BOARDHUB_EDGE_QOS": "2",
            "BOARDHUB_EDGE_STATUS_QOS": "1",
        }
        with patch.dict(os.environ, environment, clear=True):
            config = EdgeConfig.from_environment()

        self.assertEqual(config.qos, 2)
        self.assertEqual(config.status_qos, 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
