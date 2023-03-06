from __future__ import annotations


class Table:
    def __init__(self, data_source_scan: DataSourceScan, table_name: str):
        from soda.execution.column import Column
        from soda.execution.partition import Partition

        self.data_source_scan = data_source_scan
        self.table_name = str(table_name)
        self.qualified_table_name = self.data_source_scan.data_source.qualified_table_name(table_name)
        self.columns_by_name: dict[str, Column] = {}
        self.partitions: dict[str, Partition] = {}

    def get_or_create_column(self, column_name: str):
        column = self.columns_by_name.get(column_name)
        if not column:
            from soda.execution.column import Column

            column = Column(self, column_name)
            self.columns_by_name[column_name] = column
        return column

    def get_or_create_partition(self, partition_name: str | None):
        partition = self.partitions.get(partition_name)
        if partition is None:
            from soda.execution.partition import Partition

            partition = Partition(self, partition_name)
            self.partitions[partition_name] = partition
        return partition
