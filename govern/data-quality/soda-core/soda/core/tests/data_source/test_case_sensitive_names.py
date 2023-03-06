import pytest
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source
from helpers.test_table import TestTable
from soda.execution.data_type import DataType


@pytest.mark.skipif(
    test_data_source in ["athena", "spark", "mysql"],
    reason="Case sensitive identifiers not supported in tested data source.",
)
def test_row_count_thresholds_passing(data_source_fixture: DataSourceFixture):
    """
    Tests all passing thresholds on a simple row count
    """
    table_name = data_source_fixture.ensure_test_table(
        TestTable(
            name="CaseSensitive",
            columns=[("Id", DataType.TEXT)],
            quote_names=True,
        )
    )

    quoted_table_name = data_source_fixture.data_source.quote_table(table_name)
    quoted_id = data_source_fixture.data_source.quote_column("Id")

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {quoted_table_name}:
        - row_count = 0
        - missing_count({quoted_id}) = 0
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()
