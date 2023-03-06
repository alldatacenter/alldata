from soda.sodacl.missing_and_valid_cfg import MissingAndValidCfg


class ColumnConfigurationsCfg(MissingAndValidCfg):
    """
    Default missing and validity configurations for a column.
    """

    def __init__(self, column_name: str):
        super().__init__()
        self.column_name: str = column_name
