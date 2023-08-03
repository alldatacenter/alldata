from __future__ import annotations

from numbers import Number

from soda.cli.cli import DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT
from soda.common.exceptions import SODA_SCIENTIFIC_MISSING_LOG_MESSAGE
from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.column import Column
from soda.execution.data_source_scan import DataSourceScan
from soda.execution.metric.metric import Metric
from soda.execution.partition import Partition
from soda.execution.query.query import Query
from soda.sodacl.distribution_check_cfg import DistributionCheckCfg


class DistributionCheck(Check):
    def __init__(
        self,
        check_cfg: DistributionCheckCfg,
        data_source_scan: DataSourceScan,
        partition: Partition = None,
        column: Column = None,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=column,
        )

        if data_source_scan.data_source.type not in DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT:
            self.logs.info(
                f"The support for your data source type is experimental. Distribution checks are"
                f" not tested for '{data_source_scan.data_source.type}' and may not work."
            )

        self.distribution_check_cfg: DistributionCheckCfg = self.check_cfg
        metric = Metric(
            data_source_scan=self.data_source_scan,
            name=self.distribution_check_cfg.source_line,
            partition=partition,
            column=column,
            check=self,
            identity_parts=["distribution"],
        )
        metric.value = None
        metric = data_source_scan.resolve_metric(metric)
        self.metrics["distribution-difference-metric"] = metric
        self.check_value: float | None = None

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]) -> None:
        try:
            from soda.scientific.common.exceptions import LoggableException
            from soda.scientific.distribution.comparison import DistributionChecker
        except ModuleNotFoundError as e:
            self.logs.error(f"{SODA_SCIENTIFIC_MISSING_LOG_MESSAGE}\n Original error: {e}")
            return

        sql = self.sql_column_values_query(self.distribution_check_cfg)

        self.query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name="get_values_for_distro_check",
            sql=sql,
        )
        self.query.execute()
        if self.query.exception is None and self.query.rows is not None:
            test_data = [row[0] for row in self.query.rows]
            ref_file_path = self.distribution_check_cfg.reference_file_path
            dist_method = self.distribution_check_cfg.method
            dist_name = self.distribution_check_cfg.distribution_name
            dist_ref_yaml = self.data_source_scan.scan._read_file(
                file_type="disribution reference object yaml", file_path=ref_file_path
            )
            try:
                check_result_dict = DistributionChecker(
                    dist_method, dist_ref_yaml, ref_file_path, dist_name, test_data
                ).run()
                self.check_value = check_result_dict["check_value"]
                self.metrics["distribution-difference-metric"].value = self.check_value
                self.set_outcome_based_on_check_value()
            except LoggableException as e:
                self.logs.error(e, location=self.check_cfg.location)

    def set_outcome_based_on_check_value(self) -> None:
        if self.check_value is not None and (
            self.distribution_check_cfg.warn_threshold_cfg or self.distribution_check_cfg.fail_threshold_cfg
        ):
            if isinstance(self.check_value, Number):
                if (
                    self.distribution_check_cfg.fail_threshold_cfg
                    and self.distribution_check_cfg.fail_threshold_cfg.is_bad(self.check_value)
                ):
                    self.outcome = CheckOutcome.FAIL
                elif (
                    self.distribution_check_cfg.warn_threshold_cfg
                    and self.distribution_check_cfg.warn_threshold_cfg.is_bad(self.check_value)
                ):
                    self.outcome = CheckOutcome.WARN
                else:
                    self.outcome = CheckOutcome.PASS
            else:
                self.logs.error(
                    f"Cannot evaluate check: Expected a numeric value, but was {self.check_value}",
                    location=self.check_cfg.location,
                )

    def get_cloud_diagnostics_dict(self) -> dict:
        distribution_check_cfg: DistributionCheckCfg = self.check_cfg
        cloud_diagnostics = {"value": self.check_value}
        if distribution_check_cfg.fail_threshold_cfg is not None:
            cloud_diagnostics["fail"] = distribution_check_cfg.fail_threshold_cfg.to_soda_cloud_diagnostics_json()
        if distribution_check_cfg.warn_threshold_cfg is not None:
            cloud_diagnostics["warn"] = distribution_check_cfg.warn_threshold_cfg.to_soda_cloud_diagnostics_json()
        return cloud_diagnostics

    def get_log_diagnostic_dict(self) -> dict:
        log_diagnostics = super().get_log_diagnostic_dict()
        # if self.historic_diff_values:
        #     log_diagnostics.update(self.historic_diff_values)
        return log_diagnostics

    def sql_column_values_query(self, distribution_check_cfg: DistributionCheckCfg) -> str:
        column_name = distribution_check_cfg.column_name
        scan = self.data_source_scan.scan

        partition_filter = scan.jinja_resolve(self.partition.sql_partition_filter)
        distribution_check_filter = scan.jinja_resolve(distribution_check_cfg.filter)
        sample_clause = scan.jinja_resolve(distribution_check_cfg.sample_clause)

        filters = []
        filters.append(partition_filter)
        filters.append(distribution_check_filter)

        filter_clause = " AND ".join(_filter for _filter in filters if _filter)

        if sample_clause:
            limit = None  # No need to apply limit if we are sampling
        else:
            limit = int(1e6)

        return self.data_source_scan.data_source.sql_select_column_with_filter_and_limit(
            column_name=column_name,
            table_name=self.partition.table.qualified_table_name,
            filter_clause=filter_clause,
            sample_clause=sample_clause,
            limit=limit,
        )

    def get_summary(self) -> str:
        error_summary = (
            " NO THRESHOLD PROVIDED ->"
            if not self.check_cfg.fail_threshold_cfg and not self.check_cfg.warn_threshold_cfg
            else ""
        )
        summary_message = f"{super().get_summary()}{error_summary}"
        return summary_message
