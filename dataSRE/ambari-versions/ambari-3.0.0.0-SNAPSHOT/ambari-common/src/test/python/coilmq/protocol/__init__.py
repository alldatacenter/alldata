import abc
import uuid
import socket
import datetime

from coilmq.exception import ProtocolError, AuthError
from coilmq.util import frames
from coilmq.util.frames import Frame, ErrorFrame, ReceiptFrame, ConnectedFrame
from coilmq.util.concurrency import CoilThreadingTimer

SEND = 'SEND'
CONNECT = 'CONNECT'
MESSAGE = 'MESSAGE'
ERROR = 'ERROR'
CONNECTED = 'CONNECTED'
SUBSCRIBE = 'SUBSCRIBE'
UNSUBSCRIBE = 'UNSUBSCRIBE'
BEGIN = 'BEGIN'
COMMIT = 'COMMIT'
ABORT = 'ABORT'
ACK = 'ACK'
DISCONNECT = 'DISCONNECT'

VALID_COMMANDS = ['message', 'connect', 'connected', 'error', 'send',
                  'subscribe', 'unsubscribe', 'begin', 'commit', 'abort', 'ack', 'disconnect', 'nack', 'stomp']


class STOMP(object):

    __metaclass__ = abc.ABCMeta

    def __init__(self, engine):
        self.engine = engine

    def stomp(self, frame):
        self.connect(frame)

    @abc.abstractmethod
    def process_frame(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def connect(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def send(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def subscribe(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def unsubscribe(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def begin(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def commit(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def abort(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def ack(self, frame):
        raise NotImplementedError

    @abc.abstractmethod
    def disconnect(self, frame):
        raise NotImplementedError


class STOMP10(STOMP):

    def process_frame(self, frame):
        """
        Dispatches a received frame to the appropriate internal method.

        @param frame: The frame that was received.
        @type frame: C{stompclient.frame.Frame}
        """
        cmd_method = frame.cmd.lower()

        if not cmd_method in VALID_COMMANDS:
            raise ProtocolError("Invalid STOMP command: {}".format(frame.cmd))

        method = getattr(self, cmd_method, None)

        if not self.engine.connected and method not in (self.connect, self.stomp):
            raise ProtocolError("Not connected.")

        try:
            transaction = frame.headers.get('transaction')
            if not transaction or method in (self.begin, self.commit, self.abort):
                method(frame)
            else:
                if not transaction in self.engine.transactions:
                    raise ProtocolError(
                        "Invalid transaction specified: %s" % transaction)
                self.engine.transactions[transaction].append(frame)
        except Exception as e:
            self.engine.log.error("Error processing STOMP frame: %s" % e)
            self.engine.log.exception(e)
            try:
                self.engine.connection.send_frame(ErrorFrame(str(e), str(e)))
            except Exception as e:  # pragma: no cover
                self.engine.log.error("Could not send error frame: %s" % e)
                self.engine.log.exception(e)
        else:
            # The protocol is not especially clear here (not sure why I'm surprised)
            # about the expected behavior WRT receipts and errors.  We will assume that
            # the RECEIPT frame should not be sent if there is an error frame.
            # Also we'll assume that a transaction should not preclude sending the receipt
            # frame.
            # import pdb; pdb.set_trace()
            if frame.headers.get('receipt') and method != self.connect:
                self.engine.connection.send_frame(ReceiptFrame(
                    receipt=frame.headers.get('receipt')))

    def connect(self, frame, response=None):
        """
        Handle CONNECT command: Establishes a new connection and checks auth (if applicable).
        """
        self.engine.log.debug("CONNECT")

        if self.engine.authenticator:
            login = frame.headers.get('login')
            passcode = frame.headers.get('passcode')
            if not self.engine.authenticator.authenticate(login, passcode):
                raise AuthError("Authentication failed for %s" % login)

        self.engine.connected = True

        response = response or Frame(frames.CONNECTED)
        response.headers['session'] = uuid.uuid4()

        # TODO: Do we want to do anything special to track sessions?
        # (Actually, I don't think the spec actually does anything with this at all.)
        self.engine.connection.send_frame(response)

    def send(self, frame):
        """
        Handle the SEND command: Delivers a message to a queue or topic (default).
        """
        dest = frame.headers.get('destination')
        if not dest:
            raise ProtocolError('Missing destination for SEND command.')

        if dest.startswith('/queue/'):
            self.engine.queue_manager.send(frame)
        else:
            self.engine.topic_manager.send(frame)

    def subscribe(self, frame):
        """
        Handle the SUBSCRIBE command: Adds this connection to destination.
        """
        ack = frame.headers.get('ack')
        reliable = ack and ack.lower() == 'client'

        self.engine.connection.reliable_subscriber = reliable

        dest = frame.headers.get('destination')
        if not dest:
            raise ProtocolError('Missing destination for SUBSCRIBE command.')

        if dest.startswith('/queue/'):
            self.engine.queue_manager.subscribe(self.engine.connection, dest)
        else:
            self.engine.topic_manager.subscribe(self.engine.connection, dest)

    def unsubscribe(self, frame):
        """
        Handle the UNSUBSCRIBE command: Removes this connection from destination.
        """
        dest = frame.headers.get('destination')
        if not dest:
            raise ProtocolError('Missing destination for UNSUBSCRIBE command.')

        if dest.startswith('/queue/'):
            self.engine.queue_manager.unsubscribe(self.engine.connection, dest)
        else:
            self.engine.topic_manager.unsubscribe(self.engine.connection, dest)

    def begin(self, frame):
        """
        Handles BEGING command: Starts a new transaction.
        """
        if not frame.transaction:
            raise ProtocolError("Missing transaction for BEGIN command.")

        self.engine.transactions[frame.transaction] = []

    def commit(self, frame):
        """
        Handles COMMIT command: Commits specified transaction.
        """
        if not frame.transaction:
            raise ProtocolError("Missing transaction for COMMIT command.")

        if not frame.transaction in self.engine.transactions:
            raise ProtocolError("Invalid transaction: %s" % frame.transaction)

        for tframe in self.engine.transactions[frame.transaction]:
            del tframe.headers['transaction']
            self.process_frame(tframe)

        self.engine.queue_manager.clear_transaction_frames(
            self.engine.connection, frame.transaction)
        del self.engine.transactions[frame.transaction]

    def abort(self, frame):
        """
        Handles ABORT command: Rolls back specified transaction.
        """
        if not frame.transaction:
            raise ProtocolError("Missing transaction for ABORT command.")

        if not frame.transaction in self.engine.transactions:
            raise ProtocolError("Invalid transaction: %s" % frame.transaction)

        self.engine.queue_manager.resend_transaction_frames(
            self.engine.connection, frame.transaction)
        del self.engine.transactions[frame.transaction]

    def ack(self, frame):
        """
        Handles the ACK command: Acknowledges receipt of a message.
        """
        if not frame.message_id:
            raise ProtocolError("No message-id specified for ACK command.")
        self.engine.queue_manager.ack(self.engine.connection, frame)

    def disconnect(self, frame):
        """
        Handles the DISCONNECT command: Unbinds the connection.

        Clients are supposed to send this command, but in practice it should not be
        relied upon.
        """
        self.engine.log.debug("Disconnect")
        self.engine.unbind()


class STOMP11(STOMP10):

    SUPPORTED_VERSIONS = {'1.0', '1.1'}

    def __init__(self, engine, send_heartbeat_interval=100, receive_heartbeat_interval=100, *args, **kwargs):
        super(STOMP11, self).__init__(engine)
        self.last_hb = datetime.datetime.now()
        self.last_hb_sent = datetime.datetime.now()
        self.timer = CoilThreadingTimer()

        # flags to control heartbeating
        self.send_hb = self.receive_hb = False

        self.send_heartbeat_interval = datetime.timedelta(milliseconds=send_heartbeat_interval)
        self.receive_heartbeat_interval = datetime.timedelta(milliseconds=receive_heartbeat_interval)

    def enable_heartbeat(self, cx, cy, response):
        if self.send_heartbeat_interval and cy:
            self.send_heartbeat_interval = max(self.send_heartbeat_interval, datetime.timedelta(milliseconds=cy))
            self.timer.schedule(max(self.send_heartbeat_interval, datetime.timedelta(milliseconds=cy)).total_seconds(), self.send_heartbeat)
        if self.receive_heartbeat_interval and cx:
            self.timer.schedule(max(self.send_heartbeat_interval, datetime.timedelta(milliseconds=cx)).total_seconds(),
                                self.receive_heartbeat)
        self.timer.start()
        response.headers['heart-beat'] = '{0},{1}'.format(int(self.send_heartbeat_interval.microseconds / 1000),
                                                          int(self.receive_heartbeat_interval.microseconds / 1000))

    def disable_heartbeat(self):
        self.timer.stop()

    def send_heartbeat(self):
        # screw it, just send an error frame
        self.engine.connection.send_frame(ErrorFrame('heartbeat'))

    def receive_heartbeat(self):
        ago = datetime.datetime.now() - self.last_hb
        if ago > self.receive_heartbeat_interval:
            self.engine.log.debug("No heartbeat was received for {0} seconds".format(ago.total_seconds()))
            self.engine.unbind()

    def connect(self, frame, response=None):
        connected_frame = Frame(frames.CONNECTED)
        self._negotiate_protocol(frame, connected_frame)
        heart_beat = frame.headers.get('heart-beat', '0,0')
        if heart_beat:
            self.enable_heartbeat(*map(int, heart_beat.split(',')), response=connected_frame)
        super(STOMP11, self).connect(frame, response=connected_frame)

    def nack(self, frame):
        """
        Handles the NACK command: Unacknowledges receipt of a message.
        For now, this is just a placeholder to implement this version of the protocol
        """
        if not frame.headers.get('message-id'):
            raise ProtocolError("No message-id specified for NACK command.")
        if not frame.headers.get('subscription'):
            raise ProtocolError("No subscription specified for NACK command.")

    def _negotiate_protocol(self, frame, response):
        client_versions = frame.headers.get('accept-version')
        if not client_versions:
            raise ProtocolError('No version specified')
        common = set(client_versions.split(',')) & self.SUPPORTED_VERSIONS
        if not common:
            versions = ','.join(self.SUPPORTED_VERSIONS)
            self.engine.connection.send_frame(Frame(
                    frames.ERROR,
                    headers={'version': versions, 'content-type': frames.TEXT_PLAIN},
                    body='Supported protocol versions are {0}'.format(versions)
            ))
        else:
            response.headers['version'] = max(common)
            protocol_class = PROTOCOL_MAP[response.headers['version']]
            if type(self) is not protocol_class:
                self.engine.protocol = protocol_class(self.engine)
                self.engine.protocol.connect(frame, response=response)


class STOMP12(STOMP11):

    SUPPORTED_VERSIONS = STOMP11.SUPPORTED_VERSIONS.union({'1.2', })

    def connect(self, frame, response=None):
        host = frame.headers.get('host')
        if not host:
            raise ProtocolError('"host" header is required')
        if host != socket.getfqdn():
            raise ProtocolError('Virtual hosting is not supported or host is unknown')
        super(STOMP12, self).connect(frame, response)


PROTOCOL_MAP = {'1.0': STOMP10, '1.1': STOMP11, '1.2': STOMP12}
