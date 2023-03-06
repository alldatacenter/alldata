from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_min_max_avg_length(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - min_length(cat) = 3
        - max_length(cat) = 6
        - avg_length(cat) = 4.2
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
