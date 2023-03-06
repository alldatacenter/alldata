import pytest
from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


@pytest.mark.skipif(
    test_data_source == "sqlserver",
    reason="Full regex support is not supported by SQLServer. 'Percentage' format is supported but with limited functionality.",
)
def test_double_metric_computation(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
            checks for {table_name}:
              - row_count > 0
              - invalid_percent(pct) < 35 %:
                  valid format: percentage
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
    assert len(scan._queries[0].select_expressions) == 2
