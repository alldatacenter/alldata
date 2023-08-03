from typing import Dict

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.metric.numeric_query_metric import NumericQueryMetric
from soda.execution.partition import Partition
from soda.execution.query.user_defined_failed_rows_expression_query import (
    UserDefinedFailedRowsExpressionQuery,
)

KEY_FAILED_ROWS_COUNT = "failed_rows_count"


class UserDefinedFailedRowsExpressionCheck(Check):
    def __init__(
        self,
        check_cfg: "UserDefinedFailedRowsExpressionCheckCfg",
        data_source_scan: "DataSourceScan",
        partition: Partition,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
        )
        self.check_value = None
        self.metrics[KEY_FAILED_ROWS_COUNT] = self.data_source_scan.resolve_metric(
            NumericQueryMetric(
                data_source_scan=self.data_source_scan,
                partition=partition,
                column=None,
                metric_name="row_count",
                metric_args=None,
                filter=check_cfg.fail_condition_sql_expr,
                aggregation=None,
                check_missing_and_valid_cfg=None,
                column_configurations_cfg=None,
                check=self,
            )
        )

    def evaluate(self, metrics: Dict[str, Metric], historic_values: Dict[str, object]):
        self.check_value: int = metrics.get(KEY_FAILED_ROWS_COUNT).value

        self.outcome = CheckOutcome.PASS
        if self.check_value > 0:
            self.outcome = CheckOutcome.FAIL
            failed_rows_sql = self.get_failed_rows_sql()
            failed_rows_query = UserDefinedFailedRowsExpressionQuery(
                data_source_scan=self.data_source_scan,
                check_name=self.check_cfg.source_line,
                sql=failed_rows_sql,
                samples_limit=self.check_cfg.samples_limit,
                partition=self.partition,
                metric=self.metrics[KEY_FAILED_ROWS_COUNT],
            )
            failed_rows_query.execute()
            if failed_rows_query.sample_ref and failed_rows_query.sample_ref.is_persisted():
                self.failed_rows_sample_ref = failed_rows_query.sample_ref

    def get_failed_rows_sql(self) -> str:
        columns = ", ".join(
            self.data_source_scan.data_source.sql_select_all_column_names(self.partition.table.table_name)
        )
        sql = (
            f"SELECT {columns} \n"
            f"FROM {self.partition.table.qualified_table_name} \n"
            f"WHERE ({self.check_cfg.fail_condition_sql_expr})"
        )
        partition_filter = self.partition.sql_partition_filter
        if partition_filter:
            scan = self.data_source_scan.scan
            condition = scan.jinja_resolve(definition=partition_filter, location=self.check_cfg.location)
            sql += f"\n      AND ({condition})"
        return sql

    def get_log_diagnostic_dict(self) -> dict:
        log_diagnostics = {"check_value": self.check_value}
        if self.failed_rows_sample_ref:
            log_diagnostics["failed_rows_sample_ref"] = str(self.failed_rows_sample_ref)
        return log_diagnostics
