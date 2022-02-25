"""Provides the 1.0, 1.1 and 1.2 protocol classes.
"""

import uuid

from ambari_stomp.backward import encode
from ambari_stomp.constants import *
from ambari_stomp.exception import ConnectFailedException
from ambari_stomp.listener import *
import ambari_stomp.utils as utils


log = logging.getLogger('stomp.py')


class Protocol10(ConnectionListener):
    """
    Represents version 1.0 of the protocol (see https://stomp.github.io/stomp-specification-1.0.html).

    Most users should not instantiate the protocol directly. See :py:mod:`stomp.connect` for connection classes.

    :param transport:
    :param bool auto_content_length: Whether to calculate and send the content-length header automatically if it has not been set
    """
    def __init__(self, transport, auto_content_length=True):
        self.transport = transport
        self.auto_content_length = auto_content_length
        transport.set_listener('protocol-listener', self)
        self.version = '1.0'

    def send_frame(self, cmd, headers=None, body=''):
        """
        Encode and send a stomp frame
        through the underlying transport.

        :param str cmd: the protocol command
        :param dict headers: a map of headers to include in the frame
        :param body: the content of the message
        """
        frame = utils.Frame(cmd, headers, body)
        self.transport.transmit(frame)

    def abort(self, transaction, headers=None, **keyword_headers):
        """
        Abort a transaction.

        :param str transaction: the identifier of the transaction
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert transaction is not None, "'transaction' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_ABORT, headers)

    def ack(self, id, transaction=None):
        """
        Acknowledge 'consumption' of a message by id.

        :param str id: identifier of the message
        :param str transaction: include the acknowledgement in the specified transaction
        """
        assert id is not None, "'id' is required"
        headers = {HDR_MESSAGE_ID: id}
        if transaction:
            headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_ACK, headers)

    def begin(self, transaction=None, headers=None, **keyword_headers):
        """
        Begin a transaction.

        :param str transaction: the identifier for the transaction (optional - if not specified
            a unique transaction id will be generated)
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires

        :return: the transaction id
        :rtype: str
        """
        headers = utils.merge_headers([headers, keyword_headers])
        if not transaction:
            transaction = str(uuid.uuid4())
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_BEGIN, headers)
        return transaction

    def commit(self, transaction=None, headers=None, **keyword_headers):
        """
        Commit a transaction.

        :param str transaction: the identifier for the transaction
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert transaction is not None, "'transaction' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_COMMIT, headers)

    def connect(self, username=None, passcode=None, wait=False, headers=None, **keyword_headers):
        """
        Start a connection.

        :param str username: the username to connect with
        :param str passcode: the password used to authenticate with
        :param bool wait: if True, wait for the connection to be established/acknowledged
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        cmd = CMD_CONNECT
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_ACCEPT_VERSION] = self.version

        if username is not None:
            headers[HDR_LOGIN] = username

        if passcode is not None:
            headers[HDR_PASSCODE] = passcode

        self.send_frame(cmd, headers)

        if wait:
            self.transport.wait_for_connection()
            if self.transport.connection_error:
                raise ConnectFailedException()

    def disconnect(self, receipt=None, headers=None, **keyword_headers):
        """
        Disconnect from the server.

        :param str receipt: the receipt to use (once the server acknowledges that receipt, we're
            officially disconnected; optional - if not specified a unique receipt id will
            be generated)
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        if not self.transport.is_connected():
            log.debug('Not sending disconnect, already disconnected')
            return
        headers = utils.merge_headers([headers, keyword_headers])
        rec = receipt or str(uuid.uuid4())
        headers[HDR_RECEIPT] = rec
        self.set_receipt(rec, CMD_DISCONNECT)
        self.send_frame(CMD_DISCONNECT, headers)

    def send(self, destination, body, content_type=None, headers=None, **keyword_headers):
        """
        Send a message to a destination.

        :param str destination: the destination of the message (e.g. queue or topic name)
        :param body: the content of the message
        :param str content_type: the content type of the message
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert destination is not None, "'destination' is required"
        assert body is not None, "'body' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_DESTINATION] = destination
        if content_type:
            headers[HDR_CONTENT_TYPE] = content_type
        body = encode(body)
        if self.auto_content_length and body and HDR_CONTENT_LENGTH not in headers:
            headers[HDR_CONTENT_LENGTH] = len(body)
        self.send_frame(CMD_SEND, headers, body)

    def subscribe(self, destination, id=None, ack='auto', headers=None, **keyword_headers):
        """
        Subscribe to a destination.

        :param str destination: the topic or queue to subscribe to
        :param str id: a unique id to represent the subscription
        :param str ack: acknowledgement mode, either auto, client, or client-individual
            (see http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header)
            for more information
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert destination is not None, "'destination' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_DESTINATION] = destination
        if id:
            headers[HDR_ID] = id
        headers[HDR_ACK] = ack
        self.send_frame(CMD_SUBSCRIBE, headers)

    def unsubscribe(self, destination=None, id=None, headers=None, **keyword_headers):
        """
        Unsubscribe from a destination by either id or the destination name.

        :param str destination: the name of the topic or queue to unsubscribe from
        :param str id: the unique identifier of the topic or queue to unsubscribe from
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert id is not None or destination is not None, "'id' or 'destination' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        if id:
            headers[HDR_ID] = id
        if destination:
            headers[HDR_DESTINATION] = destination
        self.send_frame(CMD_UNSUBSCRIBE, headers)


class Protocol11(HeartbeatListener, ConnectionListener):
    """
    Represents version 1.1 of the protocol (see https://stomp.github.io/stomp-specification-1.1.html).

    Most users should not instantiate the protocol directly. See :py:mod:`stomp.connect` for connection classes.

    :param transport:
    :param (int,int) heartbeats:
    :param bool auto_content_length: Whether to calculate and send the content-length header automatically if it has not been set
    """
    def __init__(self, transport, heartbeats=(0, 0), auto_content_length=True):
        HeartbeatListener.__init__(self, heartbeats)
        self.transport = transport
        self.auto_content_length = auto_content_length
        transport.set_listener('protocol-listener', self)
        self.version = '1.1'

    def _escape_headers(self, headers):
        """
        :param dict(str,str) headers:
        """
        for key, val in headers.items():
            try:
                val = val.replace('\\', '\\\\').replace('\n', '\\n').replace(':', '\\c')
            except:
                pass
            headers[key] = val

    def send_frame(self, cmd, headers=None, body=''):
        """
        Encode and send a stomp frame
        through the underlying transport:

        :param str cmd: the protocol command
        :param dict headers: a map of headers to include in the frame
        :param body: the content of the message
        """
        if cmd != CMD_CONNECT:
            if headers is None:
                headers = {}
            self._escape_headers(headers)
        frame = utils.Frame(cmd, headers, body)
        self.transport.transmit(frame)

    def abort(self, transaction, headers=None, **keyword_headers):
        """
        Abort a transaction.

        :param str transaction: the identifier of the transaction
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert transaction is not None, "'transaction' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_ABORT, headers)

    def ack(self, id, subscription, transaction=None):
        """
        Acknowledge 'consumption' of a message by id.

        :param str id: identifier of the message
        :param str subscription: the subscription this message is associated with
        :param str transaction: include the acknowledgement in the specified transaction
        """
        assert id is not None, "'id' is required"
        assert subscription is not None, "'subscription' is required"
        headers = {HDR_MESSAGE_ID: id, HDR_SUBSCRIPTION: subscription}
        if transaction:
            headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_ACK, headers)

    def begin(self, transaction=None, headers=None, **keyword_headers):
        """
        Begin a transaction.

        :param str transaction: the identifier for the transaction (optional - if not specified
            a unique transaction id will be generated)
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires

        :return: the transaction id
        :rtype: str
        """
        headers = utils.merge_headers([headers, keyword_headers])
        if not transaction:
            transaction = str(uuid.uuid4())
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_BEGIN, headers)
        return transaction

    def commit(self, transaction=None, headers=None, **keyword_headers):
        """
        Commit a transaction.

        :param str transaction: the identifier for the transaction
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        assert transaction is not None, "'transaction' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_COMMIT, headers)

    def connect(self, username=None, passcode=None, wait=False, headers=None, **keyword_headers):
        """
        Start a connection.

        :param str username: the username to connect with
        :param str passcode: the password used to authenticate with
        :param bool wait: if True, wait for the connection to be established/acknowledged
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        cmd = CMD_STOMP
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_ACCEPT_VERSION] = self.version

        if self.transport.vhost:
            headers[HDR_HOST] = self.transport.vhost

        if username is not None:
            headers[HDR_LOGIN] = username

        if passcode is not None:
            headers[HDR_PASSCODE] = passcode

        self.send_frame(cmd, headers)

        if wait:
            self.transport.wait_for_connection()
            if self.transport.connection_error:
                raise ConnectFailedException()

    def disconnect(self, receipt=None, headers=None, **keyword_headers):
        """
        Disconnect from the server.

        :param str receipt: the receipt to use (once the server acknowledges that receipt, we're
            officially disconnected; optional - if not specified a unique receipt id will
            be generated)
        :param dict headers: a map of any additional headers the broker requires
        :param keyword_headers: any additional headers the broker requires
        """
        if not self.transport.is_connected():
            log.debug('Not sending disconnect, already disconnected')
            return
        headers = utils.merge_headers([headers, keyword_headers])
        rec = receipt or str(uuid.uuid4())
        headers[HDR_RECEIPT] = rec
        self.set_receipt(rec, CMD_DISCONNECT)
        self.send_frame(CMD_DISCONNECT, headers)

    def nack(self, id, subscription, transaction=None):
        """
        Let the server know that a message was not consumed.

        :param str id: the unique id of the message to nack
        :param str subscription: the subscription this message is associated with
        :param str transaction: include this nack in a named transaction
        """
        assert id is not None, "'id' is required"
        assert subscription is not None, "'subscription' is required"
        headers = {HDR_MESSAGE_ID: id, HDR_SUBSCRIPTION: subscription}
        if transaction:
            headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_NACK, headers)

    def send(self, destination, body, content_type=None, headers=None, **keyword_headers):
        """
        Send a message to a destination in the messaging system (as per https://stomp.github.io/stomp-specification-1.2.html#SEND)

        :param str destination: the destination (such as a message queue - for example '/queue/test' - or a message topic)
        :param body: the content of the message
        :param str content_type: the MIME type of message
        :param dict headers: additional headers to send in the message frame
        :param keyword_headers: any additional headers the broker requires
        """
        assert destination is not None, "'destination' is required"
        assert body is not None, "'body' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_DESTINATION] = destination
        if content_type:
            headers[HDR_CONTENT_TYPE] = content_type
        body = encode(body)
        if self.auto_content_length and body and HDR_CONTENT_LENGTH not in headers:
            headers[HDR_CONTENT_LENGTH] = len(body)
        self.send_frame(CMD_SEND, headers, body)

    def subscribe(self, destination, id, ack='auto', headers=None, **keyword_headers):
        """
        Subscribe to a destination

        :param str destination: the topic or queue to subscribe to
        :param str id: the identifier to uniquely identify the subscription
        :param str ack: either auto, client or client-individual (see https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE for more info)
        :param dict headers: a map of any additional headers to send with the subscription
        :param keyword_headers: any additional headers to send with the subscription
        """
        assert destination is not None, "'destination' is required"
        assert id is not None, "'id' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_DESTINATION] = destination
        headers[HDR_ID] = id
        headers[HDR_ACK] = ack
        self.send_frame(CMD_SUBSCRIBE, headers)

    def unsubscribe(self, id, headers=None, **keyword_headers):
        """
        Unsubscribe from a destination by its unique identifier

        :param str id: the unique identifier to unsubscribe from
        :param dict headers: additional headers to send with the unsubscribe
        :param keyword_headers: any additional headers to send with the subscription
        """
        assert id is not None, "'id' is required"
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_ID] = id
        self.send_frame(CMD_UNSUBSCRIBE, headers)


