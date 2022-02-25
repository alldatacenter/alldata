"""
Python2-specific versions of various functions used by stomp.py
"""

NULL = '\x00'


def input_prompt(prompt):
    """
    Get user input

    :rtype: str
    """
    return raw_input(prompt)


def decode(byte_data):
    """
    Decode the byte data to a string - in the case of this Py2 version, we can't really do anything (Py3 differs).

    :param bytes byte_data:

    :rtype: str
    """
    return byte_data  # no way to know if it's unicode or not, so just pass through unmolested


def encode(char_data):
    """
    Encode the parameter as a byte string.

    :param char_data:

    :rtype: bytes
    """
    if type(char_data) is unicode:
        return char_data.encode('utf-8')
    else:
        return char_data


def pack(pieces=()):
    """
    Join a sequence of strings together (note: py3 version differs)

    :param list pieces:

    :rtype: bytes
    """
    return ''.join(encode(p) for p in pieces)


def join(chars=()):
    """
    Join a sequence of characters into a string.

    :param bytes chars:

    :rtype str:
    """
    return ''.join(chars)
