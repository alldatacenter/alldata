from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from soda.execution.check.schema_check import SchemaCheck


def test_forbidden_columns_pass(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when forbidden column present: [{default_casify_column_name('non_existing_column_one')}, {default_casify_column_name('"%non_existing%"')}]
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_forbidden_columns_fail(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when forbidden column present: [{default_casify_column_name('id')}, {default_casify_column_name('non_existing_column_one')}, {default_casify_column_name('"%non_existing%"')}]
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
    check: SchemaCheck = scan._checks[0]
    assert sorted(check.schema_present_column_names) == sorted([default_casify_column_name("id")])


def test_forbidden_columns_fail_matching_wildcard(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when forbidden column present:
               - {default_casify_column_name('cst_size*')}
               - {default_casify_column_name('non_existing_column_two')}
    """
    )
    scan.execute()

    scan.assert_all_checks_fail()
    check: SchemaCheck = scan._checks[0]
    assert sorted(check.schema_present_column_names) == sorted(
        [default_casify_column_name("cst_size"), default_casify_column_name("cst_size_txt")]
    )


def test_forbidden_columns_warn(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    default_casify_column_name = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            warn:
              when forbidden column present: [{default_casify_column_name('id')}, {default_casify_column_name('non_existing_column_one')}, {default_casify_column_name('"%non_existing%"')}]
    """
    )
    scan.execute()

    scan.assert_all_checks_warn()
    check: SchemaCheck = scan._checks[0]
    assert sorted(check.schema_present_column_names) == sorted([default_casify_column_name("id")])
