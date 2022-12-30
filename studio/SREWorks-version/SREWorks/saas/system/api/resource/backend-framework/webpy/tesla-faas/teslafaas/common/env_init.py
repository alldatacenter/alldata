#!/usr/bin/env python
# encoding: utf-8
""" """
import os
import json
import logging
import logging.config
from logging.config import dictConfig

import errno

__author__ = 'adonis'


def logging_config_wrapper(*args, **kwargs):
    logging.info("Duplicated logging init")


def disable_duplicated_logger():
    # disable any other config
    logging.config.dictConfig = logging_config_wrapper
    logging.config.fileConfig = logging_config_wrapper


def init_logger(name, is_debug=True):
    """
    init logger
    """
    here = os.path.abspath(os.path.dirname(__file__))
    log_config_path = os.path.join(here, '../logging.json')
    with open(log_config_path) as f:
        try:
            log_config = json.loads(f.read())
        except ValueError:
            raise ValueError('Invalid logging config, cannot loads to JSON')
        # support home directory ~
        for handler_item in log_config.get('handlers', {}).values():
            if 'filename' not in handler_item:
                continue
            handler_item['filename'] = os.path.expanduser("~/logs/%s/application.log" % name)
            if is_debug:
                handler_item['level'] = 'DEBUG'
            try:
                os.makedirs(os.path.dirname(handler_item['filename']))
            except OSError, e:
                # be happy if someone already created the path
                if e.errno != errno.EEXIST:
                    raise
        dictConfig(log_config)
    # disable other
    disable_duplicated_logger()
    logging.info("start to load %s, my pid is %s" % (name, os.getpid()))

