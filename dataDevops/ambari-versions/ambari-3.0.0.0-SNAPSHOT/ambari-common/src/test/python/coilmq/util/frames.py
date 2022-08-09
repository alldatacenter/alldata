from functools import partial
import re
import logging
from collections import OrderedDict
import io

import six

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
NACK = 'NACK'
DISCONNECT = 'DISCONNECT'

VALID_COMMANDS = ['message', 'connect', 'connected', 'error', 'send',
                  'subscribe', 'unsubscribe', 'begin', 'commit', 'abort', 'ack', 'disconnect', 'nack']

TEXT_PLAIN = 'text/plain'


class IncompleteFrame(Exception):
    """The frame has incomplete body"""


class BodyNotTerminated(Exception):
    """The frame's body is not terminated with the NULL character"""


class EmptyBuffer(Exception):
    """The buffer is empty"""


def parse_headers(buff):
    """
    Parses buffer and returns command and headers as strings
    """
    preamble_lines = list(map(
        lambda x: six.u(x).decode(),
        iter(lambda: buff.readline().strip(), b''))
    )
    if not preamble_lines:
        raise EmptyBuffer()
    return preamble_lines[0], OrderedDict([l.split(':') for l in preamble_lines[1:]])


def parse_body(buff, headers):
    content_length = int(headers.get('content-length', -1))
    body = buff.read(content_length)
    if content_length >= 0:
        if len(body) < content_length:
            raise IncompleteFrame()
        terminator = six.u(buff.read(1)).decode()
        if not terminator:
            raise BodyNotTerminated()
    else:
        # no content length
        body, terminator, rest = body.partition(b'\x00')
        if not terminator:
            raise BodyNotTerminated()
        else:
            buff.seek(-len(rest), 2)

    return body


class Frame(object):
    """
    A STOMP frame (or message).

    :param cmd: the protocol command
    :param headers: a map of headers for the frame
    :param body: the content of the frame.
    """

    def __init__(self, cmd, headers=None, body=None):
        self.cmd = cmd
        self.headers = headers or {}
        self.body = body or ''

    def __str__(self):
        return '{{cmd={0},headers=[{1}],body={2}}}'.format(
            self.cmd,
            self.headers,
            self.body if isinstance(
                self.body, six.binary_type) else six.b(self.body)
        )

    def __eq__(self, other):
        """ Override equality checking to test for matching command, headers, and body. """
        return all([isinstance(other, Frame),
                    self.cmd == other.cmd,
                    self.headers == other.headers,
                    self.body == other.body])

    @property
    def transaction(self):
        return self.headers.get('transaction')

    @classmethod
    def from_buffer(cls, buff):
        cmd, headers = parse_headers(buff)
        body = parse_body(buff, headers)
        return cls(cmd, headers=headers, body=body)

    def pack(self):
        """
        Create a string representation from object state.

        @return: The string (bytes) for this stomp frame.
        @rtype: C{str}
        """

        self.headers.setdefault('content-length', len(self.body))

        # Convert and append any existing headers to a string as the
        # protocol describes.
        headerparts = ("{0}:{1}\n".format(key, value)
                       for key, value in self.headers.items())

        # Frame is Command + Header + EOF marker.
        return six.b("{0}\n{1}\n".format(self.cmd, "".join(headerparts))) + (self.body if isinstance(self.body, six.binary_type) else six.b(self.body)) + six.b('\x00')


class ConnectedFrame(Frame):
    """ A CONNECTED server frame (response to CONNECT).

    @ivar session: The (throw-away) session ID to include in response.
    @type session: C{str}
    """

    def __init__(self, session, extra_headers=None):
        """
        @param session: The (throw-away) session ID to include in response.
        @type session: C{str}
        """
        super(ConnectedFrame, self).__init__(
            cmd='connected', headers=extra_headers or {})
        self.headers['session'] = session


class HeaderValue(object):
    """
    An descriptor class that can be used when a calculated header value is needed.

    This class is a descriptor, implementing  __get__ to return the calculated value.
    While according to  U{http://docs.codehaus.org/display/STOMP/Character+Encoding} there
    seems to some general idea about having UTF-8 as the character encoding for headers;
    however the C{stomper} lib does not support this currently.

    For example, to use this class to generate the content-length header:

        >>> body = 'asdf'
        >>> headers = {}
        >>> headers['content-length'] = HeaderValue(calculator=lambda: len(body))
        >>> str(headers['content-length'])
        '4'

    @ivar calc: The calculator function.
    @type calc: C{callable}
    """

    def __init__(self, calculator):
        """
        @param calculator: The calculator callable that will yield the desired value.
        @type calculator: C{callable}
        """
        if not callable(calculator):
            raise ValueError("Non-callable param: %s" % calculator)
        self.calc = calculator

    def __get__(self, obj, objtype):
        return self.calc()

    def __str__(self):
        return str(self.calc())

    def __set__(self, obj, value):
        self.calc = value

    def __repr__(self):
        return '<%s calculator=%s>' % (self.__class__.__name__, self.calc)


