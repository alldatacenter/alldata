from textwrap import dedent

import pytest
from helpers.common_test_tables import customers_dist_check_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_distribution_check(data_source_fixture: DataSourceFixture, mock_file_system):
    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                method: ks
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()


@pytest.mark.parametrize(
    "table, expectation",
    [
        pytest.param(
            customers_dist_check_test_table, "SELECT \n  cst_size \nFROM {schema_name}{table_name}\n LIMIT 1000000"
        ),
    ],
)
@pytest.mark.skipif(
    test_data_source == "mysql",
    reason="TODO: Need to check why schema/database name is not picked as prefix in CI",
)
def test_distribution_sql(data_source_fixture: DataSourceFixture, mock_file_system, table, expectation):
    table_name = data_source_fixture.ensure_test_table(table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()
    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
            checks for {table_name}:
                - distribution_difference(cst_size) >= 0.05:
                    distribution reference file:  {user_home_dir}/customers_cst_size_distribution_reference.yml
                    method: ks
        """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()

    if test_data_source == "spark_df":
        assert scan._checks[0].query.sql == expectation.format(table_name=table_name, schema_name="")
    elif test_data_source == "snowflake":
        assert scan._checks[0].query.sql == expectation.format(
            table_name=table_name,
            schema_name=f"{data_source_fixture.data_source.database}.{data_source_fixture.schema_name}.",
        )
    elif test_data_source == "sqlserver":
        expectation = "SELECT TOP 1000000 \n  cst_size \nFROM {schema_name}{table_name}"
        assert scan._checks[0].query.sql == expectation.format(
            table_name=table_name, schema_name=f"{data_source_fixture.schema_name}."
        )
    elif test_data_source == "bigquery":
        # bigquery does not prepends schemas but uses connector attributes to set default dataset.
        assert scan._checks[0].query.sql == expectation.format(table_name=table_name, schema_name="")
    elif test_data_source == "oracle":
        expectation = "SELECT \n  cst_size \nFROM {table_name}\n FETCH FIRST 1000000 ROWS ONLY"
    elif test_data_source in ["duckdb", "dask"]:
        # duckdb does not prepend schemas
        assert scan._checks[0].query.sql == expectation.format(table_name=table_name, schema_name="")
    elif test_data_source == "teradata":
        expectation = "SELECT TOP 1000000 \n  cst_size \nFROM {database}{table_name}"
        assert scan._checks[0].query.sql == expectation.format(
            table_name=table_name, database=f"{data_source_fixture.data_source.database}."
        )
    else:
        assert scan._checks[0].query.sql == expectation.format(
            table_name=table_name, schema_name=f"{data_source_fixture.schema_name}."
        )


def test_distribution_missing_bins_weights(data_source_fixture: DataSourceFixture, mock_file_system):
    from soda.scientific.distribution.comparison import MissingBinsWeightsException

    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                method: ks
    """
    )

    scan.execute(allow_error_warning=True)

    log_message = (
        'The DRO in your "/Users/johndoe/customers_cst_size_distribution_reference.yml" distribution reference file does'
        ' not contain a "distribution_reference" key with weights and bins. Make sure that before running "soda scan" you'
        ' create a DRO by running "soda update-dro". For more information visit the docs:\nhttps://docs.soda.io/soda-cl/distribution.html#generate-a-distribution-reference-object-dro.'
    )

    log = next(log for log in scan._logs.logs if isinstance(log.message, MissingBinsWeightsException))
    assert str(log.message) == log_message


def test_distribution_check_with_dro_name(data_source_fixture: DataSourceFixture, mock_file_system):
    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            customers_dro1:
                dataset: {table_name}
                column: cst_size
                distribution_type: continuous
                distribution_reference:
                    bins: [1, 2, 3]
                    weights: [0.5, 0.2, 0.3]

            customers_dro2:
                dataset: {table_name}
                column: cst_size
                distribution_type: continuous
                distribution_reference:
                    bins: [1, 2, 3]
                    weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size, customers_dro1) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                method: ks
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()


def test_distribution_check_without_method(data_source_fixture: DataSourceFixture, mock_file_system):
    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()


def test_distribution_check_with_filter_no_data(data_source_fixture: DataSourceFixture, mock_file_system):
    from soda.scientific.distribution.comparison import EmptyDistributionCheckColumn

    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                filter: cst_size > 1000000
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute(allow_error_warning=True)

    log_message = (
        "The column for which you defined this distribution check does not return any data. Make sure"
        " that the columns + filters that you use do not result in empty datasets. For more"
        " information visit the docs:\nhttps://docs.soda.io/soda-cl/distribution.html#define-a-distribution-check"
    )

    log = next(log for log in scan._logs.logs if isinstance(log.message, EmptyDistributionCheckColumn))
    assert str(log.message) == log_message


def test_distribution_check_with_sample(data_source_fixture: DataSourceFixture, mock_file_system):
    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()
    data_source_name = data_source_fixture.data_source_name
    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }
    sample_query = ""
    if data_source_name in ["postgres", "snowflake"]:
        sample_query = "TABLESAMPLE SYSTEM (100)"
    elif data_source_name == "sqlserver":
        sample_query = "TABLESAMPLE (100 PERCENT)"
    elif data_source_name == "athena":
        sample_query = "TABLESAMPLE BERNOULLI(100)"
    elif data_source_name == "bigquery":
        sample_query = "TABLESAMPLE SYSTEM (100 PERCENT)"
    else:
        sample_query = ""

    scan.add_sodacl_yaml_str(
        f"""
        checks for {table_name}:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                method: ks
                sample: {sample_query}
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()


def test_distribution_check_with_filter_and_partition(data_source_fixture: DataSourceFixture, mock_file_system) -> None:
    table_name = data_source_fixture.ensure_test_table(customers_dist_check_test_table)
    table_name = data_source_fixture.data_source.default_casify_table_name(table_name)

    scan = data_source_fixture.create_test_scan()

    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/customers_cst_size_distribution_reference.yml": dedent(
            f"""
            dataset: {table_name}
            column: cst_size
            distribution_type: continuous
            distribution_reference:
                bins: [1, 2, 3]
                weights: [0.5, 0.2, 0.3]
        """
        ).strip(),
    }

    scan.add_sodacl_yaml_str(
        f"""
        filter {table_name} [filtered]:
            where: cst_size > 0

        checks for {table_name} [filtered]:
            - distribution_difference(cst_size) >= 0.05:
                distribution reference file: {user_home_dir}/customers_cst_size_distribution_reference.yml
                method: ks
                filter: cst_size < 100
    """
    )

    scan.enable_mock_soda_cloud()
    scan.execute()
    scan.assert_all_checks_pass()
