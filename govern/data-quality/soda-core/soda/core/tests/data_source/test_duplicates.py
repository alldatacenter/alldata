from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_duplicates_single_column(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - duplicate_count(cat) = 1
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    # This is a simple use case, verify that * is used in the main query.
    scan.assert_log("count(*)")


def test_duplicates_multiple_columns(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - duplicate_count(cat, country) = 1
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_duplicates_with_exclude_columns(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan._configuration.exclude_columns = {table_name: ["country"]}
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - duplicate_count(cat) = 1
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    # Exclude columns present, query should list the columns explicitly
    scan.assert_log("cat, frequency")
    scan.assert_no_log(" * ")


def test_duplicates_with_filter(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - duplicate_count(cat) = 0:
            filter: country = 'NL'
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
    scan.assert_log("AND country = 'NL'")
