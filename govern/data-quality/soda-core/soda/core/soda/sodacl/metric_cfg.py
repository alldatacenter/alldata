from typing import Optional


class MetricCfg:
    def __init__(self):
        self.metric_name: str = None
        self.table_name: Optional[str] = None
        self.column_name: Optional[str] = None
        self.title: str = None
