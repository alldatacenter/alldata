from __future__ import annotations

from textwrap import dedent

import pytest
from helpers.common_test_tables import customers_test_table, orders_test_table
from helpers.data_source_fixture import DataSourceFixture


@pytest.mark.parametrize(
    "check, expected_passing_count, table, other_table",
    [
        pytest.param("missing_count(cat) = 0", 5, "customers", None, id="missing_count"),
        pytest.param("duplicate_count(cat) = 0", 2, "customers", None, id="duplicate_count"),
        pytest.param(
            """invalid_count(cst_size) = 0:
                            valid max: 10
                            valid min: 0""",
            4,
            "customers",
            None,
            id="invalid_count",
        ),
        pytest.param(
            "values in (customer_id_nok) must exist in {{other_table_name}} (id)",
            2,
            "orders",
            "customers",
            id="reference",
        ),
    ],
)
def test_pass_queries(
    check: str,
    expected_passing_count: int,
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

    passing_queries = scan.get_passing_queries()
    assert len(passing_queries) == 1
    passing_query = passing_queries[0]

    passing_result = data_source_fixture.data_source.fetchall(passing_query)
    assert len(passing_result) == expected_passing_count
