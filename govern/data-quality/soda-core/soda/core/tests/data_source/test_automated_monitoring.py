import os

import pytest
from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.utils import derive_schema_metric_value_from_test_table


@pytest.mark.skipif(
    condition=os.getenv("SCIENTIFIC_TESTS") == "SKIP",
    reason="Environment variable SCIENTIFIC_TESTS is set to SKIP which skips tests depending on the scientific package",
)
def test_automated_monitoring(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    schema_metric_value_derived_from_test_table = derive_schema_metric_value_from_test_table(
        customers_test_table, data_source_fixture.data_source
    )

    scan = data_source_fixture.create_test_scan()
    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-row_count-automated_monitoring",
        metric_values=[10, 10, 10, 9, 8, 8, 8, 0, 0, 0],
    )

    scan.mock_historic_values(
        metric_identity=f"metric-{scan._scan_definition_name}-{scan._data_source_name}-{table_name}-schema-automated_monitoring",
        metric_values=[schema_metric_value_derived_from_test_table],
    )

    scan.add_sodacl_yaml_str(
        f"""
            automated monitoring:
              datasets:
                - include {table_name}
                - exclude PROD%
        """
    )
    scan.execute()


def test_automated_monitoring_no_provided_table(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        automated monitoring:
            datasets:
        """
    )
    scan.execute(allow_error_warning=True)
    scan_results = mock_soda_cloud.pop_scan_result()
    assert scan_results["hasErrors"]
    assert [
        x
        for x in scan_results["logs"]
        if 'Content of "datasets" must be a list of include and/or exclude expressions' in x["message"]
    ]
