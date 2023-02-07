class Address:
    host = 'localhost'
    port = '9666'

    def __init__(self, host=None, port=None):
        if host is not None:
            self.host = host
        if port is not None:
            self.port = port

    def is_empty(self) -> bool:
        return self.host == '' or self.port == ''

    def string(self) -> str:
        return f'{self.host}:{self.port}'


def new_address_from_string(address: str) -> Address:
    parts = address.split(':')
    if len(parts) == 1:
        return Address(host=parts[0])
    elif len(parts) == 2:
        return Address(host=parts[0], port=parts[1])
    else:
        raise Exception(f'parsing address error: {address}')
