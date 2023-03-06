from helpers.data_source_fixture import DataSourceFixture


def test_pandas_df(data_source_fixture: DataSourceFixture):
    import duckdb
    import pandas as pd

    con = duckdb.connect(database=":memory:")
    test_df = pd.DataFrame.from_dict({"i": [1, 2, 3, 4], "j": ["one", "two", "three", "four"]})

    scan = data_source_fixture.create_test_scan()
    scan.add_duckdb_connection(con)
    scan.add_sodacl_yaml_str(
        f"""
          checks for test_df:
            - row_count = 4
            - missing_count(i) = 0
            - missing_count(j) = 0
        """
    )
    scan.execute(allow_warnings_only=True)
    scan.assert_all_checks_pass()
    scan.assert_no_error_logs()
