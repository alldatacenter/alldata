"""The stomp.py command line client (used for testing or simple STOMP command scripting).

"""

import base64
from cmd import Cmd
from optparse import OptionParser
import json
import os
import sys
import time

from ambari_stomp.adapter.multicast import MulticastConnection
import ambari_stomp.colors
from ambari_stomp.connect import ambari_stompConnection10, StompConnection11, StompConnection12
from ambari_stomp.listener import ConnectionListener, StatsListener

sys.path.append('.')
import ambari_stomp as stomp


##
# Command-line version string
#
stomppy_version = 'Stomp.py Version %s.%s.%s' % stomp.__version__

try:
    import uuid
except ImportError:
    from backward import uuid


class SubscriptionInfo(object):
    """
    Used to store info about a subscription.
    """
    def __init__(self, id, ack):
        self.id = id
        self.ack = ack


class StompCLI(Cmd, ConnectionListener):
    """
    A command line interface to the stomp.py client.  See :py:class:`stomp.connect.StompConnection11`
    for more information on establishing a connection to a stomp server.
    """
    def __init__(self, host='localhost', port=61613, user='', passcode='', ver='1.1', prompt='> ', verbose=True,
                 use_ssl=False, heartbeats=(0, 0), stdin=sys.stdin, stdout=sys.stdout):
        Cmd.__init__(self, 'Tab', stdin, stdout)
        ConnectionListener.__init__(self)
        self.prompt = prompt
        self.verbose = verbose
        self.user = user
        self.passcode = passcode
        self.__quit = False
        if ver == '1.0':
            self.conn = StompConnection10([(host, port)])
        elif ver == '1.1':
            self.conn = StompConnection11([(host, port)], heartbeats=heartbeats)
        elif ver == '1.2':
            self.conn = StompConnection12([(host, port)], heartbeats=heartbeats)
        elif ver == 'multicast':
            self.conn = MulticastConnection()
        else:
            raise RuntimeError('Unknown version')
        if use_ssl:
            self.conn.set_ssl([(host, port)])
        self.conn.set_listener('', self)
        self.conn.start()
        self.conn.connect(self.user, self.passcode, wait=True)
        self.transaction_id = None
        self.version = ver
        try:
            self.nversion = float(ver)
        except ValueError:
            self.nversion = 1.0
        self.__subscriptions = {}
        self.__subscription_id = 1

    def __print_async(self, frame_type, headers, body):
        """
        Utility function to print a message and setup the command prompt
        for the next input
        """
        if self.__quit:
            return
        self.__sysout("\r  \r", end='')
        if self.verbose:
            self.__sysout(frame_type)
            for k, v in headers.items():
                self.__sysout('%s: %s' % (k, v))
        if self.prompt != '':
            self.__sysout('')
        self.__sysout(body)
        self.__sysout(self.prompt, end='')
        self.stdout.flush()

    def __sysout(self, msg, end="\n"):
        self.stdout.write(str(msg) + end)

    def __error(self, msg, end="\n"):
        self.stdout.write(stomp.colors.BOLD + stomp.colors.RED + str(msg) + stomp.colors.NO_COLOR + end)

    def on_connecting(self, host_and_port):
        """
        See :py:meth:`ConnectionListener.on_connecting`
        """

    def on_disconnected(self):
        """
        see :py:meth:`ConnectionListener.on_disconnected`
        """
        if not self.__quit:
            self.__error("lost connection")

    def on_message(self, headers, body):
        """
        See :py:meth:`ConnectionListener.on_message`

        Special case: if the header 'filename' is present, the content is written out
        as a file
        """
        if 'filename' in headers:
            content = base64.b64decode(body.encode())
            if os.path.exists(headers['filename']):
                fname = '%s.%s' % (headers['filename'], int(time.time()))
            else:
                fname = headers['filename']
            with open(fname, 'wb') as f:
                f.write(content)
            self.__print_async("MESSAGE", headers, "Saved file: %s" % fname)
        else:
            self.__print_async("MESSAGE", headers, body)

    def on_error(self, headers, body):
        """
        See :py:meth:`ConnectionListener.on_error`
        """
        self.__print_async("ERROR", headers, body)

    def on_receipt(self, headers, body):
        """
        See :py:meth:`ConnectionListener.on_receipt`
        """
        self.__print_async("RECEIPT", headers, body)

    def on_connected(self, headers, body):
        """
        See :py:meth:`ConnectionListener.on_connected`
        """
        self.__print_async("CONNECTED", headers, body)

    def help_help(self):
        self.__sysout('Quick help on commands')

    def default(self, line):
        self.__error('Unknown command: %s' % line.split()[0])

    def emptyline(self):
        pass

    def help(self, usage, description, required=(), optional=()):
        rparams = "\n\t" + "\n\t".join(required)
        oparams = "\n\t" + "\n\t".join(optional)

        m = {
            'hl': stomp.colors.BOLD + stomp.colors.GREEN,
            'nc': stomp.colors.NO_COLOR,
            'usage': usage,
            'description': description,
            'required': rparams.rstrip(),
            'optional': oparams.rstrip()
        }

        if rparams.rstrip() != '':
            rparams = '''%(hl)sRequired Parameters:%(nc)s%(required)s\n\n''' % m
            m['required'] = rparams

        if oparams.rstrip() != '':
            oparams = '''%(hl)sOptional Parameters:%(nc)s%(optional)s\n\n''' % m
            m['optional'] = oparams

        self.__sysout('''%(hl)sUsage:%(nc)s
\t%(usage)s

%(required)s%(optional)s%(hl)sDescription:%(nc)s
\t%(description)s
        ''' % m)

    def do_quit(self, args):
        self.__quit = True
        self.__sysout('Shutting down, please wait')
        return True
    do_exit = do_quit
    do_EOF = do_quit

    def help_quit(self):
        self.help('exit', 'Exit the stomp client')
    help_exit = help_quit

    def help_EOF(self):
        self.help('exit', 'Exit the stomp client (using CTRL-D)')

    def do_subscribe(self, args):
        args = args.split()
        if len(args) < 1:
            self.__error('Expecting: subscribe <destination> [ack]')
            return

        name = args[0]
        if name in self.__subscriptions:
            self.__error('Already subscribed to %s' % name)
            return

        ack_mode = 'auto'
        if len(args) >= 2:
            ack_mode = args[1]

        sid = self.__subscription_id
        self.__subscription_id += 1

        self.__sysout('Subscribing to "%s" with acknowledge set to "%s", id set to "%s"' % (name, ack_mode, sid))
        self.conn.subscribe(destination=name, ack=ack_mode, id=sid)
        self.__subscriptions[name] = SubscriptionInfo(sid, ack_mode)

    def help_subscribe(self):
        self.help('subscribe <destination> [ack]',
                  '''Register to listen to a given destination. Like send, the subscribe command requires a destination
\theader indicating which destination to subscribe to. The ack parameter is optional, and defaults to
\tauto.''', ['destination - the name to subscribe to'], ['ack - how to handle acknowledgements for a message; either automatically (auto) or manually (client)'])

    def do_unsubscribe(self, args):
        args = args.split()
        if len(args) < 1:
            self.__error('Expecting: unsubscribe <destination>')
            return

        if args[0] not in self.__subscriptions:
            self.__sysout('Subscription %s not found' % args[0])
            return

        self.__sysout('Unsubscribing from "%s"' % args[0])
        self.conn.unsubscribe(destination=args[0], id=self.__subscriptions[args[0]].id)
        del self.__subscriptions[args[0]]

    def help_unsubscribe(self):
        self.help('unsubscribe <destination>', 'Remove an existing subscription - so that the client no longer receive messages from that destination.',
                  ['destination - the name to unsubscribe from'], ['ack - how to handle acknowledgements for a message; either automatically (auto) or manually (client)'])

    def do_send(self, args):
        args = args.split()
        if len(args) < 2:
            self.__error('Expecting: send <destination> <message>')
        elif not self.transaction_id:
            self.conn.send(args[0], ' '.join(args[1:]))
        else:
            self.conn.send(args[0], ' '.join(args[1:]), transaction=self.transaction_id)

    def complete_send(self, text, line, begidx, endidx):
        mline = line.split(' ')[1]
        offs = len(mline) - len(text)
        return [s[offs:] for s in self.__subscriptions if s.startswith(mline)]
    complete_unsubscribe = complete_send
    complete_sendrec = complete_send
    complete_sendreply = complete_send
    complete_sendfile = complete_send

    def help_send(self):
        self.help('send <destination> <message>', 'Sends a message to a destination in the messaging system.',
                  ['destination - where to send the message', 'message - the content to send'])

    def do_sendrec(self, args):
        args = args.split()
        receipt_id = str(uuid.uuid4())
        if len(args) < 2:
            self.__error('Expecting: sendrec <destination> <message>')
        elif not self.transaction_id:
            self.conn.send(args[0], ' '.join(args[1:]), receipt=receipt_id)
        else:
            self.conn.send(args[0], ' '.join(args[1:]), transaction=self.transaction_id, receipt=receipt_id)

    def help_sendrec(self):
        self.help('sendrec <destination> <message>',
                  'Sends a message to a destination in the messaging system and blocks for receipt of the message.',
                  ['destination - where to send the message', 'message - the content to send'])

    def do_sendreply(self, args):
        args = args.split()
        if len(args) < 3:
            self.__error('Expecting: sendreply <destination> <correlation-id> <message>')
        else:
            self.conn.send(args[0], "%s\n" % ' '.join(args[2:]), headers={'correlation-id': args[1]})

    def help_sendreply(self):
        self.help('sendreply <destination> <correlation-id> <message>',
                  'Sends a reply message to a destination in the messaging system.',
                  ['destination - where to send the message',
                   'correlation-id - the correlating identifier to send with the response',
                   'message - the content to send'])

    def do_sendfile(self, args):
        args = args.split()
        if len(args) < 2:
            self.__error('Expecting: sendfile <destination> <filename> [headers.json]')
        elif not os.path.exists(args[1]):
            self.__error('File %s does not exist' % args[1])
        else:
            headers = {}
            if len(args) == 3:
                if not os.path.exists(args[2]):
                    self.__error('File %s does not exist' % args[2])
                    return
                self.__sysout("Loading %s" % args[2])
                with open(args[2], mode='rb') as jf:
                    headers = json.load(jf)
                    self.__sysout('Using headers %s' % str(headers))

            with open(args[1], mode='rb') as f:
                s = f.read()
            msg = base64.b64encode(s).decode()
            if not self.transaction_id:
                self.conn.send(args[0], msg, filename=args[1], headers=headers)
            else:
                self.conn.send(args[0], msg, filename=args[1], headers=headers, transaction=self.transaction_id)

    def help_sendfile(self):
        self.help('sendfile <destination> <filename> [headers.json]',
                  'Sends a file to a destination in the messaging system.',
                  ['destination - where to send the message', 'filename - the file to send',
                   'headers.json - json map with headers to send'])

    def do_version(self, args):
        self.__sysout('%s%s [Protocol version %s]%s' %
                      (stomp.colors.BOLD, stomppy_version, self.conn.version, stomp.colors.NO_COLOR))
    do_ver = do_version

    def help_version(self):
        self.help('version', 'Display the version of the client')
    help_ver = help_version

    def check_ack_nack(self, cmd, args):
        if self.nversion >= 1.2 and len(args) < 1:
            self.__error("Expecting: %s <ack-id>" % cmd)
            return None
        elif self.nversion == 1.1 and len(args) < 2:
            self.__error("Expecting: %s <message-id> <subscription-id>" % cmd)
            return None
        elif len(args) < 1:
            self.__error("Expecting: %s <message-id>" % cmd)
            return None

        if len(args) == 1:
            return (args[0], None)
        else:
            return (args[0], args[1])

    def do_ack(self, args):
        args = args.split()
        hdrs = self.check_ack_nack('ack', args)
        if hdrs is None:
            return

        (message_id, subscription_id) = hdrs

        if not self.transaction_id:
            self.conn.ack(message_id, subscription_id)
        else:
            self.conn.ack(message_id, subscription_id, transaction=self.transaction_id)

    def help_ack(self):
        self.help('ack <message-id> [subscription-id]', '''The command 'ack' is used to acknowledge consumption of a message from a subscription using client
\tacknowledgment. When a client has issued a 'subscribe' with the ack flag set to client, any messages
\treceived from that destination will not be considered to have been consumed (by the server) until
\tthe message has been acknowledged.''', ['message-id - the id of the message being acknowledged'], ['subscription-id the id of the subscription (only required for STOMP 1.1)'])

    def do_nack(self, args):
        args = args.split()
        hdrs = self.check_ack_nack('nack', args)
        if hdrs is None:
            return

        if not self.transaction_id:
            self.conn.nack(headers=hdrs)
        else:
            self.conn.nack(headers=hdrs, transaction=self.transaction_id)

    def help_nack(self):
        self.help('nack <message-id> [subscription]', '''The command 'nack' is used to acknowledge the failure of a message from a subscription using client
\tacknowledgment. When a client has issued a 'subscribe' with the ack flag set to client, any messages
\treceived from that destination will not be considered to have been consumed (by the server) until
\tthe message has been acknowledged (ack or nack).''', ['message-id - the id of the message being acknowledged'])

    def do_abort(self, args):
        if not self.transaction_id:
            self.__error("Not currently in a transaction")
        else:
            self.conn.abort(transaction=self.transaction_id)
            self.__sysout('Aborted transaction: %s' % self.transaction_id)
            self.transaction_id = None
    do_rollback = do_abort

    def help_abort(self):
        self.help('abort', 'Roll back a transaction in progress.')
    help_rollback = help_abort

    def do_begin(self, args):
        if self.transaction_id:
            self.__error("Currently in a transaction (%s)" % self.transaction_id)
        else:
            self.transaction_id = self.conn.begin()
            self.__sysout('Transaction id: %s' % self.transaction_id)

    def help_begin(self):
        self.help('begin', '''Start a transaction. Transactions in this case apply to sending and acknowledging -
\tany messages sent or acknowledged during a transaction will be handled atomically based on the
\ttransaction.''')

    def do_commit(self, args):
        if not self.transaction_id:
            self.__error("Not currently in a transaction")
        else:
            self.__sysout('Committing %s' % self.transaction_id)
            self.conn.commit(transaction=self.transaction_id)
            self.transaction_id = None

    def help_commit(self):
        self.help('commit', 'Commit a transaction in progress.')

    def do_stats(self, args):
        args = args.split()
        if len(args) < 1:
            stats = self.conn.get_listener('stats')
            if stats:
                self.__sysout(stats)
            else:
                self.__error('No stats available')
        elif args[0] == 'on':
            self.conn.set_listener('stats', StatsListener())
        elif args[0] == 'off':
            self.conn.remove_listener('stats')
        else:
            self.__error('Expecting: stats [on|off]')

    def help_stats(self):
        self.help('stats [on|off]', '''Record statistics on messages sent, received, errors, etc. If no argument (on|off) is specified,
\tdump the current statistics.''')

    def do_run(self, args):
        args = args.split()
        if len(args) == 0:
            self.__error("Expecting: run <filename>")
        elif not os.path.exists(args[0]):
            self.__error("File %s was not found" % args[0])
        else:
            with open(args[0]) as f:
                lines = f.read().split('\n')
            for line in lines:
                self.onecmd(line)

    def help_run(self):
        self.help('run <filename>', 'Execute commands in a specified file')


