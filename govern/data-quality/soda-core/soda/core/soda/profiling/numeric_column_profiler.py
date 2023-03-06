from __future__ import annotations

from typing import TYPE_CHECKING

from soda.execution.query.query import Query
from soda.profiling.profile_columns_result import ProfileColumnsResultColumn

if TYPE_CHECKING:
    from soda.execution.data_source_scan import DataSourceScan
    from soda.sodacl.data_source_check_cfg import ProfileColumnsCfg


class NumericColumnProfiler:
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

        # mins, maxs, min, max, frequent values
        self._set_result_column_value_frequency_attributes()

        # Average, sum, variance, standard deviation, distinct values, missing values
        self._set_result_column_aggregation_attributes()

        # histogram
        self._set_result_column_histogram_attributes()
        return self.result_column

    def _set_result_column_value_frequency_attributes(self) -> None:
        value_frequencies = self._compute_value_frequency()
        if value_frequencies:
            self.result_column.set_min_max_metrics(value_frequencies=value_frequencies)
            self.result_column.set_frequency_metric(value_frequencies=value_frequencies)
        else:
            self.logs.error(
                "Database returned no results for minumum values, maximum values and "
                f"frequent values in table: {self.table_name}, columns: {self.column_name}"
            )

    def _set_result_column_aggregation_attributes(self) -> None:
        aggregated_metrics = self._compute_aggregated_metrics()
        if aggregated_metrics:
            self.result_column.set_numeric_aggregation_metrics(aggregated_metrics=aggregated_metrics)
        else:
            self.logs.error(
                f"Database returned no results for aggregates in table: {self.table_name}, columns: {self.column_name}"
            )

    def _set_result_column_histogram_attributes(self) -> None:
        histogram_values = self._compute_histogram()
        if histogram_values:
            self.result_column.set_histogram(histogram_values=histogram_values)
        else:
            self.logs.error(
                f"Database returned no results for histograms in table: {self.table_name}, columns: {self.column_name}"
            )

    def _compute_value_frequency(self) -> list[tuple] | None:
        value_frequencies_sql = self.data_source.profiling_sql_values_frequencies_query(
            "numeric",
            self.table_name,
            self.column_name,
            self.profile_columns_cfg.limit_mins_maxs,
            self.profile_columns_cfg.limit_frequent_values,
        )

        value_frequencies_query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"profiling-{self.table_name}-{self.column_name}-value-frequencies-numeric",
            sql=value_frequencies_sql,
        )
        value_frequencies_query.execute()
        rows = value_frequencies_query.rows
        return rows

    def _compute_aggregated_metrics(self) -> list[tuple] | None:
        aggregates_sql = self.data_source.profiling_sql_aggregates_numeric(self.table_name, self.column_name)
        aggregates_query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"profiling-{self.table_name}-{self.column_name}-profiling-aggregates",
            sql=aggregates_sql,
        )
        aggregates_query.execute()
        rows = aggregates_query.rows
        return rows

    def _compute_histogram(self) -> None | dict[str, list]:
        if self.result_column.min is None:
            self.logs.warning("Min cannot be None, make sure the min metric is derived before histograms")
        if self.result_column.max is None:
            self.logs.warning("Max cannot be None, make sure the min metric is derived before histograms")
        if self.result_column.distinct_values is None:
            self.logs.warning(
                "Distinct values cannot be None, make sure the distinct values metric is derived before histograms"
            )
        if (
            self.result_column.min is None
            or self.result_column.max is None
            or self.result_column.distinct_values is None
        ):
            self.logs.warning(
                f"Histogram query for {self.table_name}, column {self.column_name} skipped. See earlier warnings."
            )
            return None

        histogram_sql, bins_list = self.data_source.histogram_sql_and_boundaries(
            table_name=self.table_name,
            column_name=self.column_name,
            min_value=self.result_column.min,
            max_value=self.result_column.max,
            n_distinct=self.result_column.distinct_values,
            column_type=self.column_data_type,
        )
        if histogram_sql is None:
            return None

        histogram_query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"profiling-{self.table_name}-{self.column_name}-histogram",
            sql=histogram_sql,
        )
        histogram_query.execute()
        histogram_values = histogram_query.rows

        if histogram_values is None:
            return None
        histogram = {}
        histogram["boundaries"] = bins_list
        histogram["frequencies"] = [int(freq) if freq is not None else 0 for freq in histogram_values[0]]
        return histogram
