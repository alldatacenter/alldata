from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_change_over_time(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - change avg last 7 for row_count = 1
            - change for row_count between -10 and +50
            - change percent for row_count < 10%
            - change avg last 7 for row_count < 50
            - change min last 7 for row_count < 50
            - change min last 5 percent for row_count between -10% and +25%
            - change min last 7 for duplicate_count(cst_size):
                warn: when not between -10 and +20
                fail: when not between -50 and +100
            - change min last 7 percent for duplicate_count(cst_size) < 50 %
        """
    )

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-row_count",
        metric_values=[10, 10, 10, 9, 8, 8, 8, 0, 0, 0],
    )
    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-cst_size-duplicate_count",
        metric_values=[0, 0, 0, 10, 10, 10, 10, 5, 0, 10],
    )

    scan.execute()

    scan.assert_all_checks_pass()


def test_change_over_time_fail(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - change percent for row_count < 10%
        """
    )

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-row_count",
        metric_values=[8],
    )
    scan.execute()

    scan.assert_all_checks_fail()


def test_change_over_time_no_historical_data(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - change percent for row_count < 10%
        """
    )

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-row_count",
        metric_values=[],
    )

    scan.execute(allow_warnings_only=True)

    scan.assert_log_warning("No historic measurements for metric metric-test_change_over_time.py")
    scan.assert_all_checks_skipped()


def test_change_over_time_last_x(data_source_fixture: DataSourceFixture):
    """This does not actually test the cloud part of the last week/month, just the syntax."""
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - change same day last week for row_count = 0
        """
    )

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-row_count",
        metric_values=[10],
    )
    scan.execute()

    scan.assert_all_checks_pass()
