"""Various listeners for using with stomp.py connections.
"""

import logging
import sys
import threading
import time

from ambari_stomp.backward import monotonic
from ambari_stomp.constants import *
import ambari_stomp.exception as exception
import ambari_stomp.utils as utils


log = logging.getLogger('stomp.py')


class Publisher(object):
    """
    Simply a registry of listeners.
    """

    def set_listener(self, name, listener):
        """
        Set a named listener to use with this connection. See :py:class:`stomp.listener.ConnectionListener`

        :param str name: the name of the listener
        :param ConnectionListener listener: the listener object
        """
        pass

    def remove_listener(self, name):
        """
        Remove a listener.

        :param str name: the name of the listener to remove
        """
        pass

    def get_listener(self, name):
        """
        Return the named listener.

        :param str name: the listener to return

        :rtype: ConnectionListener
        """
        return None


class ConnectionListener(object):
    """
    This class should be used as a base class for objects registered
    using Connection.set_listener().
    """
    def on_connecting(self, host_and_port):
        """
        Called by the STOMP connection once a TCP/IP connection to the
        STOMP server has been established or re-established. Note that
        at this point, no connection has been established on the STOMP
        protocol level. For this, you need to invoke the "connect"
        method on the connection.

        :param (str,int) host_and_port: a tuple containing the host name and port number to which the connection
            has been established.
        """
        pass

    def on_connected(self, headers, body):
        """
        Called by the STOMP connection when a CONNECTED frame is
        received (after a connection has been established or
        re-established).

        :param dict headers: a dictionary containing all headers sent by the server as key/value pairs.
        :param body: the frame's payload. This is usually empty for CONNECTED frames.
        """
        pass

    def on_disconnected(self):
        """
        Called by the STOMP connection when a TCP/IP connection to the
        STOMP server has been lost.  No messages should be sent via
        the connection until it has been reestablished.
        """
        pass

    def on_heartbeat_timeout(self):
        """
        Called by the STOMP connection when a heartbeat message has not been
        received beyond the specified period.
        """
        pass

    def on_before_message(self, headers, body):
        """
        Called by the STOMP connection before a message is returned to the client app. Returns a tuple
        containing the headers and body (so that implementing listeners can pre-process the content).

        :param dict headers: the message headers
        :param body: the message body
        """
        return headers, body

    def on_message(self, headers, body):
        """
        Called by the STOMP connection when a MESSAGE frame is received.

        :param dict headers: a dictionary containing all headers sent by the server as key/value pairs.
        :param body: the frame's payload - the message body.
        """
        pass

    def on_receipt(self, headers, body):
        """
        Called by the STOMP connection when a RECEIPT frame is
        received, sent by the server if requested by the client using
        the 'receipt' header.

        :param dict headers: a dictionary containing all headers sent by the server as key/value pairs.
        :param body: the frame's payload. This is usually empty for RECEIPT frames.
        """
        pass

    def on_error(self, headers, body):
        """
        Called by the STOMP connection when an ERROR frame is received.

        :param dict headers: a dictionary containing all headers sent by the server as key/value pairs.
        :param body: the frame's payload - usually a detailed error description.
        """
        pass

    def on_send(self, frame):
        """
        Called by the STOMP connection when it is in the process of sending a message

        :param Frame frame: the frame to be sent
        """
        pass

    def on_heartbeat(self):
        """
        Called on receipt of a heartbeat.
        """
        pass


