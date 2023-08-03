from __future__ import annotations

from textwrap import dedent

import pytest
from helpers.common_test_tables import customers_test_table, orders_test_table
from helpers.data_source_fixture import DataSourceFixture


@pytest.mark.parametrize(
    "check, table, other_table",
    [
        pytest.param("missing_count(cat) = 0", "customers", None, id="missing_count"),
        pytest.param("duplicate_count(zip) = 0", "customers", None, id="duplicate_count"),
        pytest.param("duplicate_percent(zip) = 0", "customers", None, id="duplicate_percent"),
        pytest.param(
            """invalid_count(cst_size) = 0:
                            valid max: 10
                            valid min: 0""",
            "customers",
            None,
            id="invalid_count",
        ),
        pytest.param(
            "values in (customer_id_nok) must exist in {{other_table_name}} (id)",
            "orders",
            "customers",
            id="reference",
        ),
    ],
)
def test_pass_fail_queries(
    check: str,
    table: str | None,
    other_table: str,
    data_source_fixture: DataSourceFixture,
):
    customers_table_name = data_source_fixture.ensure_test_table(customers_test_table)

    table_name = customers_table_name
    other_table_name = None

    if table:
        if table == "orders":
            orders_table_name = data_source_fixture.ensure_test_table(orders_test_table)
            table_name = orders_table_name

    if other_table:
        if other_table == "customers":
            other_table_name = customers_table_name

    if "{{table_name}}" in check:
        check = check.replace("{{table_name}}", table_name)

    if "{{other_table_name}}" in check:
        check = check.replace("{{other_table_name}}", other_table_name)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        dedent(
            f"""
            checks for {table_name}:
                - {check}
            """
        )
    )
    scan.execute_unchecked()

    scan.assert_all_checks_fail()
    result = mock_soda_cloud.build_scan_results(scan)

    assert len(result["checks"]) == 1

    block = mock_soda_cloud.find_failed_rows_diagnostics_block(0)
    assert block["type"] == "failedRowsAnalysis"
    assert block["failingRowsQueryName"]
    assert block["passingRowsQueryName"]


def _find_query_by_name(queries: list[dict], name: str) -> dict | None:
    for query in queries:
        if query["name"] == name:
            return query
    return None


def test_with_multiple_aggregate_checks(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    mock_soda_cloud = scan.enable_mock_soda_cloud()
    scan.add_sodacl_yaml_str(
        dedent(
            f"""
            checks for {table_name}:
                - missing_count(cat) = 0:
                    name: missing_count cat
                - missing_count(id) = 0:
                    name: missing_count id
            """
        )
    )
    scan.execute_unchecked()

    scan.assert_all_checks_fail()
    result = mock_soda_cloud.build_scan_results(scan)

    assert len(result["checks"]) == 2

    result = scan.build_scan_results()

    block_cat = mock_soda_cloud.find_failed_rows_diagnostics_block(0)
    assert block_cat["type"] == "failedRowsAnalysis"
    assert "cat.failed_rows[missing_count].failing_sql" in block_cat["failingRowsQueryName"]
    assert "cat.failed_rows[missing_count].passing_sql" in block_cat["passingRowsQueryName"]
    cat_failing_query = _find_query_by_name(result["queries"], block_cat["failingRowsQueryName"])
    assert "where cat is null" in cat_failing_query["sql"].lower()

    block_id = mock_soda_cloud.find_failed_rows_diagnostics_block(1)
    assert block_cat["type"] == "failedRowsAnalysis"
    assert "id.failed_rows[missing_count].failing_sql" in block_id["failingRowsQueryName"]
    assert "id.failed_rows[missing_count].passing_sql" in block_id["passingRowsQueryName"]
    id_failing_query = _find_query_by_name(result["queries"], block_id["failingRowsQueryName"])
    assert "where id is null" in id_failing_query["sql"].lower()
