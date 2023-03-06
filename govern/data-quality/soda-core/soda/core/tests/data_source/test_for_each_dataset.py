import pytest
from helpers.common_test_tables import (
    customers_dist_check_test_table,
    customers_test_table,
    orders_test_table,
    raw_customers_test_table,
)
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_for_each_dataset(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)
    rawcustomers_table_name = data_source_fixture.ensure_test_table(raw_customers_test_table)
    _ = data_source_fixture.ensure_test_table(orders_test_table)  # to test that it is not included
    _ = data_source_fixture.ensure_test_table(customers_dist_check_test_table)  # to test that it is not included

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          for each dataset D:
            datasets:
              - {customers_table_name}
              - include %rder  # it won't match orders table since there is no % in the end
              - include {rawcustomers_table_name}%
              - include {data_source_fixture.data_source.data_source_name}.{rawcustomers_table_name}%
              - exclude non_existing_dataset
            checks:
              - row_count > 0
              - missing_count(id) = 1
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()
    assert len(scan._checks) == 4


@pytest.mark.skipif(
    test_data_source != "postgres",
    reason="Run for postgres only as nothing data source specific is tested.",
)
def test_for_each_dataset_schema(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)
    casify = data_source_fixture.data_source.default_casify_column_name

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          for each dataset D:
            datasets:
              - {customers_table_name}
            checks:
              - schema:
                  warn:
                    when required column missing: [{casify('id')}]
                  fail:
                    when forbidden column present:
                      - ssn
                      - credit_card%
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


@pytest.mark.skipif(
    test_data_source != "postgres",
    reason="Run for postgres only as nothing data source specific is tested.",
)
def test_for_each_table_backwards_compatibility(data_source_fixture: DataSourceFixture):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          for each table T:
            tables:
              - {customers_table_name}
            checks:
              - row_count >= 0
        """
    )
    scan.execute_unchecked()

    scan.assert_no_error_logs()
    assert scan.has_error_or_warning_logs()


def test_for_each_dataset_with_quotes_warning(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        """
        for each dataset T:
            datasets:
                - include "some_table"
                - exclude "some_other_table"
        """
    )
    scan.execute(allow_error_warning=True)
    scan_results = mock_soda_cloud.pop_scan_result()
    character_log_warnings = [
        x for x in scan_results["logs"] if "It looks like quote characters are present" in x["message"]
    ]
    assert len(character_log_warnings) == 2
