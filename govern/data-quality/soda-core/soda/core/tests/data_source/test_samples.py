from __future__ import annotations

from textwrap import dedent

import pytest
from helpers.common_test_tables import (
    customers_dist_check_test_table,
    customers_test_table,
)
from helpers.data_source_fixture import DataSourceFixture
from soda.sampler.default_sampler import DefaultSampler
from soda.sampler.sampler import DEFAULT_FAILED_ROWS_SAMPLE_LIMIT


def test_missing_count_sample(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_count(id) = 1
        """
    )
    scan.execute()

    diagnostics = mock_soda_cloud.find_check_diagnostics(0)
    assert diagnostics["value"] == 1
    assert_missing_sample(mock_soda_cloud, 0)


def test_missing_count_sample_disabled(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    mock_soda_cloud.disable_collecting_warehouse_data = True
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_count(id) = 1
        """
    )
    scan.execute()

    diagnostics = mock_soda_cloud.find_check_diagnostics(0)
    assert diagnostics["value"] == 1
    assert len(mock_soda_cloud.files) == 0
    assert isinstance(scan._configuration.sampler, DefaultSampler)


def test_missing_percent_sample(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_percent(id) < 20 %
        """
    )
    scan.execute()

    assert_missing_sample(mock_soda_cloud, 0)


def assert_missing_sample(mock_soda_cloud, check_index):
    failed_rows_diagnostics_block = mock_soda_cloud.find_failed_rows_diagnostics_block(check_index)
    failed_rows_file = failed_rows_diagnostics_block["file"]
    columns = failed_rows_file["columns"]
    assert columns[0]["name"].lower() == "id"
    assert columns[1]["name"].lower() == "cst_size"
    assert failed_rows_file["totalRowCount"] == 1
    assert failed_rows_file["storedRowCount"] == 1
    reference = failed_rows_file["reference"]
    assert reference["type"] == "sodaCloudStorage"
    file_id = reference["fileId"]
    assert isinstance(file_id, str)
    assert len(file_id) > 0

    file_content = mock_soda_cloud.find_file_content_by_file_id(file_id)
    assert file_content.startswith("[null, ")


def test_various_valid_invalid_sample_combinations(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_count(cat) > 0
            - missing_percent(cat) > 0
            - invalid_count(cat) > 0
            - invalid_percent(cat) > 0
            - missing_count(cat) > 0:
                missing values: ['HIGH']
            - missing_percent(cat) > 0:
                missing values: ['HIGH']
            - invalid_count(cat) > 0:
                valid values: ['HIGH']
            - invalid_percent(cat) > 0:
                valid values: ['HIGH']
        """
    )
    scan.execute_unchecked()

    assert mock_soda_cloud.find_failed_rows_line_count(0) == 5
    assert mock_soda_cloud.find_failed_rows_line_count(1) == 5
    mock_soda_cloud.assert_no_failed_rows_block_present(2)
    mock_soda_cloud.assert_no_failed_rows_block_present(3)
    assert mock_soda_cloud.find_failed_rows_line_count(4) == 8
    assert mock_soda_cloud.find_failed_rows_line_count(5) == 8
    assert mock_soda_cloud.find_failed_rows_line_count(6) == 2
    assert mock_soda_cloud.find_failed_rows_line_count(7) == 2


def test_duplicate_samples(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - duplicate_count(cat) > 0
            - duplicate_count(cat, country) > 0
        """
    )
    scan.execute_unchecked()

    assert mock_soda_cloud.find_failed_rows_line_count(0) == 1
    assert mock_soda_cloud.find_failed_rows_line_count(1) == 1


def test_duplicate_percent_samples(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - duplicate_percent(cat) > 0
            - duplicate_percent(cat, country) > 0
        """
    )
    scan.execute_unchecked()

    assert mock_soda_cloud.find_failed_rows_line_count(0) == 1
    assert mock_soda_cloud.find_failed_rows_line_count(1) == 1


