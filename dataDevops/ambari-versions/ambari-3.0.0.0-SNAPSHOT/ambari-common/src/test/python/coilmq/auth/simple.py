"""
A simple config-file based authentication module.
"""
try:
    from configparser import ConfigParser
except ImportError:
    from ConfigParser import ConfigParser
    ConfigParser.read_file = ConfigParser.readfp

from coilmq.auth import Authenticator
from coilmq.config import config
from coilmq.exception import ConfigError

__authors__ = ['"Hans Lellelid" <hans@xmpl.org>']
__copyright__ = "Copyright 2009 Hans Lellelid"
__license__ = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""


def make_simple():
    """
    Create a L{SimpleAuthenticator} instance using values read from coilmq configuration.

    @return: The configured L{SimpleAuthenticator}
    @rtype: L{SimpleAuthenticator}
    @raise ConfigError: If there is a configuration error.
    """
    authfile = config.get('coilmq', 'auth.simple.file')
    if not authfile:
        raise ConfigError('Missing configuration parameter: auth.simple.file')
    sa = SimpleAuthenticator()
    sa.from_configfile(authfile)
    return sa


class SimpleAuthenticator(Authenticator):
    """
    A simple configfile-based authenticator.

    @ivar store:  Authentication key-value store (of logins to passwords).
    @type store: C{dict} of C{str} to C{str}
    """

    def __init__(self, store=None):
        """
        Initialize the authenticator to use (optionally) specified C{dict} store.

        @param store:  Authentication store, C{dict} of logins to passwords.
        @type store: C{dict} of C{str} to C{str}
        """
        if store is None:
            store = {}
        self.store = store

    def from_configfile(self, configfile):
        """
        Initialize the authentication store from a "config"-style file.

        Auth "config" file is parsed with C{ConfigParser.RawConfigParser} and must contain
        an [auth] section which contains the usernames (keys) and passwords (values).

        Example auth file::

            [auth]
            someuser = somepass
            anotheruser = anotherpass

        @param configfile: Path to config file or file-like object.
        @type configfile: C{any}
        @raise ValueError: If file could not be read or does not contain [auth] section.
        """
        cfg = ConfigParser()
        if hasattr(configfile, 'read'):
            cfg.read_file(configfile)
        else:
            filesread = cfg.read(configfile)
            if not filesread:
                raise ValueError('Could not parse auth file: %s' % configfile)

        if not cfg.has_section('auth'):
            raise ValueError('Config file contains no [auth] section.')

        self.store = dict(cfg.items('auth'))

    def authenticate(self, login, passcode):
        """
        Authenticate the login and passcode.

        @return: Whether provided login and password match values in store.
        @rtype: C{bool}
        """
        return login in self.store and self.store[login] == passcode
