from pathlib import Path
import yaml

import pytest
from pytest_mock import MockerFixture

import feathr.utils.config
from feathr.utils.config import generate_config


@pytest.mark.parametrize(
    "output_filepath", [None, "config.yml"],
)
def test__generate_config__output_filepath(
    output_filepath: str,
    tmp_path: Path,
):
    resource_prefix = "test_prefix"
    project_name = "test_project"

    # Use tmp_path so that the test files get cleaned up after the tests
    if output_filepath:
        output_filepath = str(tmp_path / output_filepath)

    config_filepath = generate_config(
        resource_prefix=resource_prefix,
        project_name=project_name,
        output_filepath=output_filepath,
        spark_config__spark_cluster="local",
    )

    # Assert if the config file was generated in the specified output path.
    if output_filepath:
        assert output_filepath == config_filepath

    # Assert the generated config string is correct.
    with open(config_filepath, "r") as f:
        config = yaml.safe_load(f)

    assert config["project_config"]["project_name"] == project_name
    assert config["feature_registry"]["api_endpoint"] == f"https://{resource_prefix}webapp.azurewebsites.net/api/v1"
    assert config["spark_config"]["spark_cluster"] == "local"
    assert config["online_store"]["redis"]["host"] == f"{resource_prefix}redis.redis.cache.windows.net"


@pytest.mark.parametrize(
    "spark_cluster,env_key,kwargs",
    [
        ("local", None, dict()),
        (
            "databricks",
            "DATABRICKS_WORKSPACE_TOKEN_VALUE",
            dict(spark_config__databricks__workspace_instance_url="databricks_url"),
        ),
        (
            "azure_synapse",
            "ADLS_KEY",
            dict(
                spark_config__azure_synapse__dev_url="synapse_url",
                spark_config__azure_synapse__pool_name="pool_name",
            ),
        ),
    ]
)
def test__generate_config__spark_cluster(
    mocker: MockerFixture,
    spark_cluster: str,
    env_key: str,
    kwargs: str,
):
    """Test if spark cluster specific configs are generated without errors.
    TODO - For now, this test doesn't check if the config values are correctly working with the actual Feathr client.
    """
    # Mock the os.environ to return the specified env vars
    mocker.patch.object(feathr.utils.config.os, "environ", {env_key: "some_value"})

    generate_config(
        resource_prefix="test_prefix",
        project_name="test_project",
        spark_config__spark_cluster=spark_cluster,
        **kwargs,
    )


@pytest.mark.parametrize(
    "adls_key,pool_name,expected_error",
    [
        ("some_key", "some_name", None),
        (None, "some_name", ValueError),
        ("some_key", None, ValueError),
    ]
)
def test__generate_config__azure_synapse_exceptions(
    mocker: MockerFixture,
    adls_key: str,
    pool_name: str,
    expected_error: Exception,
):
    """Test if exceptions are raised when databricks url and token are not provided."""

    # Either env vars or argument should yield the same result
    for environ in [{"ADLS_KEY": adls_key}, {
        "ADLS_KEY": adls_key,
        "SPARK_CONFIG__AZURE_SYNAPSE__POOL_NAME": pool_name,
    }]:
        # Mock the os.environ to return the specified env vars
        mocker.patch.object(feathr.utils.config.os, "environ", environ)

        # Test either using env vars or arguments
        if "SPARK_CONFIG__AZURE_SYNAPSE__POOL_NAME" in environ:
            kwargs = dict()
        else:
            kwargs = dict(spark_config__azure_synapse__pool_name=pool_name)

        if expected_error is None:
            generate_config(
                resource_prefix="test_prefix",
                project_name="test_project",
                spark_config__spark_cluster="azure_synapse",
                **kwargs,
            )
        else:
            with pytest.raises(ValueError):
                generate_config(
                    resource_prefix="test_prefix",
                    project_name="test_project",
                    spark_config__spark_cluster="azure_synapse",
                    **kwargs,
                )


@pytest.mark.parametrize(
    "databricks_token,workspace_url,expected_error",
    [
        ("some_token", "some_url", None),
        (None, "some_url", ValueError),
        ("some_token", None, ValueError),
    ]
)
def test__generate_config__databricks_exceptions(
    mocker: MockerFixture,
    databricks_token: str,
    workspace_url: str,
    expected_error: Exception,
):
    """Test if exceptions are raised when databricks url and token are not provided."""

    # Either env vars or argument should yield the same result
    for environ in [{"DATABRICKS_WORKSPACE_TOKEN_VALUE": databricks_token}, {
        "DATABRICKS_WORKSPACE_TOKEN_VALUE": databricks_token,
        "SPARK_CONFIG__DATABRICKS__WORKSPACE_INSTANCE_URL": workspace_url,
    }]:
        # Mock the os.environ to return the specified env vars
        mocker.patch.object(feathr.utils.config.os, "environ", environ)

        # Test either using env vars or arguments
        if "SPARK_CONFIG__DATABRICKS__WORKSPACE_INSTANCE_URL" in environ:
            kwargs = dict()
        else:
            kwargs = dict(spark_config__databricks__workspace_instance_url=workspace_url)

        if expected_error is None:
            generate_config(
                resource_prefix="test_prefix",
                project_name="test_project",
                spark_config__spark_cluster="databricks",
                **kwargs,
            )
        else:
            with pytest.raises(ValueError):
                generate_config(
                    resource_prefix="test_prefix",
                    project_name="test_project",
                    spark_config__spark_cluster="databricks",
                    **kwargs,
                )
