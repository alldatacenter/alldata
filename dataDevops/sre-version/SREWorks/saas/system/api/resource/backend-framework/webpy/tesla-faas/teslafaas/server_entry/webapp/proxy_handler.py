#!/usr/bin/env python
# encoding: utf-8
""" """
from common.decorators import exception_wrapper

__author__ = 'adonis'

import re
import httplib
import urllib
import web
import requests
import requests_unixsocket
import logging

from teslafaas.common.const import ALL_HEADERS, NON_WSGI_HEADER
from teslafaas.common.util_func import get_cur_time_ms



requests_unixsocket.monkeypatch()


log = logging.getLogger(__name__)


class NoContainerPath(Exception):
    pass


# TODO: multipart http request support
class ProxyHandler(object):
    US_URL_PREFIX = u'http+unix://%2Ftmp%2Ftesla-faas%2F'
    US_URL_SOCKET = u'container-%s.socket'
    # GW_PATTERN = '^/v2/apps/(?P<app_name>.*?)/faas/(?P<service_name>.*?)/(?P<service_url>.*)$'
    # GW_EMPTY_SERVICE_PATTERN = '^/v2/apps/(?P<app_name>.*?)/faas/(?P<service_name>.*?)[/]?$'
    GW_PATTERN = '/v2/apps/(?P<app_name>.*?)/faas/(?P<service_name>.*?)/(?P<service_url>.*)$'
    GW_EMPTY_SERVICE_PATTERN = '/v2/apps/(?P<app_name>.*?)/faas/(?P<service_name>.*?)[/]?$'

    @classmethod
    def parse_url(cls):
        url_path = web.ctx.path
        if url_path in ('/', ''):
            raise NoContainerPath()
        paths = url_path.split('/')
        if len(paths) < 2:
            raise NoContainerPath()
        if re.search("/v2/apps/", web.ctx.path):
            return cls.parse_gateway_url(url_path)
        else:
            c_name = paths[1]
            if url_path.startswith('/%s/' % c_name):
                target_uri = url_path.replace('/%s/' % c_name, "")
            else:
                target_uri = url_path.replace('/%s' % c_name, "")
            return c_name, target_uri

    @classmethod
    def parse_gateway_url(cls, gw_url):
        m = re.search(cls.GW_PATTERN, gw_url)
        if m is None:
            m = re.search(cls.GW_EMPTY_SERVICE_PATTERN, gw_url)
            if m is None:
                raise Exception('invalid gateway url path: %s' % gw_url)
        res = m.groupdict()
        return res['service_name'], res.get('service_url', '')

    @classmethod
    def get_container_url_path(cls):
        container_name, target_uri = cls.parse_url()
        socket_name = ProxyHandler.US_URL_SOCKET % container_name
        socket_name = urllib.quote(socket_name)
        res = u"%s%s/%s" % (ProxyHandler.US_URL_PREFIX, socket_name, target_uri)
        return res

    @staticmethod
    def get_proxy_headers():
        headers = {}
        for x in ALL_HEADERS:
            if x == 'Content-Length':
                continue
            wsgi_header = ("HTTP_%s" % x).upper().replace('-', '_')
            if x in NON_WSGI_HEADER:
                wsgi_header = x.upper().replace('-', '_')
            header_value = web.ctx.env.get(wsgi_header, '')
            if header_value:
                headers[x] = header_value
        return headers

    @staticmethod
    def cors_wl_process():
        # origin header exist only in cross site request
        origin = web.ctx.env.get('HTTP_ORIGIN', '')
        # if origin:
        #     web.header('Access-Control-Allow-Origin', origin)
        #     web.header('Access-Control-Allow-Credentials', 'true')
        #     web.header("Access-Control-Allow-Methods",
        #                "GET,POST,OPTIONS,PUT,DELETE")
        #     web.header("Access-Control-Allow-Headers",
        #                "Content-Type,Accept,X-Auth-User,X-User-Info,"
        #                "Product-Name,X-File,X-Product,X-Cluster, "
        #                "X-Upload-Path")

    def debug_info(self, start=True):
        debug_info = "%s: %s"
        url_path = "%s%s" % (web.ctx.home, web.ctx.fullpath)
        if start:
            title = "START"
            detail_fields = (web.ctx.method, url_path)
        else:
            title = "END"
            delay_ms = get_cur_time_ms() - self._start_time
            delay_ms_str = "%sms" % delay_ms
            detail_fields = (web.ctx.method, url_path, web.ctx.status, delay_ms_str)
        debug_info = debug_info % (title, "  ".join(detail_fields))
        return debug_info

    def proxy(self, http_method):
        # with requests_unixsocket.monkeypatch():
        self._start_time = get_cur_time_ms()
        log.info(self.debug_info(start=True))
        res = self._proxy(http_method)
        log.info(self.debug_info(start=False))
        return res

    def _proxy(self, http_method):
        try:
            target = self.get_container_url_path()
            log.info('target container url path: %s', target)
            http_method = http_method.lower()
            req_func = getattr(requests, http_method)
            req_args = dict(
                verify=False,
                data=web.data(),
                params=web.input(),
                timeout=60,
                headers=self.get_proxy_headers(),
            )
            if not self.get_proxy_headers().has_key('HTTP_X_EMPID'):
                raise requests.RequestException
            response = req_func(target, **req_args)
        except requests.RequestException as e:
            raise Exception("can not connect to container/service: %s. %s"
                            % (self.parse_url(), e))
        status_code = response.status_code
        # error processing
        if 200 <= status_code < 300:
            pass
        elif 400 <= status_code < 500:
            pass
        else:
            pass
        # set status line
        web.ctx.status = "%s %s" % (
            status_code, httplib.responses.get(status_code, ""))
        # set headers
        for x in ALL_HEADERS:
            value = response.headers.get(x, None)
            if value:
                web.header(x, value)
        # set body
        return response.content

    @exception_wrapper
    def GET(self):
        try:
            self.get_container_url_path()
        except NoContainerPath:
            return "success"
            # return json.dumps(dict(code=200, message='Tesla FaaS Server', data=[]))
        return self.proxy('GET')

    @exception_wrapper
    def POST(self):
        return self.proxy('POST')

    @exception_wrapper
    def PUT(self):
        return self.proxy('PUT')

    @exception_wrapper
    def DELETE(self):
        return self.proxy('DELETE')
