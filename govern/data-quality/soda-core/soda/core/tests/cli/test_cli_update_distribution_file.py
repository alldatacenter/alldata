from textwrap import dedent

import pytest
from cli.run_cli import run_cli
from helpers.common_test_tables import (
    customers_test_table,
    dro_categorical_test_table,
    null_test_table,
)
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source
from helpers.mock_file_system import MockFileSystem
from ruamel.yaml import YAML
from soda.cli.cli import DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT


def mock_file_system_and_run_cli(mock_file_system, data_source_fixture, dro_file):
    user_home_dir = mock_file_system.user_home_dir()
    mock_file_system.files = {
        f"{user_home_dir}/configuration.yml": data_source_fixture.create_test_configuration_yaml_str(),
        f"{user_home_dir}/customers_distribution_reference.yml": dro_file,
    }

    run_cli_output = run_cli(
        [
            "update-dro",
            "-c",
            "configuration.yml",
            "-d",
            data_source_fixture.data_source.data_source_name,
            f"{user_home_dir}/customers_distribution_reference.yml",
        ]
    )
    return run_cli_output


@pytest.mark.skipif(
    test_data_source not in DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT,
    reason="Support for other data sources is experimental and not tested",
)
@pytest.mark.parametrize(
    "dro_config",
    [
        pytest.param(
            dedent(
                """
            table: {table_name}
            column: cst_size
            distribution_type: continuous
            """
            ),
            id="no sample, no filter",
        ),
        pytest.param(
            dedent(
                """
                table: {table_name}
                column: cst_size
                distribution_type: continuous
                sample: TABLESAMPLE BERNOULLI(50) REPEATABLE(61)
                """
            ),
            id="with sample, no filter",
        ),
        pytest.param(
            dedent(
                """
                table: {table_name}
                column: cst_size
                distribution_type: continuous
                filter: cst_size > 0
                """
            ),
            id="no sample, with filter",
        ),
        pytest.param(
            dedent(
                """
                table: {table_name}
                column: cst_size
                distribution_type: continuous
                sample: TABLESAMPLE BERNOULLI(50) REPEATABLE(61)
                filter: cst_size > 0
                """
            ),
            id="with sample, with filter",
        ),
        pytest.param(
            dedent(
                """
                table: {table_name}
                column: cst_size
                distribution_type: continuous
                filter: random() > 0.5
                """
            ),
            id="hacky sample with filter",
        ),
    ],
)
def test_cli_update_distribution_file_filter_and_sample(
    data_source_fixture: DataSourceFixture, mock_file_system: MockFileSystem, dro_config: str
):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    dro_file = dro_config.format(table_name=table_name)
    mock_file_system_and_run_cli(mock_file_system, data_source_fixture, dro_file)


@pytest.mark.skipif(
    test_data_source not in DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT,
    reason="Support for other data sources is experimental and not tested",
)
@pytest.mark.parametrize(
    "distribution_type, expected_bins_and_weights",
    [
        pytest.param(
            "continuous",
            [
                (-3.0, 0.0),
                (-0.75, 0.2857142857142857),
                (1.5, 0.42857142857142855),
                (3.75, 0.0),
                (6.0, 0.2857142857142857),
            ],
            id="continuous column",
        ),
        pytest.param(
            "categorical",
            [
                (0, 0.13636363636363635),
                (1, 0.13636363636363635),
                (2, 0.2727272727272727),
                (3, 0.18181818181818182),
                (6, 0.2727272727272727),
            ],
            id="categorical column",
        ),
    ],
)
def test_cli_update_distribution_file_bins_and_weights(
    data_source_fixture: DataSourceFixture,
    mock_file_system: MockFileSystem,
    distribution_type: str,
    expected_bins_and_weights: list,
):
    if distribution_type == "continuous":
        table_name = data_source_fixture.ensure_test_table(customers_test_table)
        dro_file = dedent(
            f"""
            table: {table_name}
            column: cst_size
            distribution_type: continuous
            """
        )
    else:
        table_name = data_source_fixture.ensure_test_table(dro_categorical_test_table)
        dro_file = dedent(
            f"""
                table: {table_name}
                column: categorical_value
                distribution_type: categorical
                """
        )
    mock_file_system_and_run_cli(mock_file_system, data_source_fixture, dro_file)
    user_home_dir = mock_file_system.user_home_dir()
    parsed_dro: dict = YAML().load(mock_file_system.files[f"{user_home_dir}/customers_distribution_reference.yml"])
    weights = parsed_dro["distribution_reference"]["weights"]
    bins = parsed_dro["distribution_reference"]["bins"]
    bins_and_weights_sorted = sorted(zip(bins, weights))
    assert bins_and_weights_sorted == expected_bins_and_weights


@pytest.mark.skipif(
    test_data_source not in DATA_SOURCES_WITH_DISTRIBUTION_CHECK_SUPPORT,
    reason="Support for other data sources is experimental and not tested",
)
@pytest.mark.parametrize(
    "distribution_type",
    [
        pytest.param("continuous", id="continuous distribution type"),
        pytest.param("categorical", id="categorical distribution type"),
    ],
)
def test_cli_update_distribution_errors(
    data_source_fixture: DataSourceFixture, mock_file_system: MockFileSystem, distribution_type
):
    table_name = data_source_fixture.ensure_test_table(null_test_table)

    dro_file = dedent(
        f"""
        table: {table_name}
        column: column_with_null_values
        distribution_type: {distribution_type}
        """
    )
    cli_output = mock_file_system_and_run_cli(mock_file_system, data_source_fixture, dro_file).output

    assert (
        "column_with_null_values column has only NULL values! To generate a distribution reference object (DRO) your column needs to have more than 0 not null values!"
        in cli_output
    )
