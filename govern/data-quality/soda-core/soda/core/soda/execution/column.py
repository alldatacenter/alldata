from soda.execution.table import Table


class Column:
    def __init__(self, table: "Table", column_name: str):
        from soda.sodacl.column_configurations_cfg import ColumnConfigurationsCfg

        self.data_source_scan = table.data_source_scan
        self.table = table
        self.column_name = str(column_name)
        self.column_configurations_cfg: ColumnConfigurationsCfg = None

    def set_column_configuration_cfg(self, column_configurations_cfg: "ColumnConfigurationsCfg"):
        self.column_configurations_cfg = column_configurations_cfg

    @classmethod
    def get_partition_name(cls, column):
        return column.column_name if isinstance(column, Column) else None
