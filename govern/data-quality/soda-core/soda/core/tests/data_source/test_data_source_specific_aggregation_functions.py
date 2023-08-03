from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_data_source_specific_statistics_aggregation_metrics(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    all_checks = {
        "stdev": "stddev(cst_size) between 3.26 and 3.27",
        "stddev_pop": "stddev_pop(cst_size) between 3.02 and 3.03",
        "stddev_samp": "stddev_samp(cst_size) between 3.26 and 3.27",
        "variance(cst_size)": "variance(cst_size) between 10.65 and 10.66",
        "var_pop(cst_size)": "var_pop(cst_size) between 9.13 and 9.14",
        "var_samp(cst_size)": "var_samp(cst_size) between 10.65 and 10.66",
        "percentile(distance, 0.7)": "percentile(distance, 0.7) = 999",
    }

    supported_checks = all_checks

    if test_data_source in ["bigquery", "redshift", "athena"]:
        supported_checks.pop("percentile(distance, 0.7)")

    if test_data_source in ["dask"]:
        supported_checks.pop("percentile(distance, 0.7)")
        # TODO: Dask variaance does not work if we don't explicitly group by the column
        # most likely because there is a bug in the dask-sql implementation
        supported_checks.pop("variance(cst_size)")
        supported_checks.pop("var_samp(cst_size)")
        # Stddev_pop and stddev yields the same result in Dask
        supported_checks.pop("stddev_samp")
    # TODO see what's going wrong with Vertica later:
    # Message: Function APPROXIMATE_PERCENTILE(int) does not exist
    if test_data_source in ["sqlserver", "mysql", "spark_df", "oracle", "vertica"]:
        supported_checks = {}

    if supported_checks:
        checks_str = ""
        for check in supported_checks.values():
            checks_str += f"  - {check}\n"

        scan = data_source_fixture.create_test_scan()
        scan.add_sodacl_yaml_str(
            f"""
checks for {table_name}:
{checks_str}
configurations for {table_name}:
    valid min for distance: 0
"""
        )
        scan.execute()

        scan.assert_all_checks_pass()
