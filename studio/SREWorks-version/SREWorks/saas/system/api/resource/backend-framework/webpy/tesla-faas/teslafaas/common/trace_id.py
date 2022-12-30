#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'

import web
import struct
import random
import socket
import time
import uuid


def _ip2long(ip):
    packed_IP = socket.inet_aton(ip)
    return struct.unpack("!L", packed_IP)[0]


def _get_hex_str(value):
    return str(hex(value)).replace('0x', '')


def get_local_ip():
    myname = socket.getfqdn(socket.gethostname())
    local_ip = socket.gethostbyname(myname)
    return local_ip


def _gen_trace_id():
    return str(uuid.uuid4()).replace("-", "")


def set_request_id():
    """
    set request id
    """
    req_env = web.ctx.get('env', {})
    eagleeye_trace_id = req_env.get("HTTP_EAGLEEYE_TRACEID", None)
    trace_id = None
    if eagleeye_trace_id is None:
        trace_id = _gen_trace_id()
    else:
        trace_id = eagleeye_trace_id
    req_env['request_id'] = trace_id


def get_upstream_trace():
    """
    get trace_id
    """
    req_env = web.ctx.get('env', {})
    trace_id = req_env.get("request_id", None)
    return trace_id


def generate_trace_id(client_ip=''):
    time_section = _get_hex_str(long((time.time() * 1000000)))
    if client_ip:
        client_section = _get_hex_str(_ip2long(client_ip))
    else:
        client_section = get_local_ip()
    server_section = _get_hex_str(_ip2long(get_local_ip()))
    random_section = ('0000' + _get_hex_str(long(random.randint(0, 10000))))[
                     -5:]
    return "%s-%s-%s-%s" % (
    time_section, client_section, server_section, random_section)
