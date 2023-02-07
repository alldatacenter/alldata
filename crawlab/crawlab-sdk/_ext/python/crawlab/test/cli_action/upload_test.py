import os
import tempfile
import unittest
from argparse import Namespace
from datetime import datetime

import requests

from crawlab.cli.actions.login import cli_login
from crawlab.cli.actions.upload import cli_upload
from crawlab.client import get_api_address
from crawlab.config.config import config


class CliActionUploadTestCase(unittest.TestCase):
    endpoint = get_api_address()

    @staticmethod
    def _setup():
        name = 'test_spider' + f'_{int(datetime.now().timestamp())}'
        dir_path = os.path.join(tempfile.gettempdir(), name)
        if not os.path.exists(dir_path):
            os.makedirs(dir_path)
        venv_dir = os.path.join(dir_path, '.venv')
        if not os.path.exists(venv_dir):
            os.makedirs(venv_dir)
        with open(os.path.join(venv_dir, 'pyvenv.cfg'), 'w') as f:
            f.write('virtualenv = 20.13.0')
        os.chdir(dir_path)
        return name, dir_path

    def test_upload(self):
        name, dir_path = self._setup()
        with open(os.path.join(dir_path, 'main.py'), 'w') as f:
            f.write('print(\'hello world\')')
        description = 'test_description_' + f'_{int(datetime.now().timestamp())}'
        mode = 'random'
        priority = 1
        cmd = 'echo hello'
        param = 'test'
        cli_login(Namespace(
            username='admin',
            password='admin',
            api_address=self.endpoint,
        ))
        cli_upload(Namespace(
            id=None,
            dir=None,
            name=name,
            description=description,
            mode=mode,
            priority=priority,
            cmd=cmd,
            param=param,
            col_name=None,
            create=True,
            exclude_path='.venv'
        ))

        res = requests.get(f'{self.endpoint}/spiders', headers={'Authorization': config.data.get("token")},
                           params={'size': 1, 'page': 1, 'sort': '[]'})
        assert res.status_code == 200
        data = res.json().get('data')
        assert len(data) == 1
        spider = data[0]
        assert spider.get('name') == name
        assert spider.get('description') == description
        assert spider.get('mode') == mode
        assert spider.get('cmd') == cmd
        assert spider.get('param') == param
        requests.delete(f'{self.endpoint}/spiders/{spider.get("_id")}',
                        headers={'Authorization': config.data.get("token")})

    def test_upload_with_crawlab_json(self):
        name, dir_path = self._setup()
        description = 'test_description_' + f'_{int(datetime.now().timestamp())}'
        mode = 'all-nodes'
        priority = 1
        cmd = 'echo hello'
        param = 'test'
        with open(os.path.join(dir_path, 'main.py'), 'w') as f:
            f.write('print(\'hello world\')')
        with open(os.path.join(dir_path, 'crawlab.json'), 'w') as f:
            f.write('{"name":"%s","description":"%s","mode":"%s","priority":%d,"cmd":"%s","param":"%s"}' % (
                name, description, mode, priority, cmd, param))
        cli_login(Namespace(
            username='admin',
            password='admin',
            api_address=self.endpoint,
        ))
        cli_upload(Namespace(
            id=None,
            dir=None,
            name=name,
            description=None,
            mode=None,
            priority=None,
            cmd=None,
            param=None,
            col_name=None,
            create=True,
            exclude_path=None
        ))
        res = requests.get(f'{self.endpoint}/spiders', headers={'Authorization': config.data.get("token")},
                           params={'size': 1, 'page': 1, 'sort': '[]'})
        assert res.status_code == 200
        data = res.json().get('data')
        spider = data[0]
        assert spider.get('name') == name
        assert spider.get('description') == description
        assert spider.get('mode') == mode
        assert spider.get('cmd') == cmd
        assert spider.get('param') == param
        requests.delete(f'{self.endpoint}/spiders/{spider.get("_id")}',
                        headers={'Authorization': config.data.get("token")})


if __name__ == '__main__':
    unittest.main()
