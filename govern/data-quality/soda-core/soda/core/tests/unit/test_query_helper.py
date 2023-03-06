import pytest
from soda.common.query_helper import parse_columns_from_query


@pytest.mark.parametrize(
    "query, outcome",
    [
        pytest.param(
            """WITH frequencies AS (
              SELECT something, COUNT(*) AS frequency
              FROM test
              WHERE a = 1
              GROUP BY something)
            SELECT
                *
            FROM frequencies
            WHERE frequency > 1""",
            ["*"],
            id="cte with *",
        ),
        pytest.param(
            """SELECT SOURCE.*
            FROM sodatest_customers_6c2f3574 as SOURCE
            LEFT JOIN sodatest_customersdist_55bf46c2 as TARGET on SOURCE.cst_size = TARGET.cst_size
            WHERE (SOURCE.cst_size IS NOT NULL AND TARGET.cst_size IS NULL)""",
            ["*"],
            id="* with prefix",
        ),
        pytest.param(
            """SELECT
            a, b, c as D
            FROM frequencies""",
            ["a", "b", "c"],
            id="alias",
        ),
        pytest.param(
            """SELECT SOURCE.id, SOURCE.cst_size, SOURCE.cst_size_txt
            FROM dev_m1n0.sodatest_customers_6c2f3574 as SOURCE
            LEFT JOIN dev_m1n0.sodatest_customersdist_55bf46c2 as TARGET on SOURCE.cst_size = TARGET.cst_size
            WHERE (SOURCE.cst_size IS NOT NULL AND TARGET.cst_size IS NULL)""",
            ["id", "cst_size", "cst_size_txt"],
            id="columns with prefix",
        ),
        pytest.param(
            """SELECT TOP 100 cat
            FROM table
            WHERE frequency > 1
            """,
            ["cat"],
            id="sqlserver TOP X",
        ),
        pytest.param(
            """SELECT TOP(100) cat
            FROM table
            WHERE frequency > 1
            """,
            ["cat"],
            id="sqlserver TOP(X)",
        ),
    ],
)
def test_parse_columns_from_query(query, outcome):
    columns = parse_columns_from_query(query)

    assert sorted(columns) == sorted(outcome)
