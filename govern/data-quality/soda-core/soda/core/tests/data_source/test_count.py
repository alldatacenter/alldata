from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_row_count_thresholds_with_variables(data_source_fixture: DataSourceFixture):
    """
    Tests all passing thresholds on a simple row count
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"nine": "9"})
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count > ${{nine}}
    """
    )
    # - row_count between (${{nine}} and 15]
    scan.execute()

    scan.assert_all_checks_pass()


def test_row_count_thresholds_passing(data_source_fixture: DataSourceFixture):
    """
    Tests all passing thresholds on a simple row count
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count = 10.0
        - row_count < 11
        - row_count > 9
        - row_count <= 10
        - row_count >= 10
        - row_count != 0
        - row_count <> 0
        - row_count between 10 and 15]
        - row_count between [-5 and 10
        - row_count between (9 and 15]
        - row_count between -5 and 11)
        - row_count not between 11 and 15
        - row_count not between [-5 and 9]
        - row_count not between (10 and 15
        - row_count not between -5 and 10)
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_row_count_thresholds_failing(data_source_fixture: DataSourceFixture):
    """
    Tests all failing thresholds on a simple row count
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    # Row count is 10
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count < 10
        - row_count > 10
        - row_count <= 9
        - row_count >= 11
        - row_count between 11 and 15
        - row_count between 5 and 9]
        - row_count between (10 and 15
        - row_count between 5 and 10)
        - row_count not between [10 and 15
        - row_count not between [5 and 10]
        - row_count not between (9 and 15]
        - row_count not between [5 and 11)
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
