"""Provides the underlying transport functionality (for stomp message transmission) - (mostly) independent from the actual STOMP protocol
"""

import errno
from io import BytesIO
import logging
import math
import random
import re
import socket
import sys
import threading
import time
import warnings

try:
    import ssl
    from ssl import SSLError

    DEFAULT_SSL_VERSION = ssl.PROTOCOL_TLSv1_2
except (ImportError, AttributeError):  # python version < 2.6 without the backported ssl module
    ssl = None

    class SSLError(object):
        pass

    DEFAULT_SSL_VERSION = None

try:
    from socket import SOL_SOCKET, SO_KEEPALIVE
    from socket import SOL_TCP, TCP_KEEPIDLE, TCP_KEEPINTVL, TCP_KEEPCNT

    LINUX_KEEPALIVE_AVAIL = True
except ImportError:
    LINUX_KEEPALIVE_AVAIL = False

from ambari_stomp.backward import decode, encode, get_errno, monotonic, pack
from ambari_stomp.backwardsock import get_socket
from ambari_stomp.constants import *
import ambari_stomp.exception as exception
import ambari_stomp.listener
import ambari_stomp.utils as utils


log = logging.getLogger('stomp.py')


class BaseTransport(ambari_stomp.listener.Publisher):
    """
    Base class for transport classes providing support for listeners, threading overrides,
    and anything else outside of actually establishing a network connection, sending and
    receiving of messages (so generally socket-agnostic functions).

    :param bool wait_on_receipt: deprecated, ignored
    :param bool auto_decode: automatically decode message responses as strings, rather than
        leaving them as bytes. This preserves the behaviour as of version 4.0.16.
        (To be defaulted to False as of the next release)
    """

    #
    # Used to parse the STOMP "content-length" header lines,
    #
    __content_length_re = re.compile(b'^content-length[:]\\s*(?P<value>[0-9]+)', re.MULTILINE)

    def __init__(self, wait_on_receipt=False, auto_decode=True):
        self.__recvbuf = b''
        self.listeners = {}
        self.running = False
        self.blocking = None
        self.connected = False
        self.connection_error = False
        self.__receipts = {}
        self.current_host_and_port = None
        self.receiver_thread = None

        # flag used when we receive the disconnect receipt
        self.__disconnect_receipt = None

        # function for creating threads used by the connection
        self.create_thread_fc = utils.default_create_thread

        self.__receiver_thread_exit_condition = threading.Condition()
        self.__receiver_thread_exited = False
        self.__send_wait_condition = threading.Condition()
        self.__connect_wait_condition = threading.Condition()
        self.__auto_decode = auto_decode

    def override_threading(self, create_thread_fc):
        """
        Override for thread creation. Use an alternate threading library by
        setting this to a function with a single argument (which is the receiver loop callback).
        The thread which is returned should be started (ready to run)

        :param function create_thread_fc: single argument function for creating a thread
        """
        self.create_thread_fc = create_thread_fc

    #
    # Manage the connection
    #

    def start(self):
        """
        Start the connection. This should be called after all
        listeners have been registered. If this method is not called,
        no frames will be received by the connection.
        """
        self.running = True
        self.attempt_connection()
        self.receiver_thread = self.create_thread_fc(self.__receiver_loop)
        self.receiver_thread.name = "StompReceiver%s" % getattr(self.receiver_thread, "name", "Thread")
        self.notify('connecting')

    def stop(self):
        """
        Stop the connection. Performs a clean shutdown by waiting for the
        receiver thread to exit.
        """
        if not self.receiver_thread or not self.receiver_thread.is_alive():
          return

        with self.__receiver_thread_exit_condition:
            while not self.__receiver_thread_exited:
                self.__receiver_thread_exit_condition.wait()

    def is_connected(self):
        """
        :rtype: bool
        """
        return self.connected

    def set_connected(self, connected):
        """
        :param bool connected:
        """
        with self.__connect_wait_condition:
            self.connected = connected
            if connected:
                self.__connect_wait_condition.notify()

    def set_receipt(self, receipt_id, value):
        if value:
            self.__receipts[receipt_id] = value
        elif receipt_id in self.__receipts:
            del self.__receipts[receipt_id]

    #
    # Manage objects listening to incoming frames
    #

    def set_listener(self, name, listener):
        """
        Set a named listener to use with this connection.
        See :py:class:`stomp.listener.ConnectionListener`

        :param str name: the name of the listener
        :param ConnectionListener listener: the listener object
        """
        self.listeners[name] = listener

    def remove_listener(self, name):
        """
        Remove a listener according to the specified name

        :param str name: the name of the listener to remove
        """
        del self.listeners[name]

    def get_listener(self, name):
        """
        Return the named listener

        :param str name: the listener to return

        :rtype: ConnectionListener
        """
        return self.listeners.get(name)

    def process_frame(self, f, frame_str):
        """
        :param Frame f: Frame object
        :param bytes frame_str: raw frame content
        """
        frame_type = f.cmd.lower()
        if frame_type in ['connected', 'message', 'receipt', 'error', 'heartbeat']:
            if frame_type == 'message':
                (f.headers, f.body) = self.notify('before_message', f.headers, f.body)
            if log.isEnabledFor(logging.DEBUG):
                log.debug("Received frame: %r, headers=%r, body=%r", f.cmd, f.headers, f.body)
            else:
                log.debug("Received frame: %r, headers=%r, len(body)=%r", f.cmd, f.headers, utils.length(f.body))
            self.notify(frame_type, f.headers, f.body)
        else:
            log.warning("Unknown response frame type: '%s' (frame length was %d)", frame_type, utils.length(frame_str))

    def notify(self, frame_type, headers=None, body=None):
        """
        Utility function for notifying listeners of incoming and outgoing messages

        :param str frame_type: the type of message
        :param dict headers: the map of headers associated with the message
        :param body: the content of the message
        """
        if frame_type == 'receipt':
            # logic for wait-on-receipt notification
            receipt = headers['receipt-id']
            receipt_value = self.__receipts.get(receipt)
            with self.__send_wait_condition:
                self.set_receipt(receipt, None)
                self.__send_wait_condition.notify()

            # received a stomp 1.1+ disconnect receipt
            if receipt == self.__disconnect_receipt:
                self.disconnect_socket()

            if receipt_value == CMD_DISCONNECT:
                self.set_connected(False)

        elif frame_type == 'connected':
            self.set_connected(True)

        elif frame_type == 'disconnected':
            self.set_connected(False)

        for listener in self.listeners.values():
            if not listener:
                continue

            notify_func = getattr(listener, 'on_%s' % frame_type, None)
            if not notify_func:
                log.debug("listener %s has no method on_%s", listener, frame_type)
                continue
            if frame_type in ('heartbeat', 'disconnected'):
                notify_func()
                continue
            if frame_type == 'connecting':
                notify_func(self.current_host_and_port)
                continue

            if frame_type == 'error' and not self.connected:
                with self.__connect_wait_condition:
                    self.connection_error = True
                    self.__connect_wait_condition.notify()

            rtn = notify_func(headers, body)
            if rtn:
                (headers, body) = rtn
        return (headers, body)

    def transmit(self, frame):
        """
        Convert a frame object to a frame string and transmit to the server.

        :param Frame frame: the Frame object to transmit
        """
        for listener in self.listeners.values():
            if not listener:
                continue
            try:
                listener.on_send(frame)
            except AttributeError:
                continue

        lines = utils.convert_frame_to_lines(frame)

        packed_frame = pack(lines)

        if log.isEnabledFor(logging.DEBUG):
            log.debug("Sending frame: %s", lines)
        else:
            log.debug("Sending frame: %r, headers=%r", frame.cmd or "heartbeat", frame.headers)

        self.send(encode(packed_frame))

    def send(self, encoded_frame):
        """
        Send an encoded frame over this transport (to be implemented in subclasses)

        :param bytes encoded_frame: a Frame object which has been encoded for transmission
        """
        pass

    def receive(self):
        """
        Receive a chunk of data (to be implemented in subclasses)

        :rtype: bytes
        """
        pass

    def cleanup(self):
        """
        Cleanup the transport (to be implemented in subclasses)
        """
        pass

    def attempt_connection(self):
        """
        Attempt to establish a connection.
        """
        pass

    def disconnect_socket(self):
        """
        Disconnect the socket.
        """

    def get_connect_wait_condition(self):
      return self.__connect_wait_condition

    def wait_for_connection(self, timeout=None):
        """
        Wait until we've established a connection with the server.

        :param float timeout: how long to wait, in seconds
        """
        if timeout is not None:
            wait_time = timeout / 10.0
        else:
            wait_time = None
        with self.__connect_wait_condition:
            while not self.is_connected() and not self.connection_error:
                self.__connect_wait_condition.wait(wait_time)

    def __receiver_loop(self):
        """
        Main loop listening for incoming data.
        """
        log.info("Starting receiver loop")
        try:
            while self.running:
                try:
                    while self.running:
                        frames = self.__read()

                        for frame in frames:
                            f = utils.parse_frame(frame)
                            if f is None:
                                continue
                            if self.__auto_decode:
                                f.body = decode(f.body)
                            self.process_frame(f, frame)
                except exception.ConnectionClosedException:
                    if self.running:
                        self.notify('disconnected')
                        #
                        # Clear out any half-received messages after losing connection
                        #
                        self.__recvbuf = b''
                        self.running = False
                    break
                finally:
                    self.cleanup()
        finally:
            with self.__receiver_thread_exit_condition:
                self.__receiver_thread_exited = True
                self.__receiver_thread_exit_condition.notifyAll()
            log.info("Receiver loop ended")

    def __read(self):
        """
        Read the next frame(s) from the socket.

        :return: list of frames read
        :rtype: list(bytes)
        """
        fastbuf = BytesIO()
        while self.running:
            try:
                try:
                    c = self.receive()
                except exception.InterruptedException:
                    log.debug("socket read interrupted, restarting")
                    continue
            except Exception:
                log.debug("socket read error", exc_info=True)
                c = b''
            if c is None or len(c) == 0:
                raise exception.ConnectionClosedException()
            if c == b'\x0a' and not self.__recvbuf and not fastbuf.tell():
                #
                # EOL to an empty receive buffer: treat as heartbeat.
                # Note that this may misdetect an optional EOL at end of frame as heartbeat in case the
                # previous receive() got a complete frame whose NUL at end of frame happened to be the
                # last byte of that read. But that should be harmless in practice.
                #
                fastbuf.close()
                return [c]
            fastbuf.write(c)
            if b'\x00' in c:
                #
                # Possible end of frame
                #
                break
        self.__recvbuf += fastbuf.getvalue()
        fastbuf.close()
        result = []

        if self.__recvbuf and self.running:
            while True:
                pos = self.__recvbuf.find(b'\x00')

                if pos >= 0:
                    frame = self.__recvbuf[0:pos]
                    preamble_end_match = utils.PREAMBLE_END_RE.search(frame)
                    if preamble_end_match:
                        preamble_end = preamble_end_match.start()
                        content_length_match = BaseTransport.__content_length_re.search(frame[0:preamble_end])
                        if content_length_match:
                            content_length = int(content_length_match.group('value'))
                            content_offset = preamble_end_match.end()
                            frame_size = content_offset + content_length
                            if frame_size > len(frame):
                                #
                                # Frame contains NUL bytes, need to read more
                                #
                                if frame_size < len(self.__recvbuf):
                                    pos = frame_size
                                    frame = self.__recvbuf[0:pos]
                                else:
                                    #
                                    # Haven't read enough data yet, exit loop and wait for more to arrive
                                    #
                                    break
                    result.append(frame)
                    pos += 1
                    #
                    # Ignore optional EOLs at end of frame
                    #
                    while self.__recvbuf[pos:pos + 1] == b'\x0a':
                        pos += 1
                    self.__recvbuf = self.__recvbuf[pos:]
                else:
                    break
        return result


