import pytest
from helpers.common_test_tables import customers_test_table, raw_customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


@pytest.mark.parametrize(
    "include_tables, exclude_tables, expected_count",
    [
        ### Fuzzy matching the expected counts so that test does not come brittle with changing how
        ### many tables are used across the test suite and based on order of test case execution.
        ### More detailed test of the include/exclude behavior in a dedicated unit test.
        pytest.param(None, None, None, id="no filter"),
        pytest.param(["%raw%"], None, 1, id="just include"),
        pytest.param(None, ["%test%"], 0, id="just exclude"),
        pytest.param(
            ["%orders%"], ["%types%"], None, id="both"
        ),  # Does not test much, just makes sure when both are included it does return something.
    ],
)
@pytest.mark.skip
def test_get_row_counts_all_tables(
    data_source_fixture: DataSourceFixture, include_tables, exclude_tables, expected_count
):
    data_source_fixture.ensure_test_table(customers_test_table)
    data_source_fixture.ensure_test_table(raw_customers_test_table)
    scan = data_source_fixture.create_test_scan()
    data_source = scan._data_source_manager.get_data_source(test_data_source)

    result = data_source.get_row_counts_all_tables(include_tables=include_tables, exclude_tables=exclude_tables)
    if expected_count is not None:
        assert len(result) == expected_count
    else:
        assert len(result) > 0

    if len(result) > 0:
        # Just test that some counts are returned and not all zeroes.
        all_empty = True
        for count in result.values():
            if count > 0:
                all_empty = False
        assert not all_empty
