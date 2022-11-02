#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from flask import request
import struct
import random
import socket
import time


def _ip2long(ip):
    return struct.unpack("!L", socket.inet_aton(ip))[0]


def _get_hex_str(value):
    return str(hex(value)).replace('0x', '')


def get_local_ip():
    return socket.gethostbyname(socket.getfqdn(socket.gethostname()))


def get_upstream_trace():
    trace_id = request.environ.get("HTTP_TRACE_ID", None)
    return trace_id


def generate_trace_id(client_ip=''):
    time_section = _get_hex_str((time.time() * 1000000))
    if client_ip:
        client_section = _get_hex_str(_ip2long(client_ip))
    else:
        client_section = get_local_ip()
    server_section = _get_hex_str(_ip2long(get_local_ip()))
    random_section = ('0000' + _get_hex_str(random.randint(0, 10000)))[
                     -5:]
    return "%s-%s-%s-%s" % (time_section, client_section, server_section, random_section)
