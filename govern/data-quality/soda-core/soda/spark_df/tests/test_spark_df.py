from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable
from soda.execution.data_type import DataType


def test_spark_df_complex_data_types(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(
        TestTable(
            # fmt: off
        name="SparkDfDataComplexTypes",
        columns=[
            ("a", DataType.array(DataType.TEXT)),
            ("o", DataType.struct(
                    {
                        "nested1": DataType.TEXT,
                        "nested2": DataType.INTEGER
                    }
                )
            )
        ],
        values=[
            (["a", "b"], {"nested1": "b"}),
            (None, None),
        ]
            # fmt: on
        )
    )

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_count(a) >= 0
        """
    )
    scan.execute(allow_warnings_only=True)
    scan.assert_all_checks_pass()
    scan.assert_no_error_logs()

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          discover tables:
            tables:
              - include {table_name}
        """
    )
    scan.execute(allow_warnings_only=True)
    scan.assert_no_error_logs()

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          profile columns:
            columns:
              - include {table_name}.%
        """
    )
    scan.execute(allow_warnings_only=True)
    scan.assert_no_error_logs()

    scan = data_source_fixture.create_test_scan()
    scan.enable_mock_soda_cloud()
    scan.enable_mock_sampler()
    scan.add_sodacl_yaml_str(
        f"""
          sample datasets:
            tables:
              - include {table_name}
        """
    )
    scan.execute(allow_warnings_only=True)
    scan.assert_no_error_logs()
