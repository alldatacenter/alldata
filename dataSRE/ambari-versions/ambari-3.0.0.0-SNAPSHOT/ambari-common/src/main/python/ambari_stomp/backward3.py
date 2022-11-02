"""
Python3-specific versions of various functions used by stomp.py
"""

NULL = b'\x00'


def input_prompt(prompt):
    """
    Get user input

    :param str prompt: the prompt to display to the user

    :rtype: str
    """
    return input(prompt)


def decode(byte_data):
    """
    Decode the byte data to a string if not None.

    :param bytes byte_data: the data to decode

    :rtype: str
    """
    if byte_data is None:
        return None
    return byte_data.decode()


def encode(char_data):
    """
    Encode the parameter as a byte string.

    :param char_data: the data to encode

    :rtype: bytes
    """
    if type(char_data) is str:
        return char_data.encode()
    elif type(char_data) is bytes:
        return char_data
    else:
        raise TypeError('message should be a string or bytes')


def pack(pieces=()):
    """
    Join a sequence of strings together.

    :param list pieces: list of strings

    :rtype: bytes
    """
    encoded_pieces = (encode(piece) for piece in pieces)
    return b''.join(encoded_pieces)


def join(chars=()):
    """
    Join a sequence of characters into a string.

    :param bytes chars: list of chars

    :rtype: str
    """
    return b''.join(chars).decode()
