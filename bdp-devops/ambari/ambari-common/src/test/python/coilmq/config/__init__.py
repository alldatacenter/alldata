"""
Configuration support functionality.

The global C{config} object (C{ConfigParser.SafeConfigParser} instance) is initialized
with default configuration from the defaults.cfg file, which is located in this package.
In order to ensure that the config contains custom values, you must call the C{init_config}
function during application initialization:

from coilmq.config import config, init_config
init_config('/path/to/config.cfg')

config.getint('listen_port')
"""
import os.path
import logging
import logging.config
import warnings
import io

try:
    from configparser import ConfigParser
except ImportError:
    from ConfigParser import ConfigParser


from pkg_resources import resource_filename, resource_stream
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

config = ConfigParser()
config.read(os.path.join(os.path.dirname(__file__), 'defaults.cfg'))


def init_config(config_file=None):
    """
    Initialize the configuration from a config file.

    The values in config_file will override those already loaded from the default
    configuration file (defaults.cfg, in current package).

    This method does not setup logging.

    @param config_file: The path to a configuration file.
    @type config_file: C{str}

    @raise ValueError: if the specified config_file could not be read.
    @see: L{init_logging}  
    """
    global config

    if config_file and os.path.exists(config_file):
        read = config.read([config_file])
        if not read:
            raise ValueError(
                "Could not read configuration from file: %s" % config_file)


def init_logging(logfile=None, loglevel=logging.INFO, configfile=None):
    """
    Configures the logging using either basic filename + loglevel or passed config file path.

    This is performed separately from L{init_config()} in order to support the case where 
    logging should happen independent of (usu. *after*) other aspects of the configuration 
    initialization. For example, if logging may need to be initialized within a  daemon 
    context.

    @param logfile: An explicitly specified logfile destination.  If this is specified in addition
                    to default logging, a warning will be issued.
    @type logfile: C{str}

    @param loglevel: Which level to use when logging to explicitly specified file or stdout.
    @type loglevel: C{int}

    @param configfile: The path to a configuration file.  This takes precedence over any explicitly
                        specified logfile/loglevel (but a warning will be logged if both are specified).
                        If the file is not specified or does not exist annd no logfile was specified, 
                        then the default.cfg configuration file will be used to initialize logging.
    @type configfile: C{str}
    """
    # If a config file was specified, we will use that in place of the
    # explicitly
    use_configfile = False
    if configfile and os.path.exists(configfile):
        testcfg = ConfigParser()
        read = testcfg.read(configfile)
        use_configfile = (read and testcfg.has_section('loggers'))

    if use_configfile:
        logging.config.fileConfig(configfile)
        if logfile:
            msg = "Config file conflicts with explicitly specified logfile; config file takes precedence."
            logging.warn(msg)
    else:
        format = '%(asctime)s [%(threadName)s] %(name)s - %(levelname)s - %(message)s'
        if logfile:
            logging.basicConfig(
                filename=logfile, level=loglevel, format=format)
        else:
            logging.basicConfig(level=loglevel, format=format)


def resolve_name(name):
    """
    Resolve a dotted name to some object (usually class, module, or function).

    Supported naming formats include:
        1. path.to.module:method
        2. path.to.module.ClassName

    >>> resolve_name('coilmq.store.memory.MemoryQueue')
    <class 'coilmq.store.memory.MemoryQueue'>
    >>> t = resolve_name('coilmq.store.dbm.make_dbm')
    >>> import inspect
    >>> inspect.isfunction(t)
    True
    >>> t.__name__
    'make_dbm'

    @param name: The dotted name (e.g. path.to.MyClass)
    @type name: C{str}

    @return: The resolved object (class, callable, etc.) or None if not found.
    """
    if ':' in name:
        # Normalize foo.bar.baz:main to foo.bar.baz.main
        # (since our logic below will handle that)
        name = '%s.%s' % tuple(name.split(':'))

    name = name.split('.')

    used = name.pop(0)
    found = __import__(used)
    for n in name:
        used = used + '.' + n
        try:
            found = getattr(found, n)
        except AttributeError:
            __import__(used)
            found = getattr(found, n)

    return found
