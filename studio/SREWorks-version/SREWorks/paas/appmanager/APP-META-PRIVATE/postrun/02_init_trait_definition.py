# coding: utf-8

import json
import logging
import os
import sys

import requests
from oauthlib.oauth2 import LegacyApplicationClient
from requests_oauthlib import OAuth2Session

logger = logging.getLogger()
logger.setLevel(logging.INFO)

handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('[%(asctime)s] [%(module)s.%(funcName)s:%(lineno)d] [%(levelname)s] - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))
ENDPOINT = 'http://' + os.getenv('ENDPOINT_PAAS_APPMANAGER')
CLIENT_ID = os.getenv('APPMANAGER_CLIENT_ID')
CLIENT_SECRET = os.getenv('APPMANAGER_CLIENT_SECRET')
USERNAME = os.getenv('APPMANAGER_ACCESS_ID')
PASSWORD = os.getenv('APPMANAGER_ACCESS_SECRET')


class AppManagerClient(object):

    def __init__(self, endpoint, client_id, client_secret, username, password):
        os.environ.setdefault('OAUTHLIB_INSECURE_TRANSPORT', '1')
        self._endpoint = endpoint
        self._client_id = client_id
        self._client_secret = client_secret
        self._username = username
        self._password = password
        self._token = self._fetch_token()

    @property
    def client(self):
        return OAuth2Session(self._client_id, token=self._token)

    def _fetch_token(self):
        """
        获取 appmanager access token
        """
        oauth = OAuth2Session(client=LegacyApplicationClient(client_id=CLIENT_ID))
        return oauth.fetch_token(
            token_url=os.path.join(ENDPOINT, 'oauth/token'),
            username=self._username,
            password=self._password,
            client_id=self._client_id,
            client_secret=self._client_secret
        )


def apply(r, post_body):
    """
    导入 post_body 对应的 trait 数据
    :return:
    """
    response = r.post(ENDPOINT + '/traits', data=post_body)
    response_json = response.json()
    if response_json.get('code') != 200:
        logger.error('import trait to appmanager failed, response=%s' % response.text)
        sys.exit(1)
    logger.info('import trait to appmanager success, trait=%s' % post_body)


def apply_all_traits():
    try:
        r = AppManagerClient(ENDPOINT, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD).client
    except Exception as e:
        logger.error("cannot find appmanager client auth info, skip")
        r = requests

    # 读取所有配置
    config_map = {}
    path = os.path.join(CURRENT_PATH, 'traits')
    for root, dirs, files in os.walk(path, topdown=False):
        for name in files:
            if not name.endswith('.yaml'):
                continue
            config_map[name.split('.')[0]] = open(os.path.join(root, name)).read()

    for name in config_map:
        post_body = config_map[name]
        apply(r, post_body)


if __name__ == '__main__':
    apply_all_traits()
