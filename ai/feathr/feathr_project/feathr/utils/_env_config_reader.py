import os
from pathlib import Path
import yaml

from loguru import logger

from azure.core.exceptions import ResourceNotFoundError
from feathr.secrets.akv_client import AzureKeyVaultClient

class EnvConfigReader(object):
    """A utility class to read Feathr environment variables either from os environment variables,
    the config yaml file or Azure Key Vault.
    If a key is set in the environment variable, ConfigReader will return the value of that environment variable.
    """
    akv_name: str = None      # Azure Key Vault name to use for retrieving config values.
    yaml_config: dict = None  # YAML config file content.

    def __init__(self, config_path: str):
        """Initialize the utility class.

        Args:
            config_path: Config file path.
        """
        if config_path is not None:
            config_path = Path(config_path)
            if config_path.is_file():
                try:
                    self.yaml_config = yaml.safe_load(config_path.read_text())
                except yaml.YAMLError as e:
                    logger.warning(e)

        self.akv_name = self.get("secrets__azure_key_vault__name")
        self.akv_client = AzureKeyVaultClient(self.akv_name) if self.akv_name else None

    def get(self, key: str, default: str = None) -> str:
        """Gets the Feathr config variable for the given key.
        It will retrieve the value in the following order:
            - From the environment variable if the key is set in the os environment variables.
            - From the config yaml file if the key exists.
            - From the Azure Key Vault.
        If the key is not found in any of the above, it will return `default`.

        Args:
            key: Config variable name. For example, `SPARK_CONFIG__DATABRICKS__WORKSPACE_INSTANCE_URL`
            default (optional): Default value to return if the key is not found. Defaults to None.

        Returns:
            Feathr client's config value.
        """
        val = self._get_variable_from_env(key)
        # `val` could be a boolean value, so we need to check if it is None.
        if val is None and self.yaml_config is not None:
            val = self._get_variable_from_file(key)
        if val is None and self.akv_name is not None:
            val = self._get_variable_from_akv(key)

        if val is not None:
            return val
        else:
            logger.info(f"Config {key} is not found in the environment variable, configuration file, or the remote key value store. Returning the default value: {default}.")
            return default

    def get_from_env_or_akv(self, key: str) -> str:
        """Gets the Feathr config variable for the given key.
        It will retrieve the value in the following order:
            - From the environment variable if the key is set in the os environment variables.
            - From the Azure Key Vault.
        If the key is not found in any of the above, it will return None.

        Args:
            key: Config variable name. For example, `ADLS_ACCOUNT`

        Returns:
            Feathr client's config value.
        """
        val = self._get_variable_from_env(key)
        # `val` could be a boolean value, so we need to check if it is None.
        if val is None and self.akv_name is not None:
            val = self._get_variable_from_akv(key)

        if val is not None:
            return val
        else:
            logger.warning(f"Config {key} is not found in the environment variable or the remote key value store.")
            return None

    def _get_variable_from_env(self, key: str) -> str:
        # make it work for lower case and upper case.
        conf_var = os.environ.get(key.lower(), os.environ.get(key.upper()))

        return conf_var

    def _get_variable_from_akv(self, key: str) -> str:
        try:
            # Azure Key Vault object name is case in-sensitive.
            # https://learn.microsoft.com/en-us/azure/key-vault/general/about-keys-secrets-certificates#vault-name-and-object-name
            return self.akv_client.get_feathr_akv_secret(key)
        except ResourceNotFoundError:
            logger.warning(f"Resource {self.akv_name} not found")

        return None

    def _get_variable_from_file(self, key: str) -> str:
        args = key.split("__")
        try:
            conf_var = self.yaml_config
            for arg in args:
                if conf_var is None:
                    break
                # make it work for lower case and upper case.
                conf_var = conf_var.get(arg.lower(), conf_var.get(arg.upper()))

            return conf_var
        except Exception as e:
            logger.warning(e)

        return None