class Transport(BaseTransport):
    """
    Represents a STOMP client 'transport'. Effectively this is the communications mechanism without the definition of
    the protocol.

    :param list((str,int)) host_and_ports: a list of (host, port) tuples
    :param bool prefer_localhost: if True and the local host is mentioned in the (host,
        port) tuples, try to connect to this first
    :param bool try_loopback_connect: if True and the local host is found in the host
        tuples, try connecting to it using loopback interface
        (127.0.0.1)
    :param float reconnect_sleep_initial: initial delay in seconds to wait before reattempting
        to establish a connection if connection to any of the
        hosts fails.
    :param float reconnect_sleep_increase: factor by which the sleep delay is increased after
        each connection attempt. For example, 0.5 means
        to wait 50% longer than before the previous attempt,
        1.0 means wait twice as long, and 0.0 means keep
        the delay constant.
    :param float reconnect_sleep_max: maximum delay between connection attempts, regardless
        of the reconnect_sleep_increase.
    :param float reconnect_sleep_jitter: random additional time to wait (as a percentage of
        the time determined using the previous parameters)
        between connection attempts in order to avoid
        stampeding. For example, a value of 0.1 means to wait
        an extra 0%-10% (randomly determined) of the delay
        calculated using the previous three parameters.
    :param int reconnect_attempts_max: maximum attempts to reconnect
    :param bool use_ssl: deprecated, see :py:meth:`set_ssl`
    :param ssl_cert_file: deprecated, see :py:meth:`set_ssl`
    :param ssl_key_file: deprecated, see :py:meth:`set_ssl`
    :param ssl_ca_certs: deprecated, see :py:meth:`set_ssl`
    :param ssl_cert_validator: deprecated, see :py:meth:`set_ssl`
    :param ssl_version: deprecated, see :py:meth:`set_ssl`
    :param timeout: the timeout value to use when connecting the stomp socket
    :param bool wait_on_receipt: deprecated, ignored
    :param keepalive: some operating systems support sending the occasional heart
        beat packets to detect when a connection fails.  This
        parameter can either be set set to a boolean to turn on the
        default keepalive options for your OS, or as a tuple of
        values, which also enables keepalive packets, but specifies
        options specific to your OS implementation
    :param str vhost: specify a virtual hostname to provide in the 'host' header of the connection
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
                 ssl_version=None,
                 timeout=None,
                 keepalive=None,
                 vhost=None,
                 auto_decode=True
                 ):
        BaseTransport.__init__(self, wait_on_receipt, auto_decode)

        if host_and_ports is None:
            host_and_ports = [('localhost', 61613)]

        sorted_host_and_ports = []
        sorted_host_and_ports.extend(host_and_ports)

        #
        # If localhost is preferred, make sure all (host, port) tuples that refer to the local host come first in
        # the list
        #
        if prefer_localhost:
            sorted_host_and_ports.sort(key=utils.is_localhost)

        #
        # If the user wishes to attempt connecting to local ports using the loopback interface, for each (host, port)
        # tuple referring to a local host, add an entry with the host name replaced by 127.0.0.1 if it doesn't
        # exist already
        #
        loopback_host_and_ports = []
        if try_loopback_connect:
            for host_and_port in sorted_host_and_ports:
                if utils.is_localhost(host_and_port) == 1:
                    port = host_and_port[1]
                    if not (("127.0.0.1", port) in sorted_host_and_ports or ("localhost", port) in sorted_host_and_ports):
                        loopback_host_and_ports.append(("127.0.0.1", port))

        #
        # Assemble the final, possibly sorted list of (host, port) tuples
        #
        self.__host_and_ports = []
        self.__host_and_ports.extend(loopback_host_and_ports)
        self.__host_and_ports.extend(sorted_host_and_ports)

        self.__reconnect_sleep_initial = reconnect_sleep_initial
        self.__reconnect_sleep_increase = reconnect_sleep_increase
        self.__reconnect_sleep_jitter = reconnect_sleep_jitter
        self.__reconnect_sleep_max = reconnect_sleep_max
        self.__reconnect_attempts_max = reconnect_attempts_max
        self.__timeout = timeout

        self.socket = None
        self.__socket_semaphore = threading.BoundedSemaphore(1)
        self.current_host_and_port = None

        # setup SSL
        self.__ssl_params = {}
        if use_ssl:
            warnings.warn("Deprecated: use set_ssl instead", DeprecationWarning)
            self.set_ssl(host_and_ports,
                         ssl_key_file,
                         ssl_cert_file,
                         ssl_ca_certs,
                         ssl_cert_validator,
                         ssl_version)

        self.__keepalive = keepalive
        self.vhost = vhost

    def is_connected(self):
        """
        Return true if the socket managed by this connection is connected

        :rtype: bool
        """
        try:
            return self.socket is not None and self.socket.getsockname()[1] != 0 and BaseTransport.is_connected(self)
        except socket.error:
            return False

    def disconnect_socket(self):
        """
        Disconnect the underlying socket connection
        """
        self.running = False
        if self.socket is not None:
            if self.__need_ssl():
                #
                # Even though we don't want to use the socket, unwrap is the only API method which does a proper SSL
                # shutdown
                #
                try:
                    self.socket = self.socket.unwrap()
                except Exception:
                    #
                    # unwrap seems flaky on Win with the back-ported ssl mod, so catch any exception and log it
                    #
                    _, e, _ = sys.exc_info()
                    log.warning(e)
            elif hasattr(socket, 'SHUT_RDWR'):
                try:
                    self.socket.shutdown(socket.SHUT_RDWR)
                except socket.error:
                    _, e, _ = sys.exc_info()
                    # ignore when socket already closed
                    if get_errno(e) != errno.ENOTCONN:
                        log.warning("Unable to issue SHUT_RDWR on socket because of error '%s'", e)

        #
        # split this into a separate check, because sometimes the socket is nulled between shutdown and this call
        #
        if self.socket is not None:
            try:
                self.socket.close()
            except socket.error:
                _, e, _ = sys.exc_info()
                log.warning("Unable to close socket because of error '%s'", e)
        self.current_host_and_port = None
        self.socket = None
        self.notify('disconnected')

    def send(self, encoded_frame):
        """
        :param bytes encoded_frame:
        """
        if self.socket is not None:
            try:
                with self.__socket_semaphore:
                    self.socket.sendall(encoded_frame)
            except Exception:
                _, e, _ = sys.exc_info()
                log.error("Error sending frame", exc_info=1)
                raise e
        else:
            raise exception.NotConnectedException()

    def receive(self):
        """
        :rtype: bytes
        """
        try:
            return self.socket.recv(1024)
        except socket.error:
            _, e, _ = sys.exc_info()
            if get_errno(e) in (errno.EAGAIN, errno.EINTR):
                log.debug("socket read interrupted, restarting")
                raise exception.InterruptedException()
            if self.is_connected():
                raise

    def cleanup(self):
        """
        Close the socket and clear the current host and port details.
        """
        try:
            self.socket.close()
        except:
            pass  # ignore errors when attempting to close socket
        self.socket = None
        self.current_host_and_port = None

    def __enable_keepalive(self):
        def try_setsockopt(sock, name, fam, opt, val):
            if val is None:
                return True  # no value to set always works
            try:
                sock.setsockopt(fam, opt, val)
                log.info("keepalive: set %r option to %r on socket", name, val)
            except:
                log.error("keepalive: unable to set %r option to %r on socket", name, val)
                return False
            return True

        ka = self.__keepalive

        if not ka:
            return

        if ka is True:
            ka_sig = 'auto'
            ka_args = ()
        else:
            try:
                ka_sig = ka[0]
                ka_args = ka[1:]
            except Exception:
                log.error("keepalive: bad specification %r", ka)
                return

        if ka_sig == 'auto':
            if LINUX_KEEPALIVE_AVAIL:
                ka_sig = 'linux'
                ka_args = None
                log.info("keepalive: autodetected linux-style support")
            else:
                log.error("keepalive: unable to detect any implementation, DISABLED!")
                return

        if ka_sig == 'linux':
            log.info("keepalive: activating linux-style support")
            if ka_args is None:
                log.info("keepalive: using system defaults")
                ka_args = (None, None, None)
            lka_idle, lka_intvl, lka_cnt = ka_args
            if try_setsockopt(self.socket, 'enable', SOL_SOCKET, SO_KEEPALIVE, 1):
                try_setsockopt(self.socket, 'idle time', SOL_TCP, TCP_KEEPIDLE, lka_idle)
                try_setsockopt(self.socket, 'interval', SOL_TCP, TCP_KEEPINTVL, lka_intvl)
                try_setsockopt(self.socket, 'count', SOL_TCP, TCP_KEEPCNT, lka_cnt)
        else:
            log.error("keepalive: implementation %r not recognized or not supported", ka_sig)

    def attempt_connection(self):
        """
        Try connecting to the (host, port) tuples specified at construction time.
        """
        self.connection_error = False
        sleep_exp = 1
        connect_count = 0

        while self.running and self.socket is None and connect_count < self.__reconnect_attempts_max:
            for host_and_port in self.__host_and_ports:
                try:
                    log.info("Attempting connection to host %s, port %s", host_and_port[0], host_and_port[1])
                    self.socket = get_socket(host_and_port[0], host_and_port[1], self.__timeout)
                    self.__enable_keepalive()
                    need_ssl = self.__need_ssl(host_and_port)

                    if need_ssl:  # wrap socket
                        ssl_params = self.get_ssl(host_and_port)
                        if ssl_params['ca_certs']:
                            cert_validation = ssl.CERT_REQUIRED
                        else:
                            cert_validation = ssl.CERT_NONE
                        try:
                            tls_context = ssl.create_default_context(cafile=ssl_params['ca_certs'])
                        except AttributeError:
                            tls_context = None
                        if tls_context:
                            # Wrap the socket for TLS
                            certfile = ssl_params['cert_file']
                            keyfile = ssl_params['key_file']
                            password = ssl_params.get('password')
                            if certfile and not keyfile:
                                keyfile = certfile
                            if certfile:
                                tls_context.load_cert_chain(certfile, keyfile, password)
                            if cert_validation is None or cert_validation == ssl.CERT_NONE:
                                tls_context.check_hostname = False
                            tls_context.verify_mode = cert_validation
                            self.socket = tls_context.wrap_socket(self.socket, server_hostname=host_and_port[0])
                        else:
                            # Old-style wrap_socket where we don't have a modern SSLContext (so no SNI)
                            self.socket = ssl.wrap_socket(
                                self.socket,
                                keyfile=ssl_params['key_file'],
                                certfile=ssl_params['cert_file'],
                                cert_reqs=cert_validation,
                                ca_certs=ssl_params['ca_certs'],
                                ssl_version=ssl_params['ssl_version'])

                    self.socket.settimeout(self.__timeout)

                    if self.blocking is not None:
                        self.socket.setblocking(self.blocking)

                    #
                    # Validate server cert
                    #
                    if need_ssl and ssl_params['cert_validator']:
                        cert = self.socket.getpeercert()
                        (ok, errmsg) = ssl_params['cert_validator'](cert, host_and_port[0])
                        if not ok:
                            raise SSLError("Server certificate validation failed: %s", errmsg)

                    self.current_host_and_port = host_and_port
                    log.info("Established connection to host %s, port %s", host_and_port[0], host_and_port[1])
                    break
                except socket.error:
                    self.socket = None
                    connect_count += 1
                    log.warning("Could not connect to host %s, port %s", host_and_port[0], host_and_port[1], exc_info=1)

            if self.socket is None:
                sleep_duration = (min(self.__reconnect_sleep_max,
                                      ((self.__reconnect_sleep_initial / (1.0 + self.__reconnect_sleep_increase))
                                       * math.pow(1.0 + self.__reconnect_sleep_increase, sleep_exp)))
                                  * (1.0 + random.random() * self.__reconnect_sleep_jitter))
                sleep_end = monotonic() + sleep_duration
                log.debug("Sleeping for %.1f seconds before attempting reconnect", sleep_duration)
                while self.running and monotonic() < sleep_end:
                    time.sleep(0.2)

                if sleep_duration < self.__reconnect_sleep_max:
                    sleep_exp += 1

        if not self.socket:
            raise exception.ConnectFailedException()

    def set_ssl(self,
                for_hosts=(),
                key_file=None,
                cert_file=None,
                ca_certs=None,
                cert_validator=None,
                ssl_version=DEFAULT_SSL_VERSION,
                password=None):
        """
        Sets up SSL configuration for the given hosts. This ensures socket is wrapped in a SSL connection, raising an
        exception if the SSL module can't be found.

        :param for_hosts: hosts this SSL configuration should be applied to
        :param cert_file: the path to a X509 certificate
        :param key_file: the path to a X509 key file
        :param ca_certs: the path to the a file containing CA certificates to validate the server against.
                         If this is not set, server side certificate validation is not done.
        :param cert_validator: function which performs extra validation on the client certificate, for example
                               checking the returned certificate has a commonName attribute equal to the
                               hostname (to avoid man in the middle attacks).
                               The signature is: (OK, err_msg) = validation_function(cert, hostname)
                               where OK is a boolean, and cert is a certificate structure
                               as returned by ssl.SSLSocket.getpeercert()
        :param ssl_version: SSL protocol to use for the connection. This should be one of the PROTOCOL_x
                            constants provided by the ssl module. The default is ssl.PROTOCOL_TLSv1_2
        """
        if not ssl:
            raise Exception("SSL connection requested, but SSL library not found")

        for host_port in for_hosts:
            self.__ssl_params[host_port] = dict(key_file=key_file,
                                                cert_file=cert_file,
                                                ca_certs=ca_certs,
                                                cert_validator=cert_validator,
                                                ssl_version=ssl_version,
                                                password=password)

    def __need_ssl(self, host_and_port=None):
        """
        Whether current host needs SSL or not.

        :param (str,int) host_and_port: the host/port pair to check, default current_host_and_port
        """
        if not host_and_port:
            host_and_port = self.current_host_and_port

        return host_and_port in self.__ssl_params

    def get_ssl(self, host_and_port=None):
        """
        Get SSL params for the given host.

        :param (str,int) host_and_port: the host/port pair we want SSL params for, default current_host_and_port
        """
        if not host_and_port:
            host_and_port = self.current_host_and_port

        return self.__ssl_params.get(host_and_port)
