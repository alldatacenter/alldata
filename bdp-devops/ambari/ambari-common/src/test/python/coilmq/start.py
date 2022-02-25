#!python
"""
Entrypoint for starting the application.
"""
import os
import logging


import time
import threading
from contextlib import contextmanager

is_nt = os.name == 'nt'

if not is_nt:
    import daemon as pydaemon
    import pid
else:
    pydaemon = pid = None

import click

from coilmq.config import config as global_config, init_config, init_logging, resolve_name
from coilmq.protocol import STOMP11
from coilmq.topic import TopicManager
from coilmq.queue import QueueManager
from coilmq.server.socket_server import ThreadedStompServer

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

logger = logging.getLogger(__name__)


def server_from_config(config=None, server_class=None, additional_kwargs=None):
    """
    Gets a configured L{coilmq.server.StompServer} from specified config.

    If `config` is None, global L{coilmq.config.config} var will be used instead.

    The `server_class` and `additional_kwargs` are primarily hooks for using this method
    from a testing environment.

    @param config: A C{ConfigParser.ConfigParser} instance with loaded config values.
    @type config: C{ConfigParser.ConfigParser}

    @param server_class: Which class to use for the server.  (This doesn't come from config currently.)
    @type server_class: C{class}

    @param additional_kwargs: Any additional args that should be passed to class.
    @type additional_kwargs: C{list}

    @return: The configured StompServer.
    @rtype: L{coilmq.server.StompServer}
    """
    global global_config
    if not config:
        config = global_config

    queue_store_factory = resolve_name(config.get('coilmq', 'qstore.factory'))
    subscriber_scheduler_factory = resolve_name(config.get(
            'coilmq', 'scheduler.subscriber_priority_factory'))
    queue_scheduler_factory = resolve_name(config.get(
            'coilmq', 'scheduler.queue_priority_factory'))

    if config.has_option('coilmq', 'auth.factory'):
        authenticator_factory = resolve_name(
                config.get('coilmq', 'auth.factory'))
        authenticator = authenticator_factory()
    else:
        authenticator = None

    server = ThreadedStompServer((config.get('coilmq', 'listen_addr'), config.getint('coilmq', 'listen_port')),
                                 queue_manager=QueueManager(store=queue_store_factory(),
                                                            subscriber_scheduler=subscriber_scheduler_factory(),
                                                            queue_scheduler=queue_scheduler_factory()),
                                 topic_manager=TopicManager(),
                                 authenticator=authenticator,
                                 protocol=STOMP11)
    logger.info("Created server:%r" % server)
    return server


def context_serve(context, configfile, listen_addr, listen_port, logfile,
                  debug, daemon, uid, gid, pidfile, umask, rundir):
    """
    Takes a context object, which implements the __enter__/__exit__ "with" interface 
    and starts a server within that context.

    This method is a refactored single-place for handling the server-run code whether
    running in daemon or non-daemon mode.  It is invoked with a dummy (passthrough) 
    context object for the non-daemon use case. 

    @param options: The compiled collection of options that need to be parsed. 
    @type options: C{ConfigParser}

    @param context: The context object that implements __enter__/__exit__ "with" methods.
    @type context: C{object}

    @raise Exception: Any underlying exception will be logged but then re-raised.
    @see: server_from_config()
    """
    global global_config

    server = None
    try:
        with context:
            # There's a possibility here that init_logging() will throw an exception.  If it does,
            # AND we're in a daemon context, then we're not going to be able to do anything with it.
            # We've got no stderr/stdout here; and so (to my knowledge) no reliable (& cross-platform),
            # way to display errors.
            level = logging.DEBUG if debug else logging.INFO
            init_logging(logfile=logfile, loglevel=level,
                         configfile=configfile)

            server = server_from_config()
            logger.info("Stomp server listening on %s:%s" % server.server_address)

            if debug:
                poll_interval = float(global_config.get(
                        'coilmq', 'debug.stats_poll_interval'))
                if poll_interval:  # Setting poll_interval to 0 effectively disables it.
                    def diagnostic_loop(server):
                        log = logger
                        while True:
                            log.debug(
                                    "Stats heartbeat -------------------------------")
                            store = server.queue_manager.store
                            for dest in store.destinations():
                                log.debug("Queue %s: size=%s, subscribers=%s" % (
                                    dest, store.size(dest), server.queue_manager.subscriber_count(dest)))

                            # TODO: Add number of subscribers?

                            time.sleep(poll_interval)

                    diagnostic_thread = threading.Thread(
                            target=diagnostic_loop, name='DiagnosticThread', args=(server,))
                    diagnostic_thread.daemon = True
                    diagnostic_thread.start()

            server.serve_forever()

    except (KeyboardInterrupt, SystemExit):
        logger.info("Stomp server stopped by user interrupt.")
        raise SystemExit()
    except Exception as e:
        logger.error("Stomp server stopped due to error: %s" % e)
        logger.exception(e)
        raise SystemExit()
    finally:
        if server:
            server.server_close()