class Protocol12(Protocol11):
    """
    Represents version 1.2 of the protocol (see https://stomp.github.io/stomp-specification-1.2.html).

    Most users should not instantiate the protocol directly. See :py:mod:`stomp.connect` for connection classes.

    :param transport:
    :param (int,int) heartbeats:
    :param bool auto_content_length: Whether to calculate and send the content-length header automatically if it has not been set
    """
    def __init__(self, transport, heartbeats=(0, 0), auto_content_length=True):
        Protocol11.__init__(self, transport, heartbeats, auto_content_length)
        self.version = '1.2'

    def _escape_headers(self, headers):
        """
        :param dict(str,str) headers:
        """
        for key, val in headers.items():
            try:
                val = val.replace('\\', '\\\\').replace('\n', '\\n').replace(':', '\\c').replace('\r', '\\r')
            except:
                pass
            headers[key] = val

    def ack(self, id, transaction=None):
        """
        Acknowledge 'consumption' of a message by id.

        :param str id: identifier of the message
        :param str transaction: include the acknowledgement in the specified transaction
        """
        assert id is not None, "'id' is required"
        headers = {HDR_ID: id}
        if transaction:
            headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_ACK, headers)

    def nack(self, id, transaction=None):
        """
        Let the server know that a message was not consumed.

        :param str id: the unique id of the message to nack
        :param str transaction: include this nack in a named transaction
        """
        assert id is not None, "'id' is required"
        headers = {HDR_ID: id}
        if transaction:
            headers[HDR_TRANSACTION] = transaction
        self.send_frame(CMD_NACK, headers)

    def connect(self, username=None, passcode=None, wait=False, headers=None, **keyword_headers):
        """
        Send a STOMP CONNECT frame. Differs from 1.0 and 1.1 versions in that the HOST header is enforced.

        :param str username: optionally specify the login user
        :param str passcode: optionally specify the user password
        :param bool wait: wait for the connection to complete before returning
        :param dict headers: a map of any additional headers to send with the subscription
        :param keyword_headers: any additional headers to send with the subscription
        """
        cmd = CMD_STOMP
        headers = utils.merge_headers([headers, keyword_headers])
        headers[HDR_ACCEPT_VERSION] = self.version
        headers[HDR_HOST] = self.transport.current_host_and_port[0]

        if self.transport.vhost:
            headers[HDR_HOST] = self.transport.vhost

        if username is not None:
            headers[HDR_LOGIN] = username

        if passcode is not None:
            headers[HDR_PASSCODE] = passcode

        self.send_frame(cmd, headers)

        if wait:
            self.transport.wait_for_connection()
            if self.transport.connection_error:
                raise ConnectFailedException()
