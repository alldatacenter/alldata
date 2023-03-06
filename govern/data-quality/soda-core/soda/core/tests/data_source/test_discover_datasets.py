from __future__ import annotations

from helpers.common_test_tables import (
    customers_dist_check_test_table,
    customers_profiling,
    customers_test_table,
    orders_test_table,
)
from helpers.data_source_fixture import DataSourceFixture
from soda.execution.data_type import DataType


def test_discover_tables(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_profiling)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
          discover datasets:
            datasets:
                - include {table_name}
                - include hello
        """
    )
    scan.execute(allow_warnings_only=True)
    # remove the data source name because it's a pain to test
    discover_tables_result = mock_soda_cloud.pop_scan_result()

    assert discover_tables_result is not None
    actual_metadata = discover_tables_result["metadata"]
    actual_metadatum = actual_metadata[0]
    actual_schema = actual_metadatum["schema"]

    data_source = data_source_fixture.data_source
    to_ds_type = data_source.get_sql_type_for_schema_check
    to_ds_case = data_source.default_casify_column_name

    expected_schema = [
        {"columnName": to_ds_case("id"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("cst_size"), "sourceDataType": to_ds_type(DataType.DECIMAL)},
        {"columnName": to_ds_case("cst_size_txt"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("distance"), "sourceDataType": to_ds_type(DataType.INTEGER)},
        {"columnName": to_ds_case("pct"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("cat"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("country"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("zip"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("email"), "sourceDataType": to_ds_type(DataType.TEXT)},
        {"columnName": to_ds_case("date"), "sourceDataType": to_ds_type(DataType.DATE)},
        {"columnName": to_ds_case("ts"), "sourceDataType": to_ds_type(DataType.TIMESTAMP)},
        {"columnName": to_ds_case("ts_with_tz"), "sourceDataType": to_ds_type(DataType.TIMESTAMP_TZ)},
    ]

    all([a == b for a, b in zip(actual_schema, expected_schema)])
    # assert actual_schema == expected_schema


def test_discover_tables_customer_wildcard(data_source_fixture: DataSourceFixture):
    data_source_fixture.ensure_test_table(orders_test_table)
    profiling_table = data_source_fixture.ensure_test_table(customers_profiling)
    dist_check_test_table = data_source_fixture.ensure_test_table(customers_dist_check_test_table)

    test_table = data_source_fixture.ensure_test_table(customers_test_table)
    test_table = data_source_fixture.data_source.default_casify_table_name(test_table)
    wildcard = f"%{test_table.split('_')[1]}%"

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        discover datasets:
          datasets:
            - include {wildcard}
        """
    )
    scan.execute(allow_warnings_only=True)
    discover_tables_result = mock_soda_cloud.pop_scan_result()
    assert discover_tables_result is not None
    # we only expect tables with customer in the relation name

    expected_datasets = {x.lower() for x in [profiling_table, dist_check_test_table, test_table]}
    actual_datasets = {t.get("table").lower() for t in discover_tables_result["metadata"]}
    assert expected_datasets.issubset(actual_datasets)


def test_discover_table_with_quotes_error(data_source_fixture: DataSourceFixture):
    orders_table = data_source_fixture.ensure_test_table(orders_test_table)
    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        discover datasets:
            datasets:
                - include "{orders_table}"
                - exclude "{orders_table}"
        """
    )
    scan.execute(allow_error_warning=True)
    scan_results = mock_soda_cloud.pop_scan_result()
    character_log_warnings = [
        x
        for x in scan_results["logs"]
        if "It looks like quote characters are present" in x["message"] and x["level"] == "error"
    ]

    assert len(character_log_warnings) == 2


def test_discover_datasets_no_provided_table(data_source_fixture: DataSourceFixture):
    _ = data_source_fixture.ensure_test_table(orders_test_table)
    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        discover datasets:
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
