from tempfile import NamedTemporaryFile

import pytest
from pytest_mock import MockerFixture

import feathr.utils._env_config_reader
from feathr.utils._env_config_reader import EnvConfigReader


TEST_CONFIG_KEY = "test__config__key"
TEST_CONFIG_ENV_VAL = "test_env_val"
TEST_CONFIG_FILE_VAL = "test_file_val"
TEST_CONFIG_FILE_CONTENT = f"""
test:
    config:
        key: '{TEST_CONFIG_FILE_VAL}'
"""
TEST_CONFIG_AKV_VAL = "test_akv_val"


@pytest.mark.parametrize(
    "env_value, config_file_content, akv_value, expected_value",
    [
        (TEST_CONFIG_ENV_VAL, TEST_CONFIG_FILE_CONTENT, TEST_CONFIG_AKV_VAL, TEST_CONFIG_ENV_VAL),
        (None, TEST_CONFIG_FILE_CONTENT, TEST_CONFIG_AKV_VAL, TEST_CONFIG_FILE_VAL),
        (None, "", TEST_CONFIG_AKV_VAL, TEST_CONFIG_AKV_VAL),
        (None, "", None, "default"),
    ]
)
def test__envvariableutil__get(
    mocker: MockerFixture,
    env_value: str,
    config_file_content: str,
    akv_value: str,
    expected_value: str,
):
    """Test `get` method if it returns the expected value.
    """
    # Mock env variables
    mocker.patch.object(feathr.utils._env_config_reader.os, "environ", {
        TEST_CONFIG_KEY: env_value,
        "secrets__azure_key_vault__name": "test_akv_name",
    })
    # Mock AKS
    mocker.patch.object(
        feathr.utils._env_config_reader.AzureKeyVaultClient,
        "get_feathr_akv_secret",
        return_value=akv_value,
    )
    # Generate config file
    f = NamedTemporaryFile(delete=True)
    f.write(config_file_content.encode())
    f.seek(0)
    env_config = EnvConfigReader(config_path=f.name)

    assert env_config.get(TEST_CONFIG_KEY, default="default") == expected_value


@pytest.mark.parametrize(
    "env_value, akv_value, expected_value",
    [
        (TEST_CONFIG_ENV_VAL, TEST_CONFIG_AKV_VAL, TEST_CONFIG_ENV_VAL),
        (None, TEST_CONFIG_AKV_VAL, TEST_CONFIG_AKV_VAL),
        (None, None, None),
    ]
)
def test__envvariableutil__get_from_env_or_akv(
    mocker: MockerFixture,
    env_value: str,
    akv_value: str,
    expected_value: str,
):
    """Test `get_from_env_or_akv` method if it returns the expected value.
    """
    # Mock env variables
    mocker.patch.object(feathr.utils._env_config_reader.os, "environ", {
        TEST_CONFIG_KEY: env_value,
        "secrets__azure_key_vault__name": "test_akv_name",
    })
    # Mock AKS
    mocker.patch.object(
        feathr.utils._env_config_reader.AzureKeyVaultClient,
        "get_feathr_akv_secret",
        return_value=akv_value,
    )

    env_config = EnvConfigReader(config_path="")
    assert env_config.get_from_env_or_akv(TEST_CONFIG_KEY) == expected_value
