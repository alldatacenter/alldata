# coding: utf-8

import os
import requests

from oauthlib.oauth2 import LegacyApplicationClient
from requests_oauthlib import OAuth2Session

CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))
ENDPOINT = 'http://sreworks-appmanager'

CLIENT_ID = os.environ.get('APPMANAGER_CLIENT_ID')
CLIENT_SECRET = os.environ.get('APPMANAGER_CLIENT_SECRET')
USERNAME = os.environ.get('APPMANAGER_USERNAME')
PASSWORD = os.environ.get('APPMANAGER_PASSWORD')

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
        oauth = OAuth2Session(client=LegacyApplicationClient(client_id=CLIENT_ID))
        return oauth.fetch_token(
            token_url=os.path.join(ENDPOINT, 'oauth/token'),
            username=self._username,
            password=self._password,
            client_id=self._client_id,
            client_secret=self._client_secret
        )



client = AppManagerClient(ENDPOINT, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD).client

response = client.get(ENDPOINT + '/market/apps', params={"page":1, "pageSize":9999999})
response_json = response.json()
demoAppInfo = None
for item in response_json.get("data")["items"]:
    if item.get("appId") == "sreworks1":
      demoAppInfo = item
      break

if demoAppInfo is None:
    print("NO")
else:
    print("YES")

