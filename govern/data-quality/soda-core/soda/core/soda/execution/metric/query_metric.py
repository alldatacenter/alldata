import logging
from abc import ABC
from typing import Optional

from soda.execution.metric.metric import Metric

logger = logging.getLogger(__name__)


class QueryMetric(Metric, ABC):
    def __init__(
        self,
        data_source_scan: "DataSourceScan",
        partition: Optional["Partition"],
        column: Optional["Column"],
        name: str,
        check: "Check",
        identity_parts: list = None,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=column,
            name=name,
            check=check,
            identity_parts=identity_parts,
        )

    def create_failed_rows_sample_query(self):
        return None
