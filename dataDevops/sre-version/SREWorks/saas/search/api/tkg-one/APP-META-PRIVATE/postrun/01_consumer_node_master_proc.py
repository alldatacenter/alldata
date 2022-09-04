#! -*- coding: utf-8 -*-
# @author: xueyong.zxy
# @date: 2020年02月24日18:07:55

import sys
import requests
import socket
import time
import os
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('[tkgone] [%(asctime)s] [%(module)s.%(funcName)s:%(lineno)d] [%(levelname)s] - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

IDC_MAP = os.getenv('IDC_MAP')
CONTAINER_IP_LIST = os.getenv('CONTAINER_IP_LIST_TKGONE', '').split(',')
ENDPOINT_PAAS_TKGONE = os.getenv('ENDPOINT_PAAS_TKGONE')
BCC_PORTAL_DOMAIN = os.getenv('DNS_PAAS_HOME_DISASTER')
BCC_PORTAL_VIP = os.getenv('VIP_IP_PAAS_HOME_DISASTER')
URL = 'http://' + ENDPOINT_PAAS_TKGONE + '/consumer/node/setEnableByHost/{host}/{enable}'
HEDAERS = {'Content-Type': 'application/json'}


def send_request(url):
    resp = requests.put(url, headers=HEDAERS)
    logger.info('enable node url is %s, resp is %s' % (url, resp.text))
    if resp.json().get('code') != 200:
        logger.error('response is not ok, please check details')
        sys.exit(1)


def enable_node():
    for ip in CONTAINER_IP_LIST:
        kwargs = {'host': ip, 'enable': 'true'}
        send_request(URL.format(**kwargs))


if __name__ == '__main__':
    time.sleep(20)
    enable_node()