class HeartbeatListener(ConnectionListener):
    """
    Listener used to handle STOMP heartbeating.
    """
    def __init__(self, heartbeats):
        self.running = False
        self.heartbeats = heartbeats
        self.received_heartbeat = None
        self.heartbeat_thread = None
        self.next_outbound_heartbeat = None

    def on_connected(self, headers, body):
        """
        Once the connection is established, and 'heart-beat' is found in the headers, we calculate the real
        heartbeat numbers (based on what the server sent and what was specified by the client) - if the heartbeats
        are not 0, we start up the heartbeat loop accordingly.

        :param dict headers: headers in the connection message
        :param body: the message body
        """
        if 'heart-beat' in headers:
            self.heartbeats = utils.calculate_heartbeats(
                headers['heart-beat'].replace(' ', '').split(','), self.heartbeats)
            if self.heartbeats != (0, 0):
                self.send_sleep = self.heartbeats[0] / 1000

                # receive gets an additional grace of 50%
                self.receive_sleep = (self.heartbeats[1] / 1000) * 1.5

                # Give grace of receiving the first heartbeat
                self.received_heartbeat = monotonic() + self.receive_sleep

                self.running = True
                if self.heartbeat_thread is None:
                    self.heartbeat_thread = utils.default_create_thread(
                        self.__heartbeat_loop)
                    self.heartbeat_thread.name = "StompHeartbeat%s" % \
                        getattr(self.heartbeat_thread, "name", "Thread")

    def on_disconnected(self):
        self.running = False

    def on_message(self, headers, body):
        """
        Reset the last received time whenever a message is received.

        :param dict headers: headers in the message
        :param body: the message content
        """
        # reset the heartbeat for any received message
        self.__update_heartbeat()

    def on_receipt(self, *_):
        """
        Reset the last received time whenever a receipt is received.
        """
        self.__update_heartbeat()

    def on_error(self, *_):
        """
        Reset the last received time whenever an error is received.
        """
        self.__update_heartbeat()

    def on_heartbeat(self):
        """
        Reset the last received time whenever a heartbeat message is received.
        """
        self.__update_heartbeat()

    def on_send(self, frame):
        """
        Add the heartbeat header to the frame when connecting, and bump
        next outbound heartbeat timestamp.

        :param Frame frame: the Frame object
        """
        if frame.cmd == CMD_CONNECT or frame.cmd == CMD_STOMP:
            if self.heartbeats != (0, 0):
                frame.headers[HDR_HEARTBEAT] = '%s,%s' % self.heartbeats
        if self.next_outbound_heartbeat is not None:
            self.next_outbound_heartbeat = monotonic() + self.send_sleep

    def __update_heartbeat(self):
        # Honour any grace that has been already included
        if self.received_heartbeat is None:
            return
        now = monotonic()
        if now > self.received_heartbeat:
            self.received_heartbeat = now

    def __heartbeat_loop(self):
        """
        Main loop for sending (and monitoring received) heartbeats.
        """
        now = monotonic()

        # Setup the initial due time for the outbound heartbeat
        if self.send_sleep != 0:
            self.next_outbound_heartbeat = now + self.send_sleep

        while self.running:
            now = monotonic()

            next_events = []
            if self.next_outbound_heartbeat is not None:
                next_events.append(self.next_outbound_heartbeat - now)
            if self.receive_sleep != 0:
                t = self.received_heartbeat + self.receive_sleep - now
                if t > 0:
                    next_events.append(t)
            sleep_time = min(next_events)
            if sleep_time > 0:
                time.sleep(sleep_time)

            now = monotonic()

            if not self.transport.is_connected():
                time.sleep(self.send_sleep)
                continue

            if self.send_sleep != 0 and now > self.next_outbound_heartbeat:
                log.debug("Sending a heartbeat message at %s", now)
                try:
                    self.transport.transmit(utils.Frame(None, {}, None))
                except exception.NotConnectedException:
                    log.debug("Lost connection, unable to send heartbeat")
                except Exception:
                    _, e, _ = sys.exc_info()
                    log.debug("Unable to send heartbeat, due to: %s", e)

            if self.receive_sleep != 0:
                diff_receive = now - self.received_heartbeat

                if diff_receive > self.receive_sleep:
                    # heartbeat timeout
                    log.warning("Heartbeat timeout: diff_receive=%s, time=%s, lastrec=%s",
                                diff_receive, now, self.received_heartbeat)
                    self.transport.set_connected(False)
                    self.transport.disconnect_socket()
                    self.transport.stop()
                    for listener in self.transport.listeners.values():
                        listener.on_heartbeat_timeout()
        self.heartbeat_thread = None


class WaitingListener(ConnectionListener):
    """
    A listener which waits for a specific receipt to arrive.
    """
    def __init__(self, receipt):
        """
        :param str receipt:
        """
        self.condition = threading.Condition()
        self.receipt = receipt
        self.received = False

    def on_receipt(self, headers, body):
        """
        If the receipt id can be found in the headers, then notify the waiting thread.

        :param dict headers: headers in the message
        :param body: the message content
        """
        if 'receipt-id' in headers and headers['receipt-id'] == self.receipt:
            with self.condition:
                self.received = True
                self.condition.notify()

    def wait_on_receipt(self):
        """
        Wait until we receive a message receipt.
        """
        with self.condition:
            while not self.received:
                self.condition.wait()
        self.received = False


