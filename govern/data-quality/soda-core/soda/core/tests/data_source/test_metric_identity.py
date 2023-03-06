from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_table_metric_identities(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:

            # Next checks should be based on the same, single metric
            - row_count > 0
            - row_count > 0:
                name: This custom name should not affect metric identity
            - row_count > 1 # Another threshold should also not affect metric identity

            # Next check should create a second, distinct metric
            - row_count >= 2:
                filter: cat is not null
        """
    )
    scan.execute()

    assert len(scan._metrics) == 2


def test_column_metric_identities(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:

            # Next checks should be based on the same, single metric
            - missing_count(cst_size) > 0
            - missing_count(cst_size) > 0:
                name: This custom name should not affect metric identity
            - missing_count(cst_size) > 1 # Another threshold should also not affect metric identity

            # Next check should create a second, distinct metric
            - missing_count(cst_size) > 2:
                filter: cat is not null
        """
    )
    scan.execute()

    assert len(scan._metrics) == 2
