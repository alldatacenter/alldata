import json
import os.path
from typing import Dict

import yaml

SPIDER_CONFIG_FILE_NAME = 'crawlab'


class SpiderConfig(dict):
    def __init__(self, value: Dict = None):
        super().__init__(value or {})

    @property
    def name(self):
        return self.get('name')

    @property
    def description(self):
        return self.get('description')

    @property
    def mode(self):
        return self.get('mode')

    @property
    def priority(self):
        return self.get('priority')

    @property
    def cmd(self):
        return self.get('cmd')

    @property
    def param(self):
        return self.get('param')

    @property
    def col_name(self):
        return self.get('col_name')

    @property
    def exclude_path(self):
        return self.get('exclude_path')


def get_spider_config(dir_path: str = None) -> SpiderConfig:
    # directory path
    if dir_path is None:
        dir_path = os.path.abspath('.')
    dir_name = os.path.basename(dir_path)

    # crawlab.json
    file_path_json = os.path.join(dir_path, SPIDER_CONFIG_FILE_NAME + '.json')
    if os.path.exists(file_path_json):
        return _get_spider_config_json(file_path_json)

    # crawlab.yml / crawlab.yaml
    file_path_yml = os.path.join(dir_path, SPIDER_CONFIG_FILE_NAME + '.yml')
    if os.path.exists(file_path_yml):
        return _get_spider_config_yaml(file_path_yml)
    file_path_yaml = os.path.join(dir_path, SPIDER_CONFIG_FILE_NAME + '.yaml')
    if os.path.exists(file_path_yaml):
        return _get_spider_config_yaml(file_path_yaml)

    return SpiderConfig({
        'name': dir_name,
    })


def _get_spider_config_json(file_path: str) -> SpiderConfig:
    with open(file_path) as f:
        data = json.loads(f.read())
        return SpiderConfig(data)


def _get_spider_config_yaml(file_path: str) -> SpiderConfig:
    with open(file_path) as f:
        data = yaml.full_load(f)
        return SpiderConfig(data)
