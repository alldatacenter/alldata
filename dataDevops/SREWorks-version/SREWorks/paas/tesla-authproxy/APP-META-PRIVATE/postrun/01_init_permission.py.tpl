# coding: utf-8

import os
import time
import logging
import json
import requests
import traceback
import sys
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
AUTHPROXY_IMPORT_URL = "http://${ENDPOINT_PAAS_AUTHPROXY}/permission/import"

AUTH_APP = '${ACCOUNT_SUPER_CLIENT_ID}'
AUTH_KEY = '${ACCOUNT_SUPER_CLIENT_SECRET}'
AUTH_USER = '${ACCOUNT_SUPER_ID}'
AUTH_USER_ID = '${ACCOUNT_SUPER_PK}'
AUTH_SECRET = '${ACCOUNT_SUPER_SECRET_KEY}'
AUTH_PASSWD = _get_passwd_hash(AUTH_USER, AUTH_SECRET)


def import_permissions():
    """
    导入权限配置
    """
    with open(os.path.join(CURRENT_PATH, 'config/permission-meta.json')) as f:
        config = json.loads(f.read())
    response = requests.post(AUTHPROXY_IMPORT_URL, headers={
        'X-Auth-App': AUTH_APP,
        'X-Auth-Key': AUTH_KEY,
        'X-Auth-User': AUTH_USER,
        'X-Auth-UserId': AUTH_USER_ID,
        'X-Auth-Passwd': AUTH_PASSWD,
    }, json=config)
    response_json = response.json()
    if response_json.get('code') != 200:
        raise ValueError('Tesla authproxy response code is not 200. response=%s' % response.text)
    logger.info("Tesla authproxy permissions has updated, config=%s", json.dumps(config, ensure_ascii=False))


if __name__ == '__main__':
    time.sleep(300)
    import_permissions()
