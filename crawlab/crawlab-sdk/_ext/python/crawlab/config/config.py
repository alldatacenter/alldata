import binascii
import json
import os

from crawlab.constants.upload import CLI_DEFAULT_CONFIG_ROOT_DIR, CLI_DEFAULT_CONFIG_CLI_DIR, \
    CLI_DEFAULT_CONFIG_FILE_NAME, \
    CLI_DEFAULT_CONFIG_KEY_PASSWORD

home = os.curdir

if 'HOME' in os.environ:
    home = os.environ['HOME']
elif os.name == 'posix':
    home = os.path.expanduser('~/')
elif os.name == 'nt':
    if 'HOMEPATH' in os.environ and 'HOMEDRIVE' in os.environ:
        home = os.environ['HOMEDRIVE'] + os.environ['HOMEPATH']
elif 'HOMEPATH' in os.environ:
    home = os.environ['HOMEPATH']

CRAWLAB_ROOT_PATH = os.path.join(home, CLI_DEFAULT_CONFIG_ROOT_DIR)
CRAWLAB_CLI_PATH = os.path.join(CRAWLAB_ROOT_PATH, CLI_DEFAULT_CONFIG_CLI_DIR)

if not os.path.exists(CRAWLAB_ROOT_PATH):
    os.mkdir(CRAWLAB_ROOT_PATH)

if not os.path.exists(CRAWLAB_CLI_PATH):
    os.mkdir(CRAWLAB_CLI_PATH)


class ConfigData(dict):
    @property
    def json(self):
        return json.dumps(self)

    @property
    def password(self):
        if self.get(CLI_DEFAULT_CONFIG_KEY_PASSWORD) is not None:
            return binascii.a2b_hex(self.get(CLI_DEFAULT_CONFIG_KEY_PASSWORD)).decode()


class Config(object):
    json_path = os.path.join(CRAWLAB_CLI_PATH, CLI_DEFAULT_CONFIG_FILE_NAME)
    data = ConfigData()

    def __init__(self):
        if os.path.exists(self.json_path):
            self.load()
        else:
            self.save()

    def load(self):
        with open(self.json_path) as f:
            data_str = f.read()
            self.data = ConfigData(json.loads(data_str))

    def save(self):
        with open(self.json_path, 'wb') as f:
            f.write(self.data.json.encode())

    def set(self, key: str, value: str):
        # encode password
        if key == CLI_DEFAULT_CONFIG_KEY_PASSWORD:
            value = binascii.b2a_hex(value.encode()).decode()

        self.data[key] = value

    def unset(self, key: str):
        del self.data[key]


config = Config()
