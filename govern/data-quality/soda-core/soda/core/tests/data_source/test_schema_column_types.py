from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import format_checks
from soda.execution.check_outcome import CheckOutcome
from soda.execution.data_type import DataType


def test_columns_types_pass(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    def column_type_format(column_name: str) -> str:
        test_column = customers_test_table.find_test_column_by_name(column_name)
        casified_column_name = data_source_fixture.data_source.default_casify_column_name(column_name)
        casified_data_type = data_source_fixture.data_source.default_casify_type_name(
            data_source_fixture.data_source.get_sql_type_for_schema_check(test_column.data_type)
        )
        return f"{casified_column_name}: {casified_data_type}"

    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when wrong column type:
                {column_type_format('id')}
                {column_type_format('cst_size')}
                {column_type_format('cst_size_txt')}
                {column_type_format('distance')}
                {column_type_format('date_updated')}
                {column_type_format('ts')}
                {column_type_format('ts_with_tz')}
    """
    )
    # This Also verifies type aliasing - check using "varchar", actual is "character varying"
    scan.execute()

    scan.assert_all_checks_pass()


def test_columns_types_fail(data_source_fixture: DataSourceFixture):
    checks_str = format_checks(
        [("id", "integer"), ("does_not_exist", "integer"), ("pct", "varchar")],
        indent=15,
        data_source=data_source_fixture.data_source,
    )
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when wrong column type:
{checks_str}
    """
    )
    scan.execute()

    check = scan._checks[0]

    assert check.outcome == CheckOutcome.FAIL

    data_source = data_source_fixture.data_source
    default_casify_column_name = data_source.default_casify_column_name

    assert check.schema_missing_column_names == [default_casify_column_name("does_not_exist")]
    assert check.schema_column_type_mismatches == {
        default_casify_column_name("id"): {
            "expected_type": data_source.default_casify_type_name("integer"),
            "actual_type": data_source.get_sql_type_for_schema_check(DataType.TEXT),
        }
    }


def test_columns_types_warn(data_source_fixture: DataSourceFixture):
    checks_str = format_checks(
        [("id", "integer"), ("does_not_exist", "integer"), ("pct", "varchar")],
        indent=15,
        data_source=data_source_fixture.data_source,
    )
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            warn:
              when wrong column type:
{checks_str}
    """
    )
    scan.execute()

    check = scan._checks[0]

    assert check.outcome == CheckOutcome.WARN

    data_source = data_source_fixture.data_source
    default_casify_column_name = data_source.default_casify_column_name

    assert check.schema_missing_column_names == [default_casify_column_name("does_not_exist")]
    assert check.schema_column_type_mismatches == {
        default_casify_column_name("id"): {
            "expected_type": data_source.default_casify_type_name("integer"),
            "actual_type": data_source.get_sql_type_for_schema_check(DataType.TEXT),
        }
    }
