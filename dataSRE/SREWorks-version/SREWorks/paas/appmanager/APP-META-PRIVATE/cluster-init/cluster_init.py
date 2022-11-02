# -*- coding:utf-8 -*-

import logging
import os
import sys

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
CREATOR = '999999999999'
HEADERS = {
    'X-EmpId': CREATOR
}

def generateKubeconfig(apiServer, token):

    return {
        "apiVersion": "v1",
        "clusters": [
            {
                "cluster": {
                    "insecure-skip-tls-verify": True,
                    "server": apiServer
                },
                "name": "sreworks_cluster"
            }
        ],
        "contexts": [
            {
                "context": {
                    "cluster": "sreworks_cluster",
                    "user": "sreworks_admin"
                },
                "name": "t"
            }
        ],
        "current-context": "t",
        "kind": "Config",
        "preferences": {},
        "users": [
            {
                "name": "sreworks_admin",
                "user": {
                    "token": token,
                }
            }
        ]
    }


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


def _insert_cluster(r, cluster, master_url, oauth_token):
    """
    插入集群
    """
    response = r.post("%s/clusters" % ENDPOINT, headers=HEADERS, json={
        'clusterId': cluster,
        'clusterName': cluster,
        'clusterType': 'kubernetes',
        'clusterConfig': {"kube": generateKubeconfig(master_url, oauth_token)},
        'masterFlag': True if cluster == 'master' else False,
    })
    if response.json().get('code') == 200:
        logger.info('cluster has inserted||cluster=%s||master_url=%s||oauth_token=%s'
                    % (cluster, master_url, oauth_token))
    else:
        logger.error('cannot insert cluster info, abort||cluster=%s||master_url=%s||oauth_token=%s||response=%s'
                     % (cluster, master_url, oauth_token, response.text))
        sys.exit(1)


def _update_cluster(r, cluster, master_url, oauth_token):
    """
    更新集群
    """
    response = r.put("%s/clusters/%s" % (ENDPOINT, cluster), headers=HEADERS, json={
        'clusterName': cluster,
        'clusterType': 'kubernetes',
        'clusterConfig': {"kube": generateKubeconfig(master_url, oauth_token)},
        'masterFlag': True if cluster == 'master' else False,
    })
    if response.json().get('code') == 200:
        logger.info('cluster has updated||cluster=%s||master_url=%s||oauth_token=%s'
                    % (cluster, master_url, oauth_token))
    else:
        logger.error('cannot update cluster info, abort||cluster=%s||master_url=%s||oauth_token=%s||response=%s'
                     % (cluster, master_url, oauth_token, response.text))
        sys.exit(1)


def init_cluster(r):
    """
    初始化 appmanager 集群
    :return:
    """
    cluster_mapping = {}
    items = r.get("%s/clusters" % ENDPOINT, headers=HEADERS).json().get('data', {}).get('items', [])
    for item in items:
        cluster_mapping[item['clusterId']] = {
            'masterUrl': item.get('clusterConfig', {}).get("kube", {}).get('clusters',[{}])[0].get('cluster', {}).get('server'),
            'oauthToken': item.get('clusterConfig', {}).get("kube", {}).get('users',[{}])[0].get('user', {}).get('token'),
        }

    # 获取当前的 masterUrl 和 oauthToken 信息
    master_url = 'https://%s:%s' % (os.getenv('KUBERNETES_SERVICE_HOST'), os.getenv('KUBERNETES_SERVICE_PORT'))
    with open('/run/secrets/kubernetes.io/serviceaccount/token') as f:
        oauth_token = f.read()

    # 根据实际情况进行 cluster 更新
    clusters = ["master"]
    for cluster in clusters:
        if not cluster_mapping.get(cluster):
            _insert_cluster(r, cluster, master_url, oauth_token)
        elif cluster_mapping[cluster].get('masterUrl') != master_url \
                or cluster_mapping[cluster].get('oauthToken') != oauth_token:
            _update_cluster(r, cluster, master_url, oauth_token)
        else:
            logger.info('no need to update cluster %s' % cluster)


if __name__ == "__main__":
    init_cluster(AppManagerClient(ENDPOINT, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD).client)
    logger.info("cluster initjob success")
