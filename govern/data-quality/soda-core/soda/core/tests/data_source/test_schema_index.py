from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import format_checks
from soda.execution.check.schema_check import SchemaCheck


def test_required_columns_indexes_pass(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    checks_str = format_checks(
        [("id", "0"), ("cst_size_txt", "2"), ("distance", "3")],
        indent=15,
        data_source=data_source_fixture.data_source,
    )
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when wrong column index:
{checks_str}
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_required_columns_indexes_fail(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name
    checks_str = format_checks(
        [("id", "6"), ("cst_size_txt", "3"), ("distance", "4")],
        indent=15,
        data_source=data_source_fixture.data_source,
    )

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when wrong column index:
{checks_str}
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
    check: SchemaCheck = scan._checks[0]
    assert check.fail_result.column_index_mismatches == {
        default_casify_column_name("distance"): {
            "actual_index": 3,
            "column_on_expected_index": default_casify_column_name("pct"),
            "expected_index": 4,
        },
        default_casify_column_name("id"): {
            "actual_index": 0,
            "column_on_expected_index": default_casify_column_name("country"),
            "expected_index": 6,
        },
        default_casify_column_name("cst_size_txt"): {
            "actual_index": 2,
            "column_on_expected_index": default_casify_column_name("distance"),
            "expected_index": 3,
        },
    }


def test_required_columns_indexes_warn(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name
    checks_str = format_checks(
        [("id", "6"), ("cst_size_txt", "3"), ("distance", "4")],
        indent=15,
        data_source=data_source_fixture.data_source,
    )

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            warn:
              when wrong column index:
{checks_str}
    """
    )
    scan.execute()

    scan.assert_all_checks_warn()
    check: SchemaCheck = scan._checks[0]
    assert check.warn_result.column_index_mismatches == {
        default_casify_column_name("distance"): {
            "actual_index": 3,
            "column_on_expected_index": default_casify_column_name("pct"),
            "expected_index": 4,
        },
        default_casify_column_name("id"): {
            "actual_index": 0,
            "column_on_expected_index": default_casify_column_name("country"),
            "expected_index": 6,
        },
        default_casify_column_name("cst_size_txt"): {
            "actual_index": 2,
            "column_on_expected_index": default_casify_column_name("distance"),
            "expected_index": 3,
        },
    }
