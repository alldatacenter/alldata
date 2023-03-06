from __future__ import annotations

from helpers.common_test_tables import customers_test_table, orders_test_table
from helpers.data_source_fixture import DataSourceFixture


def test_sample_tables(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          sample datasets:
            datasets:
              - include {table_name}
        """
    )
    scan.execute(allow_warnings_only=True)
    # remove the data source name because it's a pain to test
    discover_tables_result = mock_soda_cloud.pop_scan_result()

    assert discover_tables_result is not None
    profilings = discover_tables_result["profiling"]
    profiling = profilings[0]
    sample_file = profiling["sampleFile"]
    columns = sample_file["columns"]
    casify = data_source_fixture.data_source.default_casify_column_name
    assert [c["name"] for c in columns] == [
        casify(c)
        for c in [
            "id",
            "customer_id_nok",
            "customer_id_ok",
            "customer_country",
            "customer_zip",
            "text",
        ]
    ]
    for c in columns:
        col_type = c["type"]
        assert isinstance(col_type, str)
        assert len(col_type) > 0
    assert sample_file["totalRowCount"] == 7
    assert sample_file["storedRowCount"] == 7

    reference = sample_file["reference"]
    ref_type = reference["type"]
    assert ref_type == "sodaCloudStorage"
    file_id = reference["fileId"]
    assert isinstance(file_id, str)
    assert len(file_id) > 0


def test_discover_tables_customer_wildcard_incl_only(data_source_fixture: DataSourceFixture):
    data_source_fixture.ensure_test_table(customers_test_table)
    orders_test_table_name = data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
        sample datasets:
          datasets:
            - include %{orders_test_table_name[:-2]}%
        """
    )
    scan.execute(allow_warnings_only=True)
    discover_tables_result = mock_soda_cloud.pop_scan_result()
    assert discover_tables_result is not None
    profilings = discover_tables_result["profiling"]
    dataset_names = [profiling["table"].lower() for profiling in profilings]
    assert dataset_names == [orders_test_table_name.lower()]


def test_discover_tables_customer_wildcard_incl_excl(data_source_fixture: DataSourceFixture):
    data_source_fixture.ensure_test_table(customers_test_table)
    data_source_fixture.ensure_test_table(orders_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
        sample datasets:
          datasets:
            - include %sodatest_%
            - exclude %orders%
            - exclude %profiling%
        """
    )
    scan.execute(allow_warnings_only=True)
    discover_tables_result = mock_soda_cloud.pop_scan_result()
    profilings = discover_tables_result["profiling"]
    dataset_names = [profiling["table"] for profiling in profilings]
    for dataset_name in dataset_names:
        assert "order" not in dataset_name.lower()
        assert "profiling" not in dataset_name.lower()


def test_sample_datasets_no_provided_table(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        f"""
        sample datasets:
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
