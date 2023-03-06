from helpers.common_test_tables import customers_test_table, orders_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_reference_check_fail(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)
    orders_table_name = data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {orders_table_name}:
        - values in customer_id_nok must exist in {customers_table_name} id
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()


def test_reference_check_pass(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)
    orders_table_name = data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {orders_table_name}:
        - values in (customer_id_ok) must exist in {customers_table_name} (id)
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_multi_column_reference_check(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)
    orders_table_name = data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {orders_table_name}:
        - values in (customer_country, customer_zip) must exist in {customers_table_name} (country, zip)
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
