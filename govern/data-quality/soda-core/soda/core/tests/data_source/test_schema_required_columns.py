from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import format_checks
from soda.execution.check.schema_check import SchemaCheck


def test_required_columns_pass(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name
    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when required column missing: [{default_casify_column_name('id')}, {default_casify_column_name('cst_size_txt')}, {default_casify_column_name('distance')}]
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_required_columns_fail(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    checks_str = format_checks(
        ["id", "cst_size_txt", "non_existing_column", "name"],
        indent=15,
        prefix="-",
        data_source=data_source_fixture.data_source,
    )
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when required column missing:
{checks_str}
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
    check: SchemaCheck = scan._checks[0]
    assert sorted(check.fail_result.missing_column_names) == sorted(
        [default_casify_column_name("non_existing_column"), default_casify_column_name("name")]
    )


def test_required_columns_warn(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    checks_str = format_checks(
        ["id", "cst_size_txt", "non_existing_column", "name"],
        indent=15,
        prefix="-",
        data_source=data_source_fixture.data_source,
    )
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            warn:
              when required column missing:
{checks_str}
    """
    )
    scan.execute()

    scan.assert_all_checks_warn()
    check: SchemaCheck = scan._checks[0]
    assert sorted(check.warn_result.missing_column_names) == sorted(
        [default_casify_column_name("non_existing_column"), default_casify_column_name("name")]
    )
