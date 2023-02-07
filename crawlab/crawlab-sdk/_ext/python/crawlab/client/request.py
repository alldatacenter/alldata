import os

import requests

from crawlab.config.config import config
from crawlab.constants.request import DEFAULT_CRAWLAB_API_ADDRESS
from crawlab.constants.upload import CLI_DEFAULT_CONFIG_KEY_API_ADDRESS, CLI_DEFAULT_CONFIG_KEY_TOKEN
from crawlab.errors.upload import HttpException


def get_api_address() -> str:
    return config.data.get(CLI_DEFAULT_CONFIG_KEY_API_ADDRESS) \
           or os.environ.get('CRAWLAB_API_ADDRESS') \
           or DEFAULT_CRAWLAB_API_ADDRESS


def get_api_token() -> str:
    return config.data.get(CLI_DEFAULT_CONFIG_KEY_TOKEN)


def http_request(method: str, url: str, params: dict = None, data: dict = None, headers: dict = None,
                 token: str = None, files: dict = None):
    # headers
    if headers is None:
        headers = {
            'Content-Type': 'application/json'
        }

    # url
    if not url.startswith('http'):
        url = f'{get_api_address()}{url}'

    # token
    if token is None:
        token = get_api_token()
    headers['Authorization'] = token

    # args
    kwargs = {}
    if headers.get('Content-Type') == 'application/json':
        kwargs['json'] = data
    else:
        kwargs['data'] = data

    # response
    res = requests.request(method=method, url=url, params=params, headers=headers, files=files, **kwargs)

    # status code: ok
    if res.status_code == 200:
        return res

    try:
        res_data = res.json()
    except Exception as ex:
        return HttpException(res.text)

    if res_data is not None and res_data.get('error') is not None:
        # error with message
        err_msg = f'[status code: {res.status_code}] {res.json().get("error")}'
    else:
        # error with no message
        err_msg = f'[status code: {res.status_code}] unexpected error'
    raise HttpException(err_msg)


def http_get(url: str, params: dict = None, headers: dict = None, **kwargs):
    return http_request(method='GET', url=url, params=params, headers=headers, **kwargs)


def http_put(url: str, data: dict = None, headers: dict = None, **kwargs):
    return http_request(method='PUT', url=url, data=data, headers=headers, **kwargs)


def http_post(url: str, data: dict = None, headers: dict = None, **kwargs):
    return http_request(method='POST', url=url, data=data, headers=headers, **kwargs)


def http_delete(url: str, data: dict = None, headers: dict = None, **kwargs):
    return http_request(method='DELETE', url=url, data=data, headers=headers, **kwargs)
