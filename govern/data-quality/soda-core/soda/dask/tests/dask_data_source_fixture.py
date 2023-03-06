from __future__ import annotations

import logging

import dask.dataframe as dd
import pandas as pd
from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable
from soda.scan import Scan

logger = logging.getLogger(__name__)


class DaskDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {"data_source dask": {"type": "dask"}, "schema": schema_name}

    def _test_session_starts(self) -> None:
        scan = Scan()
        self.context = scan._get_or_create_dask_context(required_soda_module="soda-core-pandas-dask")
        self.data_source = scan._data_source_manager.get_data_source(self.data_source_name)
        scan._get_or_create_data_source_scan(self.data_source_name)

    def _test_session_ends(self):
        self.data_source.connection.close()
        self._drop_schema_if_exists()

    def _create_and_insert_test_table(self, test_table: TestTable) -> None:
        df_test = pd.DataFrame(
            data=test_table.values,
            columns=[test_column.name for test_column in test_table.test_columns],
        )

        # TODO: numeric columns doesn't work in dask-sql. As a temporary fix, we rename them to "na"
        # in unit tests.
        df_test.columns = ["na" if col.isnumeric() else col for col in df_test.columns]
        dtype_conversions = self.data_source.PANDAS_TYPE_FOR_CREATE_TABLE_MAP
        convert_dict = {
            test_column.name: dtype_conversions[test_column.data_type] for test_column in test_table.test_columns
        }
        df_test = df_test.astype(convert_dict)
        dd_test = dd.from_pandas(
            df_test,
            npartitions=1,
        )
        self.context.create_table(table_name=test_table.unique_table_name, input_table=dd_test)

    def _create_schema_if_not_exists_sql(self) -> str:
        self.context.create_schema(schema_name=self.schema_name)

    def _use_schema_sql(self) -> None:
        a = 5

    def _drop_schema_if_exists(self) -> None:
        try:
            self.context.drop_schema(schema_name=self.schema_name)
        except KeyError:
            pass
