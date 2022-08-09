"""
Exception classes used by CoilMQ.

CoilMQ exceptions extend C{RuntimeError} or other appropriate sub-classes.  These will be
thrown if there is not a more appropriate error class already provided by builtins.
"""
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


class ProtocolError(RuntimeError):
    """
    Represents an error at the STOMP protocol layer.
    """


class ConfigError(RuntimeError):
    """
    Represents an error in the configuration of the application.
    """


class AuthError(RuntimeError):
    """
    Represents an authentication or authorization error.
    """


class ClientDisconnected(Exception):
    """
    A signal that client has disconnected (so we shouldn't try to keep reading from the client).
    """
