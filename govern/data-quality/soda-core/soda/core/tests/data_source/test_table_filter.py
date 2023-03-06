from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_filter_on_date(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"DATE": "2020-06-23"})
    date_expr = "" if test_data_source == "sqlserver" else "DATE"
    scan.add_sodacl_yaml_str(
        f"""
          filter {table_name} [daily]:
            where: date_updated = {date_expr} '${{DATE}}'

          checks for {table_name}:
            - row_count = 10
            - missing_count(cat) = 5

          checks for {table_name} [daily]:
            - row_count = 6
            - missing_count(cat) = 2
            - schema:
                warn:
                    when forbidden column present: [non-existing]
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"date": "2020-06-24"})
    scan.add_sodacl_yaml_str(
        f"""
          filter {table_name} [daily]:
            where: date_updated = {date_expr} '${{date}}'

          checks for {table_name}:
            - row_count = 10
            - missing_count(cat) = 5

          checks for {table_name} [daily]:
            - row_count = 4
            - missing_count(cat) = 3
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_table_filter_on_timestamp(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    if test_data_source == "sqlserver":
        where_cond = f"""CONVERT(DATETIME, '${{ts_start}}') <= ts AND ts <  CONVERT(DATETIME,'${{ts_end}}')"""
    elif test_data_source == "dask":
        where_cond = f"""\"'${{ts_start}}' <= ts AND ts < '${{ts_end}}'\""""
    else:
        where_cond = f"""TIMESTAMP '${{ts_start}}' <= ts AND ts < TIMESTAMP '${{ts_end}}'"""

    scan.add_variables({"ts_start": "2020-06-23 00:00:00", "ts_end": "2020-06-24 00:00:00"})
    scan.add_sodacl_yaml_str(
        f"""
          filter {table_name} [daily]:
            where: {where_cond}

          checks for {table_name}:
            - row_count = 10
            - missing_count(cat) = 5

          checks for {table_name} [daily]:
            - row_count = 6
            - missing_count(cat) = 2
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"ts_start": "2020-06-24 00:00:00", "ts_end": "2020-06-25 00:00:00"})
    scan.add_sodacl_yaml_str(
        f"""
          filter {table_name} [daily]:
            where:  {where_cond}

          checks for {table_name}:
            - row_count = 10
            - missing_count(cat) = 5

          checks for {table_name} [daily]:
            - row_count = 4
            - missing_count(cat) = 3
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()