def _main(config=None, host=None, port=None, logfile=None, debug=None,
          daemon=None, uid=None, gid=None, pidfile=None, umask=None, rundir=None):

    # Note that we must initialize the configuration before we enter the context
    # block; however, we _cannot_ initialize logging until we are in the context block
    # (so we defer that until the context_serve call.)
    init_config(config)

    if host is not None:
        global_config.set('coilmq', 'listen_addr', host)

    if port is not None:
        global_config.set('coilmq', 'listen_port', str(port))

        if daemon and is_nt:
            warnings.warn("Daemon context is not available for NT platform")

    # in an on-daemon mode, we use a dummy context objectx
    # so we can use the same run-server code as the daemon version.
    context = pydaemon.DaemonContext(uid=uid,
                                     gid=gid,
                                     pidfile=pid.PidFile(pidname=pidfile) if pidfile else None,
                                     umask=int(umask, 8),
                                     working_directory=rundir) if daemon and pydaemon else contextmanager(lambda: (yield))()

    context_serve(context, config, host, port, logfile, debug, daemon, uid, gid, pidfile, umask, rundir)


@click.command()
@click.option("-c", "--config", help="Read configuration from FILE. (CLI options override config file.)", metavar="FILE")
@click.option("-b", "--host", help="Listen on specified address (default 127.0.0.1)", metavar="ADDR")
@click.option("-p", "--port", help="Listen on specified port (default 61613)", type=int, metavar="PORT")
@click.option("-l", "--logfile", help="Log to specified file (unless logging configured in config file).", metavar="FILE")
@click.option("--debug", default=False, help="Sets logging to debug (unless logging configured in config file).")
@click.option("-d", "--daemon", default=False, help="Run server as a daemon (default False).")
@click.option("-u", "--uid", help="The user/UID to use for daemon process.", metavar="UID")
@click.option("-g", "--gid", help="The group/GID to use for daemon process.", metavar="GID")
@click.option("--pidfile",   help="The PID file to use.", metavar="FILE")
@click.option("--umask", help="Umask (octal) to apply for daemonized process.", metavar="MASK")
@click.option('--rundir', help="The working directory to use for the daemonized process (default /).", metavar="DIR")
def main(config, host, port, logfile, debug, daemon, uid, gid, pidfile, umask, rundir):
    """
    Main entry point for running a socket server from the commandline.

    This method will read in options from the commandline and call the L{config.init_config} method
    to get everything setup.  Then, depending on whether deamon mode was specified or not, 
    the process may be forked (or not) and the server will be started.
    """

    _main(**locals())


if __name__ == '__main__':
    try:
        main()
    except (KeyboardInterrupt, SystemExit):
        pass
    except Exception as e:
        logger.error("Server terminated due to error: %s" % e)
        logger.exception(e)
