import os
import shutil
import tempfile
import unittest

from crawlab.config import get_spider_config

os.chdir(tempfile.gettempdir())


class SpiderConfigTestCase(unittest.TestCase):
    _crawlab_json = '''
{"name": "test_spider", "col_name": "test_spider_results", "cmd": "echo hello"}
'''
    _crawlab_yaml = '''
name: test_spider
col_name: test_spider_results
cmd: 'echo hello'
'''

    def test_get_spider_config_json(self):
        with open('crawlab.json', 'w') as f:
            f.write(self._crawlab_json)
        cfg = get_spider_config()
        assert cfg.name == 'test_spider'
        assert cfg.col_name == 'test_spider_results'
        assert cfg.cmd == 'echo hello'
        os.remove('crawlab.json')

    def test_get_spider_config_yaml(self):
        for file_name in ['crawlab.yml', 'crawlab.yaml']:
            with open(file_name, 'w') as f:
                f.write(self._crawlab_yaml)
            cfg = get_spider_config()
            assert cfg.name == 'test_spider'
            assert cfg.col_name == 'test_spider_results'
            assert cfg.cmd == 'echo hello'
            os.remove(file_name)


if __name__ == '__main__':
    unittest.main()
