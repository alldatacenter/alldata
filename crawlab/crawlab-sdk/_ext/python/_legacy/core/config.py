import os
import json
import binascii

from legacy.core import CRAWLAB_ROOT


class Data(object):
    username = ''
    password = ''
    token = ''
    api_address = 'http://localhost:8080/api'

    @property
    def dict(self):
        return {
            'username': self.username,
            'password': self.password,
            'token': self.token,
            'api_address': self.api_address,
        }

    @property
    def json(self):
        return json.dumps(self.dict)


class Config(object):
    json_path = os.path.join(CRAWLAB_ROOT, 'config.json')
    data = Data()

    def __init__(self):
        if os.path.exists(self.json_path):
            self.load()
        else:
            self.save()

    def load(self):
        with open(self.json_path) as f:
            data_str = f.read()
            data = json.loads(data_str)
            self.data.username = data.get('username') or ''
            self.data.password = binascii.a2b_hex(data.get('password')).decode()
            self.data.token = data.get('token') or ''
            self.data.api_address = data.get('api_address') or ''

    def save(self):
        data = Data()
        data.username = self.data.username
        data.password = binascii.b2a_hex(self.data.password.encode()).decode()
        data.token = self.data.token
        data.api_address = self.data.api_address
        with open(self.json_path, 'wb') as f:
            f.write(data.json.encode())


config = Config()
