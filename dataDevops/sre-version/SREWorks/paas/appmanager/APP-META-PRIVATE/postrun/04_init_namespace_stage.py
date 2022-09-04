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
NAMESPACE = 'apsara-bigdata-manager'
STAGE = 'prod'


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


def apply(r, url, post_body):
    """
    导入 post_body 对应 URL
    :return:
    """
    response = r.post(ENDPOINT + url, json=post_body)
    response_json = response.json()
    code = response_json.get('code')
    if code != 200 and code != 10002:
        logger.error('import to appmanager failed, url=%s, body=%s, response=%s' % (url, post_body, response.text))
        sys.exit(1)
    logger.info('import to appmanager success, url=%s, body=%s' % (url, post_body))


def apply_all():
    try:
        r = AppManagerClient(ENDPOINT, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD).client
    except Exception as e:
        logger.error("cannot find appmanager client auth info, skip")
        r = requests

    apply(r, '/namespaces', {
        'namespaceId': 'apsara-bigdata-manager',
        'namespaceName': '服务专用(apsara-bigdata-manager)',
        'namespaceExt': {}
    })
    apply(r, '/namespaces', {
        'namespaceId': 'default',
        'namespaceName': '前端配置专用(default)',
        'namespaceExt': {}
    })
    apply(r, '/namespaces/apsara-bigdata-manager/stages', {
        'stageId': 'master',
        'stageName': '服务专用(master)',
        'stageExt': {}
    })
    apply(r, '/namespaces/apsara-bigdata-manager/stages', {
        'stageId': 'slave',
        'stageName': '服务专用(slave)',
        'stageExt': {}
    })
    apply(r, '/namespaces/default/stages', {
        'stageId': 'prod',
        'stageName': '前端配置专用',
        'stageExt': {}
    })


if __name__ == '__main__':
    if os.getenv("SREWORKS_INIT") != "enable":
        apply_all()
