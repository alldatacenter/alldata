import pytest
from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_count_filtered(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    # Row count is 10
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count = 3:
            filter: cat = 'HIGH'
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_missing_filtered(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    # Row count is 10
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - missing_count(pct) = 1:
            missing values: [No value, N/A, error]
            filter: cat = 'HIGH'
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


@pytest.mark.skipif(
    test_data_source == "sqlserver",
    reason="Full regex support is not supported by SQLServer. 'Percentage' format is supported but with limited functionality.",
)
def test_valid_filtered(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    # Row count is 10
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - valid_count(pct) = 2:
                valid format: percentage
                missing values: [No value, N/A, error]
                filter: cat = 'HIGH'
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


@pytest.mark.skipif(
    test_data_source == "sqlserver",
    reason="Full regex support is not supported by SQLServer. 'Percentage' format is supported but with limited functionality.",
)
def test_valid_percentage_filtered(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    # Row count is 10
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - invalid_percent(pct) = 40:
            valid format: percentage
            missing values: [N/A]
            filter: cat IS NULL
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
