import os
from starlette.config import Config

env_file = os.path.join("registry", "access_control", ".env")
config = Config(os.path.abspath(env_file))

def _get_config(key:str, config:Config = config):
    return os.environ.get(key) or config.get(key)

# API Settings
RBAC_API_BASE: str = _get_config("RBAC_API_BASE")

# Authentication
RBAC_API_CLIENT_ID: str = _get_config("RBAC_API_CLIENT_ID")
RBAC_AAD_TENANT_ID: str = _get_config("RBAC_AAD_TENANT_ID")
RBAC_AAD_INSTANCE: str = _get_config("RBAC_AAD_INSTANCE")
RBAC_API_AUDIENCE: str = _get_config("RBAC_API_AUDIENCE")

# SQL Database
RBAC_CONNECTION_STR: str = _get_config("RBAC_CONNECTION_STR")

# Downstream API Endpoint
RBAC_REGISTRY_URL: str = _get_config("RBAC_REGISTRY_URL")
