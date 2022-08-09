"""
Python2.5 (and lower) specific versions of various networking (ipv6) functions used by stomp.py
"""

from socket import *

ERRMSG = "getaddrinfo returns an empty list"


def get_socket(host, port, timeout=None):
    """
    Return a socket.

    :param str host: the hostname to connect to
    :param int port: the port number to connect to
    :param timeout: if specified, set the socket timeout
    """
    for res in getaddrinfo(host, port, 0, SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        sock = None
        try:
            sock = socket(af, socktype, proto)
            if timeout is not None:
                sock.settimeout(timeout)
            sock.connect(sa)
            return sock

        except error, msg:
            if sock is not None:
                sock.close()

    raise error, ERRMSG
