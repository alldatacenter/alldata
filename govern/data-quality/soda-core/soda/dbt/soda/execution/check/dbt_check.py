from __future__ import annotations

from soda.execution.check.check import Check
from soda.execution.metric.metric import Metric
from soda.sodacl.dbt_check_cfg import DbtCheckCfg


class DbtCheck(Check):
    def __init__(self, check_cfg: DbtCheckCfg, identity: str, expression: str | None):
        self.identity = identity
        self.check_cfg = check_cfg
        self.expression = expression
        self.cloud_check_type = "metricThreshold"

    def get_cloud_diagnostics_dict(self) -> dict:
        return {"value": 0}

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]):
        # Not much to evaluate since this is done by dbt
        pass

    def get_cloud_dict(self):
        # TODO some values are hard-coded like location and metrics since dbt tests don't map properly to our model
        cloud_dict = {
            "identity": self.identity,
            "identities": {
                # All versions are the same here, but we have all of them for compatibility with standard check.
                "v1": self.identity,
                "v2": self.identity,
                "v3": self.identity,
            },
            "name": self.name,
            "type": self.cloud_check_type,
            "definition": self.check_cfg.name,
            "location": {
                "filePath": self.check_cfg.file_path,
                "line": 0,
                "col": 0,
            },
            "dataSource": self.data_source_scan.data_source.data_source_name,
            "table": self.check_cfg.table_name,
            "column": self.check_cfg.column_name,
            "metrics": ["dbt_metric"],
            "outcome": self.outcome.value if self.outcome else None,
            "diagnostics": self.get_cloud_diagnostics_dict(),
            "source": "dbt",
        }

        return cloud_dict

    def get_dict(self):
        dict = {
            "identity": self.identity,
            "name": self.name,
            "type": self.cloud_check_type,
            "definition": self.check_cfg.name,
            "location": {
                "filePath": self.check_cfg.file_path,
                "line": 0,
                "col": 0,
            },
            "dataSource": self.data_source_scan.data_source.data_source_name,
            "table": self.check_cfg.table_name,
            "column": self.check_cfg.column_name,
            "metrics": ["dbt_metric"],
            "outcome": self.outcome.value if self.outcome else None,
        }

        return dict
