from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from soda.common.logs import Logs
from soda.sampler.sample import Sample


@dataclass
class SampleContext:
    sample: Sample
    sample_name: str
    query: str
    data_source: DataSource
    partition: Partition | None
    column: Column | None
    scan: Scan
    logs: Logs
    samples_limit: int | None
    passing_sql: str | None
    check_name: str | None

    def get_scan_folder_name(self):
        parts = [
            self.scan._scan_definition_name,
            self.scan._data_timestamp.strftime("%Y%m%d%H%M%S"),
            datetime.now(tz=timezone.utc).strftime("%Y%m%d%H%M%S"),
        ]
        return "_".join([part for part in parts if part])

    def get_sample_file_name(self):
        parts = [
            self.partition.table.table_name if self.partition else None,
            self.partition.partition_name if self.partition else None,
            self.column.column_name if self.column else None,
            self.sample_name,
        ]
        return "_".join([part for part in parts if part])
