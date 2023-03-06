from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import derive_schema_metric_value_from_test_table
from soda.execution.check_outcome import CheckOutcome
from soda.execution.data_type import DataType


def test_schema_changes_pass(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    data_source = data_source_fixture.data_source

    schema_metric_value_derived_from_test_table = derive_schema_metric_value_from_test_table(
        customers_test_table, data_source
    )

    scan = data_source_fixture.create_test_scan()

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-schema",
        metric_values=[schema_metric_value_derived_from_test_table],
    )

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
          - schema:
              fail:
                when schema changes: any
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_schema_check_have_no_data(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
          - schema:
              fail:
                when schema changes: any
        """
    )
    scan.execute(allow_error_warning=True)
    scan_cloud_result = mock_soda_cloud.pop_scan_result()
    assert scan_cloud_result["checks"][0]["outcomeReasons"] == [
        {
            "code": "notEnoughHistory",
            "message": "Skipping schema checks since there is no historic schema metrics!",
            "severity": "warn",
        }
    ]


def test_schema_changes_column_addition(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    data_source = data_source_fixture.data_source

    # start from the historic measurement value
    schema_metric_value_derived_from_test_table = derive_schema_metric_value_from_test_table(
        customers_test_table, data_source
    )
    # remove the 4th column from the historic metric value
    # this will result in schema check discovering a column being added
    schema_metric_value_derived_from_test_table.pop(3)

    scan = data_source_fixture.create_test_scan()

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{data_source_fixture.data_source.data_source_name}-{table_name}-schema",
        metric_values=[schema_metric_value_derived_from_test_table],
    )

    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when schema changes:
                - column add
        - schema:
            fail:
              when schema changes:
                - column delete
        - schema:
            fail:
              when schema changes:
                - column type change
        - schema:
            fail:
              when schema changes:
                - column index change
        - schema:
            fail:
              when schema changes: any
        - schema:
            warn:
              when schema changes:
                - column add
        - schema:
            warn:
              when schema changes:
                - column delete
        - schema:
            warn:
              when schema changes:
                - column type change
        - schema:
            warn:
              when schema changes:
                - column index change
        - schema:
            warn:
              when schema changes: any
    """
    )
    scan.execute()

    assert scan._checks[0].outcome == CheckOutcome.FAIL
    assert scan._checks[1].outcome == CheckOutcome.PASS
    assert scan._checks[2].outcome == CheckOutcome.PASS
    assert scan._checks[3].outcome == CheckOutcome.FAIL
    assert scan._checks[4].outcome == CheckOutcome.FAIL
    assert scan._checks[5].outcome == CheckOutcome.WARN
    assert scan._checks[6].outcome == CheckOutcome.PASS
    assert scan._checks[7].outcome == CheckOutcome.PASS
    assert scan._checks[8].outcome == CheckOutcome.WARN
    assert scan._checks[9].outcome == CheckOutcome.WARN


def test_schema_changes_column_deletion(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    data_source = data_source_fixture.data_source

    schema_metric_value_derived_from_test_table = derive_schema_metric_value_from_test_table(
        customers_test_table, data_source
    )

    # remove the 3rd column from the historic metric value
    schema_metric_value_derived_from_test_table.insert(
        3,
        {
            "columnName": "extra",
            "sourceDataType": data_source.get_sql_type_for_schema_check(DataType.TEXT),
        },
    )

    scan = data_source_fixture.create_test_scan()

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{data_source_fixture.data_source.data_source_name}-{table_name}-schema",
        metric_values=[schema_metric_value_derived_from_test_table],
    )

    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            fail:
              when schema changes:
                - column add
        - schema:
            fail:
              when schema changes:
                - column delete
        - schema:
            fail:
              when schema changes:
                - column type change
        - schema:
            fail:
              when schema changes:
                - column index change
        - schema:
            fail:
              when schema changes: any
        - schema:
            warn:
              when schema changes:
                - column add
        - schema:
            warn:
              when schema changes:
                - column delete
        - schema:
            warn:
              when schema changes:
                - column type change
        - schema:
            warn:
              when schema changes:
                - column index change
        - schema:
            warn:
              when schema changes: any
    """
    )
    scan.execute()

    assert scan._checks[0].outcome == CheckOutcome.PASS
    assert scan._checks[1].outcome == CheckOutcome.FAIL
    assert scan._checks[2].outcome == CheckOutcome.PASS
    assert scan._checks[3].outcome == CheckOutcome.FAIL
    assert scan._checks[4].outcome == CheckOutcome.FAIL
    assert scan._checks[5].outcome == CheckOutcome.PASS
    assert scan._checks[6].outcome == CheckOutcome.WARN
    assert scan._checks[7].outcome == CheckOutcome.PASS
    assert scan._checks[8].outcome == CheckOutcome.WARN
    assert scan._checks[9].outcome == CheckOutcome.WARN


def test_schema_changes_warn_and_fail(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    data_source = data_source_fixture.data_source

    # start from the historic measurement value
    schema_metric_value_derived_from_test_table = derive_schema_metric_value_from_test_table(
        customers_test_table, data_source
    )
    # remove the 4th column from the historic metric value
    # this will result in schema check discovering a column being added
    schema_metric_value_derived_from_test_table.pop(3)

    scan = data_source_fixture.create_test_scan()

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{data_source_fixture.data_source.data_source_name}-{table_name}-schema",
        metric_values=[schema_metric_value_derived_from_test_table],
    )

    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - schema:
            warn:
              when schema changes:
                - column add
            fail:
              when wrong column type:
                id: integer
                does_not_exist: integer
                pct: varchar
    """
    )
    scan.execute()

    assert scan._checks[0].outcome == CheckOutcome.FAIL
