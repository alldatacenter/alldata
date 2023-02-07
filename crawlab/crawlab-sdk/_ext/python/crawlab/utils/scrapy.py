import configparser
import importlib
import os

import scrapy


def get_scrapy_cfg():
    cp = configparser.ConfigParser()
    cp.read('scrapy.cfg')
    return cp


def get_items_fields(items):
    data = []
    for key in [key for key in dir(items) if not key.startswith('__')]:
        cls = getattr(items, key)
        try:
            if isinstance(cls(), scrapy.Item):
                d = {
                    'name': key,
                    'fields': []
                }
                if hasattr(cls, 'fields'):
                    d['fields'] = list(cls.fields.keys())
                data.append(d)
        except:
            pass
    return data


def update_items_field(items_data):
    content = ''
    content += 'import scrapy\n\n'


def get_pipelines(pipelines):
    data = []
    for key in [key for key in dir(pipelines) if not key.startswith('__')]:
        cls = getattr(pipelines, key)
        try:
            if isinstance(cls(), object):
                data.append(key)
        except:
            pass

    return data


def get_spider_filepath(filenames, project_name, name):
    for filename in filenames:
        mod_name = filename.replace('.py', '')
        try:
            spider_file = importlib.import_module(f'{project_name}.spiders.{mod_name}')
        except:
            continue
        for key in dir(spider_file):
            try:
                cls = getattr(spider_file, key)
                instance = cls()
                if isinstance(instance, scrapy.Spider):
                    spider_name = getattr(instance, 'name')
                    if spider_name == name:
                        return os.path.join('.', project_name, 'spiders', filename)[1:]
            except:
                pass
