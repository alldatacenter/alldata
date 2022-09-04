import random
import socket
import struct
import time

import web


def _ip2long(ip):
    packedIP = socket.inet_aton(ip)
    return struct.unpack("!L", packedIP)[0]


def _long2ip(addr):
    return socket.inet_ntoa(struct.pack("!L", addr))


def _get_hex_str(value):
    return str(hex(value)).replace('0x', '')


def _get_hex_value(strv):
    return long('0x' + strv, 16)


def get_local_ip():
    return web.ctx.local_ip


def gen_request_id(client_ip):
    time_section = _get_hex_str(long((time.time() * 1000000)))
    client_section = _get_hex_str(_ip2long(client_ip))
    server_section = _get_hex_str(_ip2long(get_local_ip()))
    random_section = ('0000' + _get_hex_str(long(random.randint(0, 10000))))[-5:]
    return "%s-%s-%s-%s" % (time_section, client_section, server_section, random_section)


def parse_request_id(request_id):
    request_section_list = request_id.split('-')
    ts = _get_hex_value(request_section_list[0]) / 1000000
    timeArray = time.localtime(ts)
    request_time = time.strftime("%Y-%m-%d %H:%M:%S", timeArray)
    client_section = _long2ip(_get_hex_value(request_section_list[1]))
    server_section = _long2ip(_get_hex_value(request_section_list[2]))
    random_section = _long2ip(_get_hex_value(request_section_list[3]))
    return "time:%s, client:%s, server:%s, random:%s" % (request_time, client_section, server_section, random_section)


if __name__ == '__main__':
    a1 = gen_request_id('1.1.1.1')
    a2 = gen_request_id('1.1.1.1')
    print a1
    print a2
    if a1 == a2:
        print "equal"
    else:
        print "not equal"
    print a1
    print parse_request_id(a1)
