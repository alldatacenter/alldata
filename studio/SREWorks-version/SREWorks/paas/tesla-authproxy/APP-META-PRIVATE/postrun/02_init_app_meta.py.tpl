# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import copy
import os
import sys
import time
import logging
import json
import hashlib
import requests
import hashlib

logger = logging.getLogger()
logger.setLevel(logging.INFO)

handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('[authproxy] [%(asctime)s] [%(module)s.%(funcName)s:%(lineno)d] [%(levelname)s] - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)


def _get_passwd_hash(user_name, user_passwd):
    """
    构造 password hash
    :param user_name: 用户名
    :param user_passwd: 用户密码
    :return:
    """
    key = "%(user_name)s%(local_time)s%(passwd)s" % {
        'user_name': user_name,
        'local_time': time.strftime('%Y%m%d', time.localtime(time.time())),
        'passwd': user_passwd
    }
    m = hashlib.md5()
    m.update(key.encode('utf-8'))
    return m.hexdigest()


CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))
AUTHPROXY_ENDPOINT = "http://${ENDPOINT_PAAS_AUTHPROXY}"

AUTH_APP = '${ACCOUNT_SUPER_CLIENT_ID}'
AUTH_KEY = '${ACCOUNT_SUPER_CLIENT_SECRET}'
AUTH_USER = '${ACCOUNT_SUPER_ID}'
AUTH_USER_ID = '${ACCOUNT_SUPER_PK}'
AUTH_SECRET = '${ACCOUNT_SUPER_SECRET_KEY}'
AUTH_PASSWD = _get_passwd_hash(AUTH_USER, AUTH_SECRET)

ENV_TYPE = '${ENV_TYPE}'


def get_config():
    """
    获取配置内容
    :return:
    """
    with open(os.path.join(CURRENT_PATH, 'config/app-meta/%s.json' % ENV_TYPE)) as f:
        return json.loads(f.read())


def import_apps(config):
    """
    导入应用配置
    """
    for app in config.get('apps', []):
        request = copy.deepcopy(app)
        app_md5 = hashlib.md5()
        app_md5.update(request['appId'].encode('utf-8'))
        request['appAccessKey'] = request['appId'] + '-' + app_md5.hexdigest()
        response = requests.post(os.path.join(AUTHPROXY_ENDPOINT, 'auth/app/register'), headers={
            'X-Auth-App': AUTH_APP,
            'X-Auth-Key': AUTH_KEY,
            'X-Auth-User': AUTH_USER,
            'X-Auth-UserId': AUTH_USER_ID,
            'X-Auth-Passwd': AUTH_PASSWD,
        }, json=request)
        response_json = response.json()
        if response_json.get('code') != 200 \
                and 'app already' not in response_json.get('message', ''):
            raise ValueError('Tesla authproxy response code is not 200. request=%r, response=%s'
                             % (request, response.text))
        logger.info("Tesla authproxy app has updated, app=%s", json.dumps(request, ensure_ascii=False))


def import_service_meta(config):
    """
    导入服务原始数据
    :param config: 服务 meta 配置
    :return:
    """
    services = config.get('services', [])
    for app in config.get('apps', []):
        app_services = app.get('services', [])
        if app_services:
            services += app_services
    logger.info('Service meta list: %s', services)

    for service in services:
        request = copy.deepcopy(service)
        if 'permissions' in request:
            del request['permissions']
        request['owners'] = '${super_account_account}'
        response = requests.post(os.path.join(AUTHPROXY_ENDPOINT, 'service/meta/create'), headers={
            'X-Auth-App': AUTH_APP,
            'X-Auth-Key': AUTH_KEY,
            'X-Auth-User': AUTH_USER,
            'X-Auth-UserId': AUTH_USER_ID,
            'X-Auth-Passwd': AUTH_PASSWD,
        }, json=request)
        response_json = response.json()
        if response_json.get('code') != 200 \
                and 'The same serviceMetaDO already exists' not in response_json.get('message', ''):
            raise ValueError('Tesla authproxy response code is not 200. request=%r, response=%s'
                             % (request, response.text))
        logger.info("Tesla authproxy service meta has updated, app=%s", json.dumps(request, ensure_ascii=False))

        # 导入相关权限
        permissions = service.get('permissions', [])
        for permission in permissions:
            request = copy.deepcopy(permission)
            request['serviceCode'] = service['serviceCode']
            request['serviceName'] = service['serviceName']
            response = requests.post(os.path.join(AUTHPROXY_ENDPOINT, 'permission/meta/create'), headers={
                'X-Auth-User': '${super_account_account}',
                'X-Auth-UserId': '${super_account_account_id}',
            }, json=request)
            response_json = response.json()
            if response_json.get('code') != 200 \
                    and 'The same permission already exists' not in response_json.get('message', ''):
                raise ValueError('Tesla authproxy response code is not 200. request=%r, response=%s'
                                 % (request, response.text))
            logger.info("Tesla authproxy permission meta has updated, app=%s", json.dumps(request, ensure_ascii=False))


def enable_permissions(config):
    """
    启用相关权限
    :param config: 服务配置
    :return:
    """
    for app in config.get('apps', []):
        app_id = app['appId']
        services = app.get('services', [])
        if not services:
            services = config.get('services', [])
        for service in services:
            service_code = service['serviceCode']
            request = {
                'appId': app_id,
                'serviceCode': service_code,
                'permissionMetaIds': [],
            }
            response = requests.post(os.path.join(AUTHPROXY_ENDPOINT, 'permission/meta/enable'), headers={
                'X-Auth-App': AUTH_APP,
                'X-Auth-Key': AUTH_KEY,
                'X-Auth-User': AUTH_USER,
                'X-Auth-UserId': AUTH_USER_ID,
                'X-Auth-Passwd': AUTH_PASSWD,
                'X-EmpId': AUTH_USER,
            }, json=request)
            response_json = response.json()
            if response_json.get('code') != 200:
                raise ValueError('Tesla authproxy response code is not 200. request=%r, response=%s'
                                 % (request, response.text))
            logger.info("Tesla authproxy app permissions has updated, app=%s", json.dumps(request, ensure_ascii=False))


if __name__ == '__main__':
    conf = get_config()
    import_apps(conf)
    import_service_meta(conf)
    enable_permissions(conf)
