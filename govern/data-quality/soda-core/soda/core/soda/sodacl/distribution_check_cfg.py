from __future__ import annotations

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location
from soda.sodacl.threshold_cfg import ThresholdCfg


class DistributionCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: str | None,
        location: Location,
        name: str | None,
        column_name: str,
        distribution_name: str,
        filter: str,
        sample_clause: str,
        reference_file_path: str,
        fail_threshold_cfg: ThresholdCfg | None,
        warn_threshold_cfg: ThresholdCfg | None,
        method: str,
    ):
        super().__init__(
            source_header=source_header,
            source_line=source_line,
            source_configurations=source_configurations,
            location=location,
            name=name,
        )
        self.column_name = column_name
        self.distribution_name = distribution_name
        self.filter = filter
        self.sample_clause = sample_clause
        self.reference_file_path = reference_file_path
        self.fail_threshold_cfg = fail_threshold_cfg
        self.warn_threshold_cfg = warn_threshold_cfg
        self.method = method

    def get_column_name(self) -> str | None:
        return self.column_name
