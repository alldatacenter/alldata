import os
import unittest
from argparse import Namespace

from crawlab.cli.actions.login import cli_login
from crawlab.client import get_api_address
from crawlab.config.config import config


class CliActionLoginTestCase(unittest.TestCase):
    @staticmethod
    def test_login():
        args = Namespace(
            username='admin',
            password='admin',
            api_address=get_api_address(),
        )
        cli_login(args)
        assert os.path.exists(config.json_path)
        assert len(config.data.get('token')) > 0


if __name__ == '__main__':
    unittest.main()
