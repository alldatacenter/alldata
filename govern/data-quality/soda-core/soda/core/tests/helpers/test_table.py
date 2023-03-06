from __future__ import annotations

import hashlib

from helpers.test_column import TestColumn
from soda.common.json_helper import JsonHelper


class TestTable:
    """
    Test infrastructure that includes all data for a test table: name, schema and row values.
    This is part of the main codebase because we want data_source implementation to start from a single
    class where methods must be implemented and can be customized.
    """

    __test__ = False
    __names = []

    def __init__(
        self,
        name: str,
        columns: list[tuple[str, str]] | list[TestColumn],
        values: list[tuple] = None,
        quote_names: bool = False,
        create_view: bool = False,
    ):
        """
        name: logical name
        columns: tuples with column name and DataType's
        values: list of row value tuples
        create_as_view: Creates a table with a _TABLE_  and a corresponding view with the name
        """
        # Ensure unique table data names
        if name in TestTable.__names:
            raise AssertionError(f"Duplicate TableData name: {name}")
        TestTable.__names.append(name)

        self.name: str = name
        if len(columns) == 0 or isinstance(columns[0], TestColumn):
            self.test_columns: list[TestColumn] = columns
        else:
            self.test_columns: list[TestColumn] = [TestColumn(column[0], column[1]) for column in columns]

        self.values: list[tuple] = values
        self.quote_names: bool = quote_names
        self.create_view = create_view

        if self.create_view:
            self.unique_table_name = f"SODATEST_{name}_TABLE_{self.__test_table_hash()}"
            self.unique_view_name = f"SODATEST_{name}_{self.__test_table_hash()}"
        else:
            self.unique_table_name = f"SODATEST_{name}_{self.__test_table_hash()}"

    def __test_table_hash(self):
        json_text = JsonHelper.to_json(
            [
                self.name,
                [test_column.to_hashable_json() for test_column in self.test_columns],
                list(self.values) if self.values else None,
            ]
        )
        hexdigest = hashlib.md5(json_text.encode()).hexdigest()
        return hexdigest[-8:]

    def find_test_column_by_name(self, column_name: str) -> TestColumn:
        return next(test_column for test_column in self.test_columns if test_column.name == column_name)
