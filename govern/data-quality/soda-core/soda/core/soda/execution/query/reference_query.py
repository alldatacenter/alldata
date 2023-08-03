from __future__ import annotations

from soda.execution.query.query import Query
from soda.execution.query.sample_query import SampleQuery


class ReferenceQuery(Query):
    @staticmethod
    def build_source_column_list(metric):
        source_column_names = metric.check.check_cfg.source_column_names
        return ",".join(source_column_names)

    def __init__(
        self,
        data_source_scan: DataSourceScan,
        metric: ReferentialIntegrityMetric,
        partition: Partition,
        samples_limit: int | None = None,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            unqualified_query_name=f"reference[{ReferenceQuery.build_source_column_list(metric)}]",
            samples_limit=samples_limit,
            partition=partition,
        )

        self.metric = metric

        from soda.execution.data_source import DataSource
        from soda.sodacl.reference_check_cfg import ReferenceCheckCfg

        data_source: DataSource = data_source_scan.data_source

        check_cfg: ReferenceCheckCfg = metric.check.check_cfg
        source_table_name = data_source.qualified_table_name(metric.partition.table.table_name)
        source_column_names = check_cfg.source_column_names
        target_table_name = data_source.qualified_table_name(check_cfg.target_table_name)
        target_column_names = check_cfg.target_column_names

        selectable_source_columns = self.data_source_scan.data_source.sql_select_all_column_names(
            self.partition.table.table_name
        )
        source_diagnostic_column_fields = ", ".join([f"SOURCE.{c}" for c in selectable_source_columns])

        # TODO add global config of table diagnostic columns and apply that here
        # source_diagnostic_column_names = check_cfg.source_diagnostic_column_names
        # if source_diagnostic_column_names:
        #     source_diagnostic_column_names += source_column_names
        #     source_diagnostic_column_fields = ', '.join([f'SOURCE.{column_name}' for column_name in source_diagnostic_column_names])

        join_condition = " AND ".join(
            [
                f"SOURCE.{source_column_name} = TARGET.{target_column_names[index]}"
                for index, source_column_name in enumerate(source_column_names)
            ]
        )

        # Search for all rows where:
        # 1. source value is not null - to avoid null values triggering fails
        # 2. target value is null - this means that source value was not found in target column.
        # Passing query is same on source side, but not null on target side.
        where_condition = " OR ".join(
            [
                f"(SOURCE.{source_column_name} IS NOT NULL AND TARGET.{target_column_name} IS NULL)"
                for source_column_name, target_column_name in zip(source_column_names, target_column_names)
            ]
        )
        passing_where_condition = " AND ".join(
            [
                f"(SOURCE.{source_column_name} IS NOT NULL AND TARGET.{target_column_name} IS NOT NULL)"
                for source_column_name, target_column_name in zip(source_column_names, target_column_names)
            ]
        )

        partition_filter = self.partition.sql_partition_filter
        if partition_filter:
            scan = self.data_source_scan.scan
            resolved_partition_filter = scan.jinja_resolve(definition=partition_filter)
            where_condition = f"{resolved_partition_filter} AND ({where_condition})"
            passing_where_condition = f"{resolved_partition_filter} AND ({passing_where_condition})"

        jinja_resolve = self.data_source_scan.scan.jinja_resolve

        self.sql = jinja_resolve(
            data_source.sql_reference_query(
                "count(*)", source_table_name, target_table_name, join_condition, where_condition
            )
        )

        self.failed_rows_sql = jinja_resolve(
            data_source.sql_reference_query(
                source_diagnostic_column_fields,
                source_table_name,
                target_table_name,
                join_condition,
                where_condition,
                self.samples_limit,
            )
        )

        self.failing_sql = jinja_resolve(
            data_source.sql_reference_query(
                source_diagnostic_column_fields, source_table_name, target_table_name, join_condition, where_condition
            )
        )

        self.passing_sql = jinja_resolve(
            data_source.sql_reference_query(
                source_diagnostic_column_fields,
                source_table_name,
                target_table_name,
                join_condition,
                passing_where_condition,
            )
        )

    def execute(self):
        self.fetchone()
        missing_reference_count = int(self.row[0])
        self.metric.set_value(missing_reference_count)

        if missing_reference_count and self.samples_limit > 0:
            # TODO: Sample Query execute implicitly stores the failed rows file reference in the passed on metric.
            sample_query = SampleQuery(
                self.data_source_scan,
                self.metric,
                "failed_rows",
                self.failed_rows_sql,
            )
            sample_query.execute()
