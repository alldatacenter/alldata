import json
import os
import re

from print_color import print as print_color
from requests import Response

from crawlab.client import http_put, http_post, http_get
from crawlab.config import get_spider_config
from crawlab.constants.upload import CLI_DEFAULT_UPLOAD_SPIDER_MODE, CLI_DEFAULT_UPLOAD_SPIDER_CMD, \
    CLI_DEFAULT_UPLOAD_IGNORE_PATTERNS
from crawlab.errors.upload import MissingIdException, HttpException


def create_spider(name: str, description: str = None, mode: str = None, priority: int = None, cmd: str = None,
                  param: str = None, col_name: str = None) -> str:
    # results collection name
    if col_name is None:
        col_name = f'results_{"_".join(name.lower().split(" "))}'

    # mode
    if mode is None:
        mode = CLI_DEFAULT_UPLOAD_SPIDER_MODE

    # cmd
    if cmd is None:
        cmd = CLI_DEFAULT_UPLOAD_SPIDER_CMD

    # http post
    res = http_post(url='/spiders', data={
        'name': name,
        'description': description,
        'mode': mode,
        'priority': priority,
        'cmd': cmd,
        'param': param,
        'col_name': col_name,
    })

    return res.json().get('data').get('_id')


def exists_spider_by_name(name: str) -> bool:
    try:
        res = http_get(url=f'/spiders', params={'conditions': json.dumps([{'key': 'name', 'op': 'eq', 'value': name}])})
        if not isinstance(res, Response) or res.status_code != 200:
            return False
        data = res.json().get('data')
        if data is None or len(data) == 0:
            return False
        return True
    except:
        return False


def upload_file(_id: str, file_path: str, target_path: str):
    if _id is None:
        raise MissingIdException("Missing ID")

    with open(file_path, 'rb') as f:
        data = {
            'path': target_path,
        }
        files = {'file': f}

        url = f'/spiders/{_id}/files/save'
        http_post(url=url, data=data, files=files, headers={})


def upload_dir(dir_path: str, create: bool = True, spider_id: str = None, name=None, description=None, mode=None,
               priority=None, cmd=None, param=None, col_name=None, exclude_path: list = None):
    # spider config
    cfg = get_spider_config(dir_path)
    # variables
    if name is None:
        name = cfg.name
    if description is None:
        description = cfg.description
    if mode is None:
        mode = cfg.mode
    if priority is None:
        priority = cfg.priority
    if cmd is None:
        cmd = cfg.cmd
    if param is None:
        param = cfg.param
    if col_name is None:
        col_name = cfg.col_name
    if exclude_path is None:
        exclude_path = cfg.exclude_path
    # create spider
    if create:
        try:
            spider_id = create_spider(name=name, description=description, mode=mode, priority=priority, cmd=cmd,
                                      param=param, col_name=col_name)
            print_color(f'created spider {name} (id: {spider_id})', tag='success', tag_color='green', color='white')
        except HttpException:
            print_color(f'create spider {name} failed', tag='error', tag_color='red', color='white')
            return

    # stats
    stats = {
        'success': 0,
        'error': 0,
    }

    # iterate all files in the directory
    for root, dirs, files in os.walk(dir_path):
        for file_name in files:
            # file path
            file_path = os.path.join(root, file_name)

            # ignored file
            if is_ignored(file_path, exclude_path):
                continue

            # target path
            target_path = file_path.replace(dir_path, '')

            # upload file
            try:
                upload_file(spider_id, file_path, target_path)
                print_color(f'uploaded {file_path}', tag='success', tag_color='green', color='white')
                stats['success'] += 1

            except HttpException:
                print_color(f'failed to upload {file_path}', tag='error', tag_color='red', color='white')
                stats['error'] += 1

    # logging
    print_color(f'uploaded spider {name}', tag='success', tag_color='green', color='white')
    print_color(f'success: {stats["success"]}', tag='info', tag_color='cyan', color='white')
    print_color(f'failed: {stats["error"]}', tag='info', tag_color='cyan', color='white')


def is_ignored(file_path: str, exclude_path_patterns: list = None) -> bool:
    exclude_path_patterns = exclude_path_patterns or []
    ignore_patterns = exclude_path_patterns + CLI_DEFAULT_UPLOAD_IGNORE_PATTERNS
    for pat in ignore_patterns:
        if re.search(pat, file_path) is not None:
            return True
    return False
