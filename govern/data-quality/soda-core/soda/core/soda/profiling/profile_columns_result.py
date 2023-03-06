from __future__ import annotations

from numbers import Number
from typing import Any

from soda.sodacl.data_source_check_cfg import ProfileColumnsCfg


class ProfileColumnsResultColumn:
    def __init__(self, column_name: str, column_data_type: str):
        self.column_name: str = column_name
        self.column_data_type: str = column_data_type
        self.mins: list[float | int] | None = None
        self.maxs: list[float | int] | None = None
        self.min: float | int | None = None
        self.max: float | int | None = None
        self.frequent_values: list[dict] | None = None
        self.average: float | None = None
        self.sum: float | int | None = None
        self.standard_deviation: float | None = None
        self.variance: float | None = None
        self.distinct_values: int | None = None
        self.missing_values: int | None = None
        self.histogram: dict[str, list[str | int | float]] | None = None
        self.average_length: float | None = None
        self.min_length: float | None = None
        self.max_length: float | None = None

    def set_min_max_metrics(self, value_frequencies: list[tuple]) -> None:
        self.mins = [self.unify_type(row[2]) for row in value_frequencies if row[0] == "mins"]
        self.maxs = [self.unify_type(row[2]) for row in value_frequencies if row[0] == "maxs"]
        self.min = self.mins[0]
        self.max = self.maxs[0]

    def set_frequency_metric(self, value_frequencies: list[tuple]) -> None:
        self.frequent_values = [
            {"value": str(row[2]), "frequency": int(row[3])} for row in value_frequencies if row[0] == "frequent_values"
        ]

    def set_numeric_aggregation_metrics(self, aggregated_metrics: list[tuple]) -> None:
        self.average = self.cast_float_dtype_handle_none(aggregated_metrics[0][0])
        self.sum = self.cast_float_dtype_handle_none(aggregated_metrics[0][1])
        self.variance = self.cast_float_dtype_handle_none(aggregated_metrics[0][2])
        self.standard_deviation = self.cast_float_dtype_handle_none(aggregated_metrics[0][3])
        self.distinct_values = self.cast_int_dtype_handle_none(aggregated_metrics[0][4])
        self.missing_values = self.cast_int_dtype_handle_none(aggregated_metrics[0][5])

    def set_histogram(self, histogram_values: dict[str, list]) -> None:
        self.histogram = histogram_values

    def set_text_aggregation_metrics(self, aggregated_metrics: list[tuple]) -> None:
        self.distinct_values = self.cast_int_dtype_handle_none(aggregated_metrics[0][0])
        self.missing_values = self.cast_int_dtype_handle_none(aggregated_metrics[0][1])
        # TODO: after the discussion, we should change the type of the average_length to float
        # CLOUD-2764
        self.average_length = self.cast_int_dtype_handle_none(aggregated_metrics[0][2])
        self.min_length = self.cast_int_dtype_handle_none(aggregated_metrics[0][3])
        self.max_length = self.cast_int_dtype_handle_none(aggregated_metrics[0][4])

    @staticmethod
    def unify_type(v: Any) -> Any:
        if isinstance(v, Number):
            return float(v)
        else:
            return v

    @staticmethod
    def cast_float_dtype_handle_none(value: float | None) -> float | None:
        if value is None:
            return None
        # TODO: after the discussion, we should round float values upto n decimal places
        # CLOUD-2765
        cast_value = float(value)
        return cast_value

    @staticmethod
    def cast_int_dtype_handle_none(value: int | None) -> int | None:
        if value is None:
            return None
        cast_value = int(value)
        return cast_value

    def get_cloud_dict(self) -> dict:
        cloud_dict = {
            "columnName": self.column_name,
            "profile": {
                "mins": self.mins,
                "maxs": self.maxs,
                "min": self.min,
                "max": self.max,
                "frequent_values": self.frequent_values,
                "avg": self.average,
                "sum": self.sum,
                "stddev": self.standard_deviation,
                "variance": self.variance,
                "distinct": self.distinct_values,
                "missing_count": self.missing_values,
                "histogram": self.histogram,
                "avg_length": self.average_length,
                "min_length": self.min_length,
                "max_length": self.max_length,
            },
        }
        return cloud_dict

    def get_dict(self) -> dict:
        return {
            "columnName": self.column_name,
            "profile": {
                "mins": self.mins,
                "maxs": self.maxs,
                "min": self.min,
                "max": self.max,
                "frequent_values": self.frequent_values,
                "avg": self.average,
                "sum": self.sum,
                "stddev": self.standard_deviation,
                "variance": self.variance,
                "distinct": self.distinct_values,
                "missing_count": self.missing_values,
                "histogram": self.histogram,
                "avg_length": self.average_length,
                "min_length": self.min_length,
                "max_length": self.max_length,
            },
        }


class ProfileColumnsResultTable:
    def __init__(self, table_name: str, data_source: str, row_count: int | None = None):
        self.table_name: str = table_name
        self.data_source: str = data_source
        self.row_count: int | None = row_count
        self.result_columns: list[ProfileColumnsResultColumn] = []

    def append_column(self, column: ProfileColumnsResultColumn) -> None:
        self.result_columns.append(column)

    def get_cloud_dict(self) -> dict:
        cloud_dict = {
            "table": self.table_name,
            "dataSource": self.data_source,
            "rowCount": self.row_count,
            "columnProfiles": [result_column.get_cloud_dict() for result_column in self.result_columns],
        }
        return cloud_dict

    def get_dict(self) -> dict:
        return {
            "table": self.table_name,
            "dataSource": self.data_source,
            "rowCount": self.row_count,
            "columnProfiles": [result_column.get_dict() for result_column in self.result_columns],
        }


class ProfileColumnsResult:
    def __init__(self, profile_columns_cfg: ProfileColumnsCfg):
        self.profile_columns_cfg: ProfileColumnsCfg = profile_columns_cfg
        self.tables: list[ProfileColumnsResultTable] = []

    def append_table(self, table: ProfileColumnsResultTable) -> None:
        self.tables.append(table)
