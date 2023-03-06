import pytest
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import format_query_one_line, test_data_source


@pytest.mark.parametrize(
    "include_tables, exclude_tables, expected_sql",
    [
        pytest.param(None, None, None, id="no filter"),
        pytest.param(
            ["table1", "table2"],
            None,
            "(lower(table_column) like 'table1' OR lower(table_column) like 'table2')",
            id="just include",
        ),
        pytest.param(
            None,
            ["table1", "table2"],
            "lower(table_column) not like 'table1' AND lower(table_column) not like 'table2'",
            id="just exclude",
        ),
        pytest.param(
            ["table1"],
            ["table2"],
            "(lower(table_column) like 'table1') AND (lower(table_column) not like 'table2'",
            id="both",
        ),
    ],
)
@pytest.mark.skip("Skipped until expected vs actual output can be reliably compared.")
def test_sql_table_include_exclude_filter(
    data_source_fixture: DataSourceFixture, include_tables, exclude_tables, expected_sql
):
    scan = data_source_fixture.create_test_scan()
    data_source = scan._data_source_manager.get_data_source(test_data_source)

    generated_sql = data_source.sql_table_include_exclude_filter("table_column", None, include_tables, exclude_tables)
    assert format_query_one_line(generated_sql) == format_query_one_line(expected_sql)
