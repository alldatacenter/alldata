"""
Python2.6 (and greater) specific versions of various networking (ipv6) functions used by stomp.py
"""

import socket


def get_socket(host, port, timeout=None):
    """
    Return a socket connection.

    :param str host: the hostname to connect to
    :param int port: the port number to connect to
    :param timeout: if specified, set the socket timeout
    """
    return socket.create_connection((host, port), timeout)
