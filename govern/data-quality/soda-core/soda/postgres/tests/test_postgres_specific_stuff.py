import pytest
from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable


@pytest.mark.skip("New tests structure is breaking datasource specific tests in CI, Investigation pending")
def test_row_count_thresholds_passing(data_source_fixture: DataSourceFixture):
    """
    Tests all passing thresholds on a simple row count
    """
    table_name = data_source_fixture.ensure_test_table(
        TestTable(
            name="TYPES",
            columns=[
                # numeric types
                ("c01", "smallint"),
                ("c02", "bigint"),
                ("c03", "integer"),
                ("c04", "bigint"),
                ("c05", "decimal"),
                ("c06", "numeric"),
                ("c07", "real"),
                ("c08", "double precision"),
            ],
            # fmt:off
            values=[
                (1, 1, 1, 1, 1, 1, 1, 1),
                (None, None, None, None, None, None, None, None)
            ],
            # fmt:on
        )
    )

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - missing_count(c01) = 1
        - missing_count(c02) = 1
        - missing_count(c03) = 1
        - missing_count(c04) = 1
        - missing_count(c05) = 1
        - missing_count(c06) = 1
        - missing_count(c07) = 1
        - missing_count(c08) = 1
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
