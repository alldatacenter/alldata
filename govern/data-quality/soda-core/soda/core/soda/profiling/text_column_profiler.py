from __future__ import annotations

from typing import TYPE_CHECKING

from soda.execution.query.query import Query
from soda.profiling.profile_columns_result import ProfileColumnsResultColumn

if TYPE_CHECKING:
    from soda.execution.data_source_scan import DataSourceScan
    from soda.sodacl.data_source_check_cfg import ProfileColumnsCfg


class TextColumnProfiler:
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        profile_columns_cfg: ProfileColumnsCfg,
        table_name: str,
        column_name: str,
        column_data_type: str,
    ) -> None:
        self.data_source_scan = data_source_scan
        self.data_source = data_source_scan.data_source
        self.logs = data_source_scan.scan._logs
        self.profile_columns_cfg = profile_columns_cfg
        self.table_name = table_name
        self.column_name = column_name
        self.column_data_type = column_data_type
        self.result_column = ProfileColumnsResultColumn(column_name=column_name, column_data_type=column_data_type)

    def profile(self) -> ProfileColumnsResultColumn:
        self.logs.debug(f"Profiling column {self.column_name} of {self.table_name}")

        # frequent values for text column
        self._set_result_column_value_frequency_attribute()

        # pure text aggregates
        self._set_result_column_text_aggregation_attributes()

        return self.result_column

    def _set_result_column_value_frequency_attribute(self) -> None:
        value_frequencies = self._compute_value_frequency()
        if value_frequencies:
            self.result_column.set_frequency_metric(value_frequencies)
        else:
            self.logs.warning(
                f"Database returned no results for textual frequent values in {self.table_name}, column: {self.column_name}"
            )

    def _set_result_column_text_aggregation_attributes(self) -> None:
        text_aggregates = self._compute_text_aggregates()
        if text_aggregates:
            self.result_column.set_text_aggregation_metrics(text_aggregates)
        else:
            self.logs.warning(
                f"Database returned no results for textual aggregates in {self.table_name}, column: {self.column_name}"
            )

    def _compute_value_frequency(self) -> list[tuple] | None:
        # frequent values for text column
        value_frequencies_sql = self.data_source.profiling_sql_values_frequencies_query(
            "text",
            self.table_name,
            self.column_name,
            self.profile_columns_cfg.limit_mins_maxs,
            self.profile_columns_cfg.limit_frequent_values,
        )
        value_frequencies_query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"profiling-{self.table_name}-{self.column_name}-value-frequencies-text",
            sql=value_frequencies_sql,
        )
        value_frequencies_query.execute()
        frequency_rows = value_frequencies_query.rows
        return frequency_rows

    def _compute_text_aggregates(self) -> list[tuple] | None:
        text_aggregates_sql = self.data_source.profiling_sql_aggregates_text(self.table_name, self.column_name)
        text_aggregates_query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"profiling: {self.table_name}, {self.column_name}: get textual aggregates",
            sql=text_aggregates_sql,
        )
        text_aggregates_query.execute()
        text_aggregates_rows = text_aggregates_query.rows
        return text_aggregates_rows
