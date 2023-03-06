from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import execute_scan_and_get_scan_result


def test_vars_in_check(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan_result = execute_scan_and_get_scan_result(
        data_source_fixture,
        f"""
          checks for ${{TABLE_NAME}}:
            - row_count > 0:
                name: testing name ${{NAME}}
                filter: ${{COLUMN}} = '${{VALUE}}'
                identity: ${{IDENTITY}}
        """,
        variables={
            "TABLE_NAME": table_name,
            "NAME": "something",
            "COLUMN": "country",
            "VALUE": "BE",
            "IDENTITY": "test-check",
        },
    )

    assert scan_result["checks"][0]["name"] == "testing name something"


def test_vars_in_configuration(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    scan.add_variables(
        {
            "DS_TYPE": "postgres",
            "DS_HOST": "host: localhost",
        }
    )
    scan.add_configuration_yaml_str(
        f"""
        data_source another_source:
            type: ${{DS_TYPE}}
            ${{DS_HOST}}
            username: soda
            password: secret
            database: soda
            schema: public
        """
    )

    data_sources = scan._data_source_manager.data_source_properties_by_name
    assert data_sources["another_source"]["type"] == "postgres"
    assert data_sources["another_source"]["host"] == "localhost"


def test_vars_in_foreach_name(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan_result = execute_scan_and_get_scan_result(
        data_source_fixture,
        f"""
          for each dataset D:
            datasets:
                - include {table_name}
            checks:
                - row_count > 1:
                    name: Row count in ${{D}} must be positive
        """,
    )
    assert scan_result["checks"][0]["name"].lower() == f"Row count in {table_name} must be positive".lower()
