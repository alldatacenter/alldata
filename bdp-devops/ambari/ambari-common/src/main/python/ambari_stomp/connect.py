"""Main entry point for clients to create a STOMP connection.

Provides connection classes for `1.0 <http://stomp.github.io/stomp-specification-1.0.html>`_,
`1.1 <http://stomp.github.io/stomp-specification-1.1.html>`_, and
`1.2 <http://stomp.github.io/stomp-specification-1.2.html>`_ versions of the STOMP protocol.
"""

from ambari_stomp.listener import *
from ambari_stomp.protocol import *
from ambari_stomp.transport import *


class BaseConnection(Publisher):
    """
    Base class for all connection classes.
    """

    def __init__(self, transport):
        """
        :param Transport transport:
        """
        self.transport = transport

    def set_listener(self, name, lstnr):
        """
        :param str name:
        :param ConnectionListener lstnr:
        """
        self.transport.set_listener(name, lstnr)

    def remove_listener(self, name):
        """
        :param str name:
        """
        self.transport.remove_listener(name)

    def get_listener(self, name):
        """
        :param str name:

        :rtype: ConnectionListener
        """
        return self.transport.get_listener(name)

    def start(self):
        self.transport.start()

    def stop(self):
        self.transport.stop()

    def is_connected(self):
        """
        :rtype: bool
        """
        return self.transport.is_connected()

    def set_receipt(self, receipt_id, value):
        self.transport.set_receipt(receipt_id, value)

    def set_ssl(self, *args, **kwargs):
        self.transport.set_ssl(*args, **kwargs)

    def get_ssl(self, *args, **kwargs):
        self.transport.get_ssl(*args, **kwargs)


class StompConnection10(BaseConnection, Protocol10):
    """
    Represents a 1.0 connection (comprising transport plus 1.0 protocol class).
    See :py:class:`stomp.transport.Transport` for details on the initialisation parameters.
    """
    def __init__(self,
                 host_and_ports=None,
                 prefer_localhost=True,
                 try_loopback_connect=True,
                 reconnect_sleep_initial=0.1,
                 reconnect_sleep_increase=0.5,
                 reconnect_sleep_jitter=0.1,
                 reconnect_sleep_max=60.0,
                 reconnect_attempts_max=3,
                 use_ssl=False,
                 ssl_key_file=None,
                 ssl_cert_file=None,
                 ssl_ca_certs=None,
                 ssl_cert_validator=None,
                 wait_on_receipt=False,
                 ssl_version=DEFAULT_SSL_VERSION,
                 timeout=None,
                 keepalive=None,
                 auto_decode=True,
                 auto_content_length=True):
        transport = Transport(host_and_ports, prefer_localhost, try_loopback_connect,
                              reconnect_sleep_initial, reconnect_sleep_increase, reconnect_sleep_jitter,
                              reconnect_sleep_max, reconnect_attempts_max, use_ssl, ssl_key_file, ssl_cert_file,
                              ssl_ca_certs, ssl_cert_validator, wait_on_receipt, ssl_version, timeout,
                              keepalive, None, auto_decode)
        BaseConnection.__init__(self, transport)
        Protocol10.__init__(self, transport, auto_content_length)

    def disconnect(self, receipt=None, headers=None, **keyword_headers):
        """
        Call the protocol disconnection, and then stop the transport itself.

        :param str receipt: the receipt to use with the disconnect
        :param dict headers: a map of any additional headers to send with the disconnection
        :param keyword_headers: any additional headers to send with the disconnection
        """
        Protocol10.disconnect(self, receipt, headers, **keyword_headers)
        self.transport.stop()


class StompConnection11(BaseConnection, Protocol11):
    """
    Represents a 1.1 connection (comprising transport plus 1.1 protocol class)
    See :py:class:`stomp.transport.Transport` for details on the initialisation parameters.
    """
    def __init__(self,
                 host_and_ports=None,
                 prefer_localhost=True,
                 try_loopback_connect=True,
                 reconnect_sleep_initial=0.1,
                 reconnect_sleep_increase=0.5,
                 reconnect_sleep_jitter=0.1,
                 reconnect_sleep_max=60.0,
                 reconnect_attempts_max=3,
                 use_ssl=False,
                 ssl_key_file=None,
                 ssl_cert_file=None,
                 ssl_ca_certs=None,
                 ssl_cert_validator=None,
                 wait_on_receipt=False,
                 ssl_version=DEFAULT_SSL_VERSION,
                 timeout=None,
                 heartbeats=(0, 0),
                 keepalive=None,
                 vhost=None,
                 auto_decode=True,
                 auto_content_length=True):
        transport = Transport(host_and_ports, prefer_localhost, try_loopback_connect,
                              reconnect_sleep_initial, reconnect_sleep_increase, reconnect_sleep_jitter,
                              reconnect_sleep_max, reconnect_attempts_max, use_ssl, ssl_key_file, ssl_cert_file,
                              ssl_ca_certs, ssl_cert_validator, wait_on_receipt, ssl_version, timeout,
                              keepalive, vhost, auto_decode)
        BaseConnection.__init__(self, transport)
        Protocol11.__init__(self, transport, heartbeats, auto_content_length)

    def disconnect(self, receipt=None, headers=None, **keyword_headers):
        """
        Call the protocol disconnection, and then stop the transport itself.

        :param str receipt: the receipt to use with the disconnect
        :param dict headers: a map of any additional headers to send with the disconnection
        :param keyword_headers: any additional headers to send with the disconnection
        """
        Protocol11.disconnect(self, receipt, headers, **keyword_headers)
        self.transport.stop()


class StompConnection12(BaseConnection, Protocol12):
    """
    Represents a 1.2 connection (comprising transport plus 1.2 protocol class).
    See :py:class:`stomp.transport.Transport` for details on the initialisation parameters.
    """
    def __init__(self,
                 host_and_ports=None,
                 prefer_localhost=True,
                 try_loopback_connect=True,
                 reconnect_sleep_initial=0.1,
                 reconnect_sleep_increase=0.5,
                 reconnect_sleep_jitter=0.1,
                 reconnect_sleep_max=60.0,
                 reconnect_attempts_max=3,
                 use_ssl=False,
                 ssl_key_file=None,
                 ssl_cert_file=None,
                 ssl_ca_certs=None,
                 ssl_cert_validator=None,
                 wait_on_receipt=False,
                 ssl_version=DEFAULT_SSL_VERSION,
                 timeout=None,
                 heartbeats=(0, 0),
                 keepalive=None,
                 vhost=None,
                 auto_decode=True,
                 auto_content_length=True):
        transport = Transport(host_and_ports, prefer_localhost, try_loopback_connect,
                              reconnect_sleep_initial, reconnect_sleep_increase, reconnect_sleep_jitter,
                              reconnect_sleep_max, reconnect_attempts_max, use_ssl, ssl_key_file, ssl_cert_file,
                              ssl_ca_certs, ssl_cert_validator, wait_on_receipt, ssl_version, timeout,
                              keepalive, vhost, auto_decode)
        BaseConnection.__init__(self, transport)
        Protocol12.__init__(self, transport, heartbeats, auto_content_length)

    def disconnect(self, receipt=None, headers=None, **keyword_headers):
        """
        Call the protocol disconnection, and then stop the transport itself.

        :param str receipt: the receipt to use with the disconnect
        :param dict headers: a map of any additional headers to send with the disconnection
        :param keyword_headers: any additional headers to send with the disconnection
        """
        Protocol12.disconnect(self, receipt, headers, **keyword_headers)
        self.transport.stop()
