import importlib
import json
import os
import sys
from zipfile import ZipFile, ZIP_DEFLATED

import pathspec
from prettytable import PrettyTable

from legacy.core import CRAWLAB_TMP
from legacy.core.config import config
from legacy.core.request import Request
from crawlab.utils.scrapy import get_scrapy_cfg, get_items_fields, get_pipelines, get_spider_filepath

ignore_matches = None


def zip_dir(start_dir, target_path):
    start_dir = start_dir  # 要压缩的文件夹路径
    file_new = target_path  # 压缩后文件夹的名字

    z = ZipFile(file_new, 'w', ZIP_DEFLATED)
    for dir_path, dir_names, file_names in os.walk(start_dir):
        f_path = dir_path.replace(start_dir, '')  # 这一句很重要，不replace的话，就从根目录开始复制
        f_path = f_path and f_path + os.sep or ''  # 实现当前文件夹以及包含的所有文件的压缩
        for filename in file_names:
            if is_ignored(os.path.join(start_dir, dir_path, filename)):
                continue
            print(f_path + filename)
            z.write(os.path.join(dir_path, filename), f_path + filename)
    z.close()
    return file_new


def get_zip_file(input_path, result):
    files = os.listdir(input_path)
    for file in files:
        filepath = os.path.join(input_path, file)
        if os.path.isdir(filepath):
            get_zip_file(filepath, result)
        else:
            result.append(filepath)


def zip_file_path(input_path, target_path):
    f = ZipFile(target_path, 'w', ZIP_DEFLATED)
    file_list = []
    get_zip_file(input_path, file_list)
    for file in file_list:
        f.write(file)
    f.close()
    return target_path


def get_ignore_matches():
    global ignore_matches
    ignore_file = os.path.join(os.path.abspath(os.curdir), '.crawlabignore')
    if not os.path.exists(ignore_file):
        return None
    if ignore_matches is not None:
        return ignore_matches
    with open(ignore_file, 'r') as fh:
        spec = pathspec.PathSpec.from_lines('gitwildmatch', fh)
    ignore_matches = spec
    return ignore_matches


def is_ignored(file_name: str) -> bool:
    matches = get_ignore_matches()
    if matches is None:
        return False
    return matches.match_file(file_name)


class Client(object):
    def __init__(self):
        pass

    @staticmethod
    def list(columns, items):
        tb = PrettyTable()
        tb.field_names = columns
        for item in items:
            row = []
            for col in columns:
                row.append(item.get(col))
            tb.add_row(row)
        print(tb)
        print(f'total: {len(items)}')

    @staticmethod
    def update_token():
        data = Request.post('/login', data={
            'username': config.data.username,
            'password': config.data.password,
        })
        if data.get('error'):
            print('error: ' + data.get('error'))
            print('error when logging-in')
            return
        config.data.token = data.get('data')
        config.save()
        print('logged-in successfully')

    @staticmethod
    def list_nodes():
        data = Request.get('/nodes')
        if data.get('error'):
            print('error: ' + data.get('error'))
        items = data.get('data') or []
        columns = ['_id', 'name', 'status', 'create_ts', 'update_ts']
        Client.list(columns, items)

    @staticmethod
    def list_spiders():
        data = Request.get('/spiders', {'page_num': 1, 'page_size': 99999999})
        if data.get('error'):
            print('error: ' + data.get('error'))
        items = (data.get('data').get('list') or []) if data.get('data') is not None else []
        columns = ['_id', 'name', 'display_name', 'type', 'col', 'create_ts', 'update_ts']
        Client.list(columns, items)

    @staticmethod
    def list_schedules():
        data = Request.get('/schedules', {'page_num': 1, 'page_size': 99999999})
        if data.get('error'):
            print('error: ' + data.get('error'))
        items = data.get('data') or []
        columns = ['_id', 'name', 'spider_name', 'run_type', 'cron', 'create_ts', 'update_ts']
        Client.list(columns, items)

    @staticmethod
    def list_tasks(number=10):
        data = Request.get('/tasks', {'page_num': 1, 'page_size': number})
        if data.get('error'):
            print('error: ' + data.get('error'))
        items = data.get('data') or []
        columns = ['_id', 'status', 'node_name', 'spider_name', 'error', 'result_count', 'create_ts', 'update_ts']
        Client.list(columns, items)

    @staticmethod
    def upload_customized_spider(directory=None, name=None, col=None, display_name=None, command=None, id=None):
        # check if directory exists
        if not os.path.exists(directory):
            print(f'error: {directory} does not exist')
            return

        # compress the directory to the zipfile
        target_path = os.path.join(CRAWLAB_TMP, name or os.path.basename(directory)) + '.zip'
        zip_dir(directory, target_path)

        # upload zip file to server
        if id is None:
            res = Request.upload('/spiders', target_path, data={
                'name': name,
                'display_name': display_name,
                'col': col,
                'command': command,
            })
            if res.get('error'):
                print('error: ' + res.get('error'))
                return
        else:
            res = Request.upload(f'/spiders/{id}/upload', target_path)
            if res.get('error'):
                print('error: ' + res.get('error'))
                return
        print('uploaded successfully')

    @staticmethod
    def upload_configurable_spider(directory=None, name=None, col=None, display_name=None, command=None, id=None):
        pass

    @staticmethod
    def settings(directory=None):
        if directory is None:
            directory = os.path.abspath(os.curdir)
        os.chdir(directory)

        cp = get_scrapy_cfg()

        settings_mod_name = cp.get('settings', 'default')

        sys.path.insert(0, directory)
        settings = importlib.import_module(settings_mod_name)

        data = []
        for key in [key for key in dir(settings) if not key.startswith('__')]:
            data.append({
                'key': key,
                'value': getattr(settings, key),
            })

        print(json.dumps(data))

    @staticmethod
    def items(directory=None):
        if directory is None:
            directory = os.path.abspath(os.curdir)
        os.chdir(directory)

        cp = get_scrapy_cfg()

        settings_mod_name = cp.get('settings', 'default')
        project_name = settings_mod_name.split('.')[0]
        items_mod_name = f'{project_name}.items'

        sys.path.insert(0, directory)
        items = importlib.import_module(items_mod_name)

        # get list of all items fields
        data = get_items_fields(items)
        print(json.dumps(data))

    @staticmethod
    def pipelines(directory=None, name=None, delete=None):
        if directory is None:
            directory = os.path.abspath(os.curdir)
        os.chdir(directory)

        cp = get_scrapy_cfg()

        settings_mod_name = cp.get('settings', 'default')
        project_name = settings_mod_name.split('.')[0]
        pipelines_mod_name = f'{project_name}.pipelines'

        sys.path.insert(0, directory)
        pipelines = importlib.import_module(pipelines_mod_name)

        # get list of all pipelines
        if name is None:
            data = get_pipelines(pipelines)
            print(json.dumps(data))

    @staticmethod
    def find_spider_filepath(directory=None, name=None):
        if directory is None:
            directory = os.path.abspath(os.curdir)
        os.chdir(directory)
        sys.path.insert(0, directory)

        cp = get_scrapy_cfg()

        settings_mod_name = cp.get('settings', 'default')
        project_name = settings_mod_name.split('.')[0]

        spiders_path = os.path.join(directory, project_name, 'spiders')
        filenames = os.listdir(spiders_path)

        filepath = get_spider_filepath(filenames, project_name, name)
        print(filepath)


client = Client()
