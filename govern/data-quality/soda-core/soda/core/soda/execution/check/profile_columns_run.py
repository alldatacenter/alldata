from __future__ import annotations

from collections import defaultdict
from typing import TYPE_CHECKING

from soda.profiling.numeric_column_profiler import NumericColumnProfiler
from soda.profiling.profile_columns_result import (
    ProfileColumnsResult,
    ProfileColumnsResultColumn,
    ProfileColumnsResultTable,
)
from soda.profiling.text_column_profiler import TextColumnProfiler

if TYPE_CHECKING:
    from soda.execution.data_source_scan import DataSourceScan
    from soda.sodacl.data_source_check_cfg import ProfileColumnsCfg


class ProfileColumnsRun:
    def __init__(self, data_source_scan: DataSourceScan, profile_columns_cfg: ProfileColumnsCfg):
        self.data_source_scan = data_source_scan
        self.soda_cloud = data_source_scan.scan._configuration.soda_cloud
        self.data_source = data_source_scan.data_source
        self.profile_columns_cfg: ProfileColumnsCfg = profile_columns_cfg
        self.logs = self.data_source_scan.scan._logs

    def run(self) -> ProfileColumnsResult:
        self.logs.info(f"Running column profiling for data source: {self.data_source.data_source_name}")

        profile_result_column_tables = self.get_table_columns_metadata()

        profile_columns_result: ProfileColumnsResult = ProfileColumnsResult(self.profile_columns_cfg)

        if profile_result_column_tables is None:
            self._raise_no_tables_to_profile_warning()
            return profile_columns_result

        self.logs.info("Profiling columns for the following tables:")
        for table_name, columns_metadata in profile_result_column_tables.items():
            row_count = self.data_source.get_table_row_count(table_name)
            result_table = ProfileColumnsResultTable(
                table_name=table_name, data_source=self.data_source.data_source_name, row_count=row_count
            )
            self.logs.info(f"  - {table_name}")
            for column_name, column_data_type in columns_metadata.items():
                profiling_column_type = "unknown type"
                try:
                    if column_data_type.startswith(tuple(self.data_source.NUMERIC_TYPES_FOR_PROFILING)):
                        profiling_column_type = "numeric"
                        numeric_column_profiler = NumericColumnProfiler(
                            data_source_scan=self.data_source_scan,
                            profile_columns_cfg=self.profile_columns_cfg,
                            table_name=table_name,
                            column_name=column_name,
                            column_data_type=column_data_type,
                        )
                        result_column: ProfileColumnsResultColumn = numeric_column_profiler.profile()
                        result_table.append_column(result_column)
                    elif column_data_type.startswith(tuple(self.data_source.TEXT_TYPES_FOR_PROFILING)):
                        profiling_column_type = "text"
                        text_column_profiler = TextColumnProfiler(
                            data_source_scan=self.data_source_scan,
                            profile_columns_cfg=self.profile_columns_cfg,
                            table_name=table_name,
                            column_name=column_name,
                            column_data_type=column_data_type,
                        )
                        result_column: ProfileColumnsResultColumn = text_column_profiler.profile()
                        result_table.append_column(result_column)
                    else:
                        self.logs.warning(
                            f"Column '{table_name}.{column_name}' was not profiled because column data "
                            f"type '{column_data_type}' is not in supported profiling data types"
                        )
                except Exception as e:
                    self.logs.error(
                        f"Problem profiling {profiling_column_type} column '{table_name}.{column_name}' with data type '{column_data_type}': {e}"
                    )
            profile_columns_result.append_table(result_table)
        return profile_columns_result

    def get_table_columns_metadata(self) -> defaultdict[str, dict] | None:
        include_patterns = self.parse_profiling_expressions(self.profile_columns_cfg.include_columns)
        exclude_patterns = self.parse_profiling_expressions(self.profile_columns_cfg.exclude_columns)

        tables_columns_metadata = self.data_source.get_tables_columns_metadata(
            include_patterns=include_patterns,
            exclude_patterns=exclude_patterns,
            query_name="profile-columns-get-table-and-column-metadata",
        )
        if not tables_columns_metadata:
            return None
        return tables_columns_metadata

    @staticmethod
    def parse_profiling_expressions(profiling_expressions: list[str]) -> list[dict[str, str]]:
        parsed_profiling_expressions = []
        for profiling_expression in profiling_expressions:
            table_name_pattern, column_name_pattern = profiling_expression.split(".")
            parsed_profiling_expressions.append(
                {
                    "table_name_pattern": table_name_pattern,
                    "column_name_pattern": column_name_pattern,
                }
            )
        return parsed_profiling_expressions

    def _raise_no_tables_to_profile_warning(self) -> None:
        self.logs.warning(
            "Your SodaCL profiling expressions did not return any existing dataset name"
            f" and column name combinations for your '{self.data_source.data_source_name}' "
            "data source. Please make sure that the patterns in your profiling expressions define "
            "existing dataset name and column name combinations."
            " Profiling results may be incomplete or entirely skipped. See the docs for more information: \n"
            f"https://go.soda.io/display-profile",
            location=self.profile_columns_cfg.location,
        )
