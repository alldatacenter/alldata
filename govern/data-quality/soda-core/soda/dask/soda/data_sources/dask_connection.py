from dask_sql import Context
from soda.data_sources.dask_cursor import DaskCursor


class DaskConnection:
    def __init__(self, context: Context):
        self.context = context

    def cursor(self) -> DaskCursor:
        return DaskCursor(self.context)

    def close(self) -> None:
        ...

    def commit(self) -> None:
        ...

    def rollback(self) -> None:
        ...
