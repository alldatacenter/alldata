import logging
import uuid
from typing import Dict, Optional

from ruamel.yaml import YAML
from soda.common.file_system import FileSystemSingleton
from soda.common.yaml_helper import to_yaml_str

logger = logging.getLogger(__name__)
yaml = YAML()


class ConfigHelper:
    """Helper class for handling global Soda config."""

    DEFAULT_CONFIG = {"send_anonymous_usage_stats": True, "user_cookie_id": str(uuid.uuid4())}
    # The possible load paths for the config file - /tmp in the worst case.
    LOAD_PATHS = ["~/.soda/config.yml", ".soda/config.yml", "/tmp/.soda/config.yml"]
    __instance = None
    __config: Dict = {}
    file_system = FileSystemSingleton.INSTANCE

    @staticmethod
    def get_instance(path: Optional[str] = None):
        if ConfigHelper.__instance is None:
            ConfigHelper(path)
        return ConfigHelper.__instance

    def __init__(self, path: Optional[str] = None):
        if ConfigHelper.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            ConfigHelper.__instance = self

        if path:
            self.LOAD_PATHS.insert(0, path)

        self.__config = self.config
        self._config_path = self.LOAD_PATHS[0]
        if not self.__config:
            self.init_config_file()

        self.__ensure_basic_config()

    @property
    def config_path(self) -> str:
        return self._config_path

    @config_path.setter
    def config_path(self, value):
        self._config_path = value

    @property
    def config(self) -> Dict:
        if not self.__config:
            self.__config = self.load_config()

        return self.__config

    def load_config(self) -> Dict:
        config = {}
        for path in self.LOAD_PATHS:
            logger.debug(f"Trying to load Soda Config file {path}.")

            if self.file_system.exists(path):
                content = yaml.load(self.file_system.file_read_as_str(path))
                if content:
                    config = content
                break

        return config

    def get_value(self, key: str, default_value=None):
        """Get value from loaded config."""
        return self.config.get(key, default_value)

    def init_config_file(self) -> None:
        """Init default config file if not present."""

        if self.file_system.exists(self.config_path):
            logger.debug(f"Config file {self.config_path} already exists")
        else:
            for destination in self.LOAD_PATHS:
                try:
                    logger.info(f"Trying to create config YAML file {destination} ...")
                    self.file_system.mkdirs(self.file_system.dirname(destination))
                    self.config_path = destination
                    self.upsert_config_file(self.DEFAULT_CONFIG)
                    return
                except OSError:
                    logger.warning(f"Unable to create config path in {destination}")

    def upsert_value(self, key: str, value: str):
        """Update or insert a value in the config file and refresh loaded state."""
        config = self.config
        config[key] = value
        self.upsert_config_file(config)
        self.__config = self.load_config()

    def upsert_config_file(self, config: Dict):
        """Write provided config into a yaml file."""
        self.file_system.file_write_from_str(
            self.config_path,
            to_yaml_str(config),
        )

    @staticmethod
    def generate_user_cookie_id() -> str:
        return str(uuid.uuid4())

    def __ensure_basic_config(self) -> None:
        for key, value in self.DEFAULT_CONFIG.items():
            if key not in self.config:
                self.upsert_value(key, value)

    @property
    def send_anonymous_usage_stats(self) -> bool:
        return self.config.get("send_anonymous_usage_stats", True)