def test_duplicate_without_rows_samples(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - duplicate_count(id) = 0
        """
    )
    scan.execute_unchecked()

    assert len(mock_soda_cloud.files) == 0


sample_limit_config_header = "check, has_sample_query"
sample_limit_config = [
    pytest.param(
        "- missing_count(cat) = 0",
        True,
        id="missing_count",
    ),
    pytest.param(
        "- missing_percent(cat) = 0",
        True,
        id="missing_percent",
    ),
    pytest.param(
        """- invalid_count(cat) = 0:
                valid format: uuid""",
        True,
        id="invalid_count",
    ),
    pytest.param(
        """- invalid_percent(cat) = 0:
                valid format: uuid""",
        True,
        id="invalid_percent",
    ),
    pytest.param(
        "- duplicate_count(cat) = 0",
        True,
        id="duplicate_count",
    ),
    pytest.param(
        "- duplicate_percent(cat) = 0",
        True,
        id="duplicate_percent",
    ),
    pytest.param(
        "- values in (cst_size) must exist in {{another_table_name}} (cst_size)",
        True,
        id="reference",
    ),
    pytest.param(
        """- failed rows:
                fail condition: cat = 'HIGH' and cst_size < .7""",
        False,
        id="failed_rows_condition",
    ),
]


@pytest.mark.parametrize(
    sample_limit_config_header,
    sample_limit_config,
)
def test_sample_limit_configuration(check: str, has_sample_query: bool, data_source_fixture: DataSourceFixture):
    """
    Tests failed rows queries + sampler with user provided sample limit.
    Tests both the resulting count but checks whether query has the limit applied as well.
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    samples_limit = 1

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()

    check = check.replace("{{samples_limit}}", str(samples_limit))

    if "\n" not in check:
        check += ":"

    if "{{schema}}" in check:
        if data_source_fixture.data_source.schema:
            check = check.replace("{{schema}}", f"{data_source_fixture.data_source.schema}.")
        else:
            check = check.replace("{{schema}}", "")

    if "{{table_name}}" in check:
        check = check.replace("{{table_name}}", table_name)

    if "{{another_table_name}}" in check:
        another_table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
        check = check.replace("{{another_table_name}}", another_table_name)

    scan.add_sodacl_yaml_str(
        dedent(
            f"""
          checks for {table_name}:
            {check}
                samples limit: {samples_limit}
        """
        )
    )
    scan.execute()

    scan.assert_all_checks_fail()
    assert mock_soda_cloud.find_failed_rows_line_count(0) == samples_limit

    if has_sample_query:
        sample_queries = scan.get_sample_queries()

        assert len(sample_queries) == 1

        limit_keyword = data_source_fixture.data_source.LIMIT_KEYWORD
        assert f"{limit_keyword} {samples_limit}" in sample_queries[0]


@pytest.mark.parametrize(
    sample_limit_config_header,
    sample_limit_config,
)
def test_sample_limit_default(check: str, has_sample_query: bool, data_source_fixture: DataSourceFixture):
    """
    Tests failed rows queries + sampler without user provided limit.
    Tests both the resulting count but checks whether query has the limit applied as well.
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()

    if "{{schema}}" in check:
        if data_source_fixture.data_source.schema:
            check = check.replace("{{schema}}", f"{data_source_fixture.data_source.schema}.")
        else:
            check = check.replace("{{schema}}", "")

    if "{{table_name}}" in check:
        check = check.replace("{{table_name}}", table_name)

    if "{{another_table_name}}" in check:
        another_table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
        check = check.replace("{{another_table_name}}", another_table_name)

    scan.add_sodacl_yaml_str(
        dedent(
            f"""
          checks for {table_name}:
            {check}
        """
        )
    )
    scan.execute()

    scan.assert_all_checks_fail()

    if has_sample_query:
        sample_queries = scan.get_sample_queries()

        assert len(sample_queries) == 1

        limit_keyword = data_source_fixture.data_source.LIMIT_KEYWORD
        assert f"{limit_keyword} {DEFAULT_FAILED_ROWS_SAMPLE_LIMIT}" in sample_queries[0]
