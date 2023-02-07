import json

import requests
from legacy.core.config import config


class Request(object):
    @staticmethod
    def get_error(res):
        try:
            return json.loads(res.content)
        except Exception as err:
            print(err)
            print(res.content)
            return {'error': 'not json content'}

    @staticmethod
    def get(path, params=None):
        try:
            res = requests.get(
                f'{config.data.api_address}{path}',
                params,
                headers={'Authorization': config.data.token},
            )
        except requests.exceptions.ConnectionError as err:
            print(f'error: {err}')
            return {'error': err}
        if res.status_code != 200:
            return Request.get_error(res)
        return json.loads(res.content)

    @staticmethod
    def post(path, data=None):
        try:
            res = requests.post(
                f'{config.data.api_address}{path}',
                json=data,
                headers={'Authorization': config.data.token},
            )
        except requests.exceptions.ConnectionError as err:
            print(f'error: {err}')
            return {'error': err}
        if res.status_code != 200:
            return Request.get_error(res)
        return json.loads(res.content)

    @staticmethod
    def put(path, data=None):
        try:
            res = requests.put(
                f'{config.data.api_address}{path}',
                json=data,
                headers={'Authorization': config.data.token},
            )
        except requests.exceptions.ConnectionError as err:
            print(f'error: {err}')
            return {'error': err}
        if res.status_code != 200:
            return Request.get_error(res)
        return json.loads(res.content)

    @staticmethod
    def delete(path=None):
        try:
            res = requests.delete(
                f'{config.data.api_address}{path}',
                headers={'Authorization': config.data.token},
            )
        except requests.exceptions.ConnectionError as err:
            print(f'error: {err}')
            return {'error': err}
        if res.status_code != 200:
            return Request.get_error(res)
        return json.loads(res.content)

    @staticmethod
    def upload(path=None, file=None, data=None):
        url = f'{config.data.api_address}{path}'
        try:
            res = requests.post(
                url,
                headers={'Authorization': config.data.token},
                data=data,
                files={
                    'file': open(file, 'rb')
                }
            )
        except requests.exceptions.ConnectionError as err:
            return {'error': err}
        if res.status_code != 200:
            return Request.get_error(res)
        return json.loads(res.content)
