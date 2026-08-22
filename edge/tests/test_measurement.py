import sys
import unittest
from pathlib import Path
from unittest.mock import call, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from boardhub_edge.measurement import (
    MeasurementRow,
    _cleanup_table,
    build_report,
    percentile_nearest_rank,
    summarize,
)


class MeasurementStatisticsTest(unittest.TestCase):
    def test_nearest_rank_p95(self) -> None:
        self.assertEqual(percentile_nearest_rank(range(1, 21), 0.95), 19)

    def test_summary_uses_median_and_nearest_rank(self) -> None:
        self.assertEqual(
            summarize([1.0, 2.0, 3.0, 100.0]),
            {"min": 1.0, "median": 2.5, "p95": 100.0, "max": 100.0},
        )

    def test_report_excludes_warmup_sample(self) -> None:
        rows = []
        for scenario in ("online", "offline-recovery"):
            for sample in range(3):
                rows.append(
                    MeasurementRow(
                        scenario=scenario,
                        sample=sample,
                        event_id=f"{scenario}-{sample}",
                        sequence=sample,
                        occurred_at="2026-01-01T00:00:00.000Z",
                        received_at="2026-01-01T00:00:00.010Z",
                        persistence_latency_ms=999.0 if sample == 0 else float(sample),
                        payload_bytes=100,
                        outbox_attempts=1 if scenario == "offline-recovery" else None,
                        ack_latency_ms=float(sample) if scenario == "offline-recovery" else None,
                    )
                )
        report = build_report({"Commit": "abc123"}, rows, recovery_ms=1000)
        self.assertIn("Eventi misurati: **2**", report)
        self.assertNotIn("999.00", report)


class MeasurementCleanupTest(unittest.TestCase):
    @patch("boardhub_edge.measurement._http_json")
    def test_active_session_is_closed(self, http_json) -> None:
        http_json.return_value = {"status": "IN_SESSION"}

        _cleanup_table("qr-table-07")

        self.assertEqual(
            http_json.call_args_list,
            [
                call("GET", "/api/v1/public/tables/qr-table-07"),
                call(
                    "POST",
                    "/api/v1/admin/tables/qr-table-07/close-session",
                    headers={"X-BoardHub-Venue-Key": "boardhub-local-venue-admin-key"},
                ),
            ],
        )

    @patch("boardhub_edge.measurement._http_json")
    def test_claimable_table_is_disabled(self, http_json) -> None:
        http_json.return_value = {"status": "CLAIMABLE"}

        _cleanup_table("qr-table-07")

        self.assertEqual(
            http_json.call_args_list,
            [
                call("GET", "/api/v1/public/tables/qr-table-07"),
                call(
                    "POST",
                    "/api/v1/admin/tables/qr-table-07/disable",
                    headers={"X-BoardHub-Venue-Key": "boardhub-local-venue-admin-key"},
                ),
            ],
        )

    @patch("boardhub_edge.measurement._http_json")
    def test_disabled_table_requires_no_mutation(self, http_json) -> None:
        http_json.return_value = {"status": "DISABLED"}

        _cleanup_table("qr-table-07")

        http_json.assert_called_once_with(
            "GET", "/api/v1/public/tables/qr-table-07"
        )


if __name__ == "__main__":
    unittest.main()