class ErrorFrame(Frame):
    """ An ERROR server frame. """

    def __init__(self, message, body=None, extra_headers=None):
        """
        @param body: The message body bytes.
        @type body: C{str}
        """
        super(ErrorFrame, self).__init__(cmd='error',
                                         headers=extra_headers or {}, body=body)
        self.headers['message'] = message
        self.headers[
            'content-length'] = HeaderValue(calculator=lambda: len(self.body))

    def __repr__(self):
        return '<%s message=%r>' % (self.__class__.__name__, self.headers['message'])


class ReceiptFrame(Frame):
    """ A RECEIPT server frame. """

    def __init__(self, receipt, extra_headers=None):
        """
        @param receipt: The receipt message ID.
        @type receipt: C{str}
        """
        super(ReceiptFrame, self).__init__(
            'RECEIPT', headers=extra_headers or {})
        self.headers['receipt-id'] = receipt


class FrameBuffer(object):
    """
    A customized version of the StompBuffer class from Stomper project that returns frame objects
    and supports iteration.

    This version of the parser also assumes that stomp messages with no content-lengh
    end in a simple \\x00 char, not \\x00\\n as is assumed by
    C{stomper.stompbuffer.StompBuffer}. Additionally, this class differs from Stomper version
    by conforming to PEP-8 coding style.

    This class can be used to smooth over a transport that may provide partial frames (or
    may provide multiple frames in one data buffer).

    @ivar _buffer: The internal byte buffer.
    @type _buffer: C{str}

    @ivar debug: Log extra parsing debug (logs will be DEBUG level).
    @type debug: C{bool}
    """

    # regexp to check that the buffer starts with a command.
    command_re = re.compile('^(.+?)\n')

    # regexp to remove everything up to and including the first
    # instance of '\x00' (used in resynching the buffer).
    sync_re = re.compile('^.*?\x00')

    # regexp to determine the content length. The buffer should always start
    # with a command followed by the headers, so the content-length header will
    # always be preceded by a newline.  It may not always proceeded by a
    # newline, though!
    content_length_re = re.compile('\ncontent-length\s*:\s*(\d+)\s*(\n|$)')

    def __init__(self):
        self._buffer = io.BytesIO()
        self._pointer = 0
        self.debug = False
        self.log = logging.getLogger('%s.%s' % (
            self.__module__, self.__class__.__name__))

    def clear(self):
        """
        Clears (empties) the internal buffer.
        """
        self._buffer = io

    def buffer_len(self):
        """
        @return: Number of bytes in the internal buffer.
        @rtype: C{int}
        """
        return len(self._buffer)

    def buffer_empty(self):
        """
        @return: C{True} if buffer is empty, C{False} otherwise.
        @rtype: C{bool}
        """
        return not bool(self._buffer)

    def append(self, data):
        """
        Appends bytes to the internal buffer (may or may not contain full stomp frames).

        @param data: The bytes to append.
        @type data: C{str}
        """
        self._buffer.write(data)

    def extract_frame(self):
        """
        Pulls one complete frame off the buffer and returns it.

        If there is no complete message in the buffer, returns None.

        Note that the buffer can contain more than once message. You
        should therefore call this method in a loop (or use iterator
        functionality exposed by class) until None returned.

        @return: The next complete frame in the buffer.
        @rtype: L{stomp.frame.Frame}
        """
        # (mbytes, hbytes) = self._find_message_bytes(self.buffer)
        # if not mbytes:
        #     return None
        #
        # msgdata = self.buffer[:mbytes]
        # self.buffer = self.buffer[mbytes:]
        # hdata = msgdata[:hbytes]
        # # Strip off any leading whitespace from headers; this is necessary, because
        # # we do not (any longer) expect a trailing \n after the \x00 byte (which means
        # # it will become a leading \n to the next frame).
        # hdata = hdata.lstrip()
        # elems = hdata.split('\n')
        # cmd = elems.pop(0)
        # headers = {}
        #
        # for e in elems:
        #     try:
        #         (k,v) = e.split(':', 1) # header values may contain ':' so specify maxsplit
        #     except ValueError:
        #         continue
        #     headers[k.strip()] = v.strip()
        #
        # # hbytes points to the start of the '\n\n' at the end of the header,
        # # so 2 bytes beyond this is the start of the body. The body EXCLUDES
        # # the final byte, which is  '\x00'.
        # body = msgdata[hbytes + 2:-1]
        self._buffer.seek(self._pointer, 0)
        try:
            f = Frame.from_buffer(self._buffer)
            self._pointer = self._buffer.tell()
        except (IncompleteFrame, EmptyBuffer):
            self._buffer.seek(self._pointer, 0)
            return None

        return f

    def __iter__(self):
        """
        Returns an iterator object.
        """
        return self

    def __next__(self):
        """
        Return the next STOMP message in the buffer (supporting iteration).

        @rtype: L{stomp.frame.Frame}
        """
        msg = self.extract_frame()
        if not msg:
            raise StopIteration()
        return msg

    def next(self):
        return self.__next__()
