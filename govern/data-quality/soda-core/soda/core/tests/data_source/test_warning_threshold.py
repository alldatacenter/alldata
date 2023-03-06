from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_warning_threshold(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count:
            warn: when not between (19 and 25]
            fail: when not between (5 and 30]
        - row_count:
            warn: when not between (5 and 30]
            fail: when not between (19 and 25]
        - row_count:
            warn: when < 15
            fail: when < 5
        - row_count:
            warn: when = 10
            fail: when = 11
        - row_count:
            warn: when between 1 and 100
            fail: when between 5 and 15
    """
    )
    scan.execute()

    scan.assert_check_warn()
    scan.assert_check_fail()
    scan.assert_check_warn()
    scan.assert_check_warn()
    scan.assert_check_fail()