class StatsListener(ConnectionListener):
    """
    A connection listener for recording statistics on messages sent and received.
    """
    def __init__(self):
        # The number of errors received
        self.errors = 0
        # The number of connections established
        self.connections = 0
        # The number of disconnections
        self.disconnects = 0
        # The number of messages received
        self.messages = 0
        # The number of messages sent
        self.messages_sent = 0
        # The number of heartbeat timeouts
        self.heartbeat_timeouts = 0
        # The number of heartbeats
        self.heartbeat_count = 0

    def on_disconnected(self):
        """
        Increment the disconnect count. See :py:meth:`ConnectionListener.on_disconnected`
        """
        self.disconnects += 1
        log.info("disconnected (x %s)", self.disconnects)

    def on_error(self, headers, body):
        """
        Increment the error count. See :py:meth:`ConnectionListener.on_error`

        :param dict headers: headers in the message
        :param body: the message content
        """
        log.info("received an error %s [%s]", body, headers)
        self.errors += 1

    def on_connecting(self, host_and_port):
        """
        Increment the connection count. See :py:meth:`ConnectionListener.on_connecting`

        :param (str,int) host_and_port: the host and port as a tuple
        """
        log.info("connecting %s %s (x %s)", host_and_port[0], host_and_port[1], self.connections)
        self.connections += 1

    def on_message(self, headers, body):
        """
        Increment the message received count. See :py:meth:`ConnectionListener.on_message`

        :param dict headers: headers in the message
        :param body: the message content
        """
        self.messages += 1

    def on_send(self, frame):
        """
        Increment the send count. See :py:meth:`ConnectionListener.on_send`

        :param Frame frame:
        """
        self.messages_sent += 1

    def on_heartbeat_timeout(self):
        """
        Increment the heartbeat timeout. See :py:meth:`ConnectionListener.on_heartbeat_timeout`
        """
        log.debug("received heartbeat timeout")
        self.heartbeat_timeouts += 1

    def on_heartbeat(self):
        """
        Increment the heartbeat count. See :py:meth:`ConnectionListener.on_heartbeat`
        """
        self.heartbeat_count += 1

    def __str__(self):
        """
        Return a string containing the current statistics (messages sent and received,
        errors, etc)
        """
        return '''Connections: %s
Messages sent: %s
Messages received: %s
Heartbeats received: %s
Errors: %s''' % (self.connections, self.messages_sent, self.messages, self.heartbeat_count, self.errors)


class PrintingListener(ConnectionListener):
    def on_connecting(self, host_and_port):
        """
        :param (str,int) host_and_port:
        """
        print('on_connecting %s %s' % host_and_port)

    def on_connected(self, headers, body):
        """
        :param dict headers:
        :param body:
        """
        print('on_connected %s %s' % (headers, body))

    def on_disconnected(self):
        print('on_disconnected')

    def on_heartbeat_timeout(self):
        print('on_heartbeat_timeout')

    def on_before_message(self, headers, body):
        """
        :param dict headers:
        :param body:
        """
        print('on_before_message %s %s' % (headers, body))
        return headers, body

    def on_message(self, headers, body):
        """
        :param dict headers:
        :param body:
        """
        print('on_message %s %s' % (headers, body))

    def on_receipt(self, headers, body):
        """
        :param dict headers:
        :param body:
        """
        print('on_receipt %s %s' % (headers, body))

    def on_error(self, headers, body):
        """
        :param dict headers:
        :param body:
        """
        print('on_error %s %s' % (headers, body))

    def on_send(self, frame):
        """
        :param Frame frame:
        """
        print('on_send %s %s %s' % (frame.cmd, frame.headers, frame.body))

    def on_heartbeat(self):
        print('on_heartbeat')


class TestListener(StatsListener, WaitingListener):
    """
    Implementation of StatsListener and WaitingListener. Useful for testing.
    """
    def __init__(self, receipt=None):
        """
        :param str receipt:
        """
        StatsListener.__init__(self)
        WaitingListener.__init__(self, receipt)
        self.message_list = []
        self.message_condition = threading.Condition()
        self.message_received = False
        self.heartbeat_condition = threading.Condition()
        self.heartbeat_received = False

    def on_message(self, headers, message):
        """
        :param dict headers:
        :param message:
        """
        StatsListener.on_message(self, headers, message)
        self.message_list.append((headers, message))
        with self.message_condition:
            self.message_received = True
            self.message_condition.notify()

    def wait_for_message(self):
        with self.message_condition:
            while not self.message_received:
                self.message_condition.wait()
        self.message_received = False

    def get_latest_message(self):
        return self.message_list[-1]

    def on_heartbeat(self):
        StatsListener.on_heartbeat(self)
        with self.heartbeat_condition:
            self.heartbeat_received = True
            self.heartbeat_condition.notify()

    def wait_for_heartbeat(self):
        with self.heartbeat_condition:
            while not self.heartbeat_received:
                self.heartbeat_condition.wait()
        self.heartbeat_received = False