def do_nothing_loop():
    while 1:
        time.sleep(1)


def optional_arg(arg_default):
    def func(option, opt_str, value, parser):
        if parser.rargs and not parser.rargs[0].startswith('-'):
            val = parser.rargs[0]
            parser.rargs.pop(0)
        else:
            val = arg_default
        setattr(parser.values, option.dest, val)
    return func


def main():
    parser = OptionParser(version=stomppy_version)

    parser.add_option('-H', '--host', type='string', dest='host', default='localhost',
                      help='Hostname or IP to connect to. Defaults to localhost if not specified.')
    parser.add_option('-P', '--port', type=int, dest='port', default=61613,
                      help='Port providing stomp protocol connections. Defaults to 61613 if not specified.')
    parser.add_option('-U', '--user', type='string', dest='user', default=None,
                      help='Username for the connection')
    parser.add_option('-W', '--password', type='string', dest='password', default=None,
                      help='Password for the connection')
    parser.add_option('-F', '--file', type='string', dest='filename',
                      help='File containing commands to be executed, instead of prompting from the command prompt.')
    parser.add_option('-S', '--stomp', type='string', dest='stomp', default='1.1',
                      help='Set the STOMP protocol version.')
    parser.add_option('-L', '--listen', type='string', dest='listen', default=None,
                      help='Listen for messages on a queue/destination')
    parser.add_option("-V", "--verbose", dest="verbose", default='on',
                      help='Verbose logging "on" or "off" (if on, full headers from stomp server responses are printed)')
    parser.add_option('--ssl', action='callback', callback=optional_arg(True), dest='ssl',
                      help='Enable SSL connection')
    parser.add_option('--heartbeats', type='string', dest='heartbeats', default="0,0",
                      help='Heartbeats to request when connecting with protocol >= 1.1, two comma separated integers.')

    parser.set_defaults()
    (options, _) = parser.parse_args()

    if options.verbose == 'on':
        verbose = True
    else:
        verbose = False

    if options.ssl is None:
        options.ssl = False

    if options.listen:
        prompt = ''
    else:
        prompt = '> '

    heartbeats = tuple(map(int, options.heartbeats.split(",")))

    st = StompCLI(options.host, options.port, options.user, options.password, options.stomp, prompt, verbose,
                  options.ssl, heartbeats)

    if options.listen:
        st.do_subscribe(options.listen)
        try:
            while 1:
                time.sleep(10)
        except:
            print("\n")
    elif options.filename:
        st.do_run(options.filename)
    else:
        # disable CTRL-C, since can't guarantee correct handling of disconnect
        import signal

        def signal_handler(signal, frame):
            pass
        signal.signal(signal.SIGINT, signal_handler)

        try:
            try:
                st.cmdloop()
            except KeyboardInterrupt:
                st.do_quit()
        finally:
            st.conn.disconnect()


#
# command line access
#
if __name__ == '__main__':
    try:
        main()
    except:
        pass
