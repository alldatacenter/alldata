import os

from grpc_interceptor_headers.header_manipulator_client_interceptor import header_adder_interceptor


def _get_auth_token_env() -> str:
    return os.getenv('CRAWLAB_GRPC_AUTH_KEY')


def get_auth_token_interceptor():
    header_name = 'authorization'
    header_content = _get_auth_token_env()
    header_interceptor = header_adder_interceptor(header_name, header_content)
    return header_interceptor
