from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_min_max_avg_sum(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - min(cst_size) = -3
        - max(cst_size) = 6
        - avg(cst_size) between 1.12 and 1.13
        - sum(cst_size) between 7.8 and 7.9
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
