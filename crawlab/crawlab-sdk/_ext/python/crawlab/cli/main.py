import argparse

from crawlab.cli.actions.migrate import cli_migrate
from crawlab.constants.upload import CLI_ACTION_UPLOAD, CLI_ACTION_LOGIN, CLI_DEFAULT_API_ADDRESS, \
    CLI_DEFAULT_API_USERNAME, \
    CLI_DEFAULT_API_PASSWORD, CLI_ACTION_CONFIG, CLI_ACTION_MIGRATE
from crawlab.cli.actions.config import cli_config_func
from crawlab.cli.actions.login import cli_login
from crawlab.cli.actions.upload import cli_upload

# root parser
root_parser = argparse.ArgumentParser(description='CLI tool for Crawlab')

# sub-parsers
subparsers = root_parser.add_subparsers()

# login parser
login_parser = subparsers.add_parser(CLI_ACTION_LOGIN)
login_parser.add_argument('--api_address', '-a', help='HTTP URL of API address of Crawlab',
                          default=CLI_DEFAULT_API_ADDRESS, type=str)
login_parser.add_argument('--username', '-u', help='Username for logging in Crawlab', default=CLI_DEFAULT_API_USERNAME,
                          type=str)
login_parser.add_argument('--password', '-p', help='Password for logging in Crawlab', default=CLI_DEFAULT_API_PASSWORD,
                          type=str)
login_parser.set_defaults(func=cli_login, action=CLI_ACTION_LOGIN)

# upload parser
upload_parser = subparsers.add_parser(CLI_ACTION_UPLOAD)
upload_parser.add_argument('--dir', '-d', help='Local directory of spider to upload. Default: current directory',
                           default=None, type=str)
upload_parser.add_argument('--create', '-c', help='Whether to create a new spider. Default: false', action='store_true',
                           default=False)
upload_parser.add_argument('--id', '-i', help='Spider ID if uploading to an existing spider.',
                           type=str)
upload_parser.add_argument('--name', '-n', help='Spider name if creating a new spider. Default: directory name',
                           type=str)
upload_parser.add_argument('--description', '-D',
                           help='Spider description if creating a new spider. Default: spider name', type=str)
upload_parser.add_argument('--mode', '-M',
                           help='Default spider running task mode. Default: random', type=str, default='random')
upload_parser.add_argument('--priority', '-p',
                           help='Default spider running task priority. Default: 5', type=int, default=5)
upload_parser.add_argument('--cmd', '-m',
                           help='Spider execute command if creating a new spider')
upload_parser.add_argument('--param', '-P',
                           help='Spider execute params if creating a new spider')
upload_parser.add_argument('--col_name', '-C',
                           help='Spider results collection name if creating a new spider. Default: results_<spider_name>',
                           type=str)
upload_parser.add_argument('--exclude_path', '-e',
                           help='Exclude dir of spider to upload, like .env .venv node_modules',
                           default=None,
                           type=str,
                           required=False)
upload_parser.set_defaults(func=cli_upload, action=CLI_ACTION_UPLOAD)

# config parser
config_parser = subparsers.add_parser(CLI_ACTION_CONFIG)
config_parser.add_argument('--set', '-s', type=str)
config_parser.add_argument('--unset', '-u', type=str)
config_parser.set_defaults(func=cli_config_func, action=CLI_ACTION_CONFIG)

# migrate parser
migrate_parser = subparsers.add_parser(CLI_ACTION_MIGRATE)
migrate_parser.add_argument('--mongo_host', help='MongoDB host', type=str, default='localhost')
migrate_parser.add_argument('--mongo_port', help='MongoDB port', type=int, default=27017)
migrate_parser.add_argument('--mongo_db', help='MongoDB db', type=str, default='crawlab_test')
migrate_parser.add_argument('--mongo_username', help='MongoDB username', type=str, default=None)
migrate_parser.add_argument('--mongo_password', help='MongoDB password', type=str, default=None)
migrate_parser.add_argument('--mongo_auth_source', help='MongoDB auth source', type=str, default='admin')
migrate_parser.set_defaults(func=cli_migrate, action=CLI_ACTION_MIGRATE)


def main():
    args = root_parser.parse_args()
    if not hasattr(args, 'func'):
        root_parser.print_help()
        return
    try:
        args.func(args)
    except Exception as e:
        print(e)
        if getattr(args, 'action') == CLI_ACTION_LOGIN:
            login_parser.print_help()
        elif getattr(args, 'action') == CLI_ACTION_UPLOAD:
            upload_parser.print_help()
        elif getattr(args, 'action') == CLI_ACTION_CONFIG:
            config_parser.print_help()
        elif getattr(args, 'action') == CLI_ACTION_MIGRATE:
            migrate_parser.print_help()
        else:
            root_parser.print_help()


if __name__ == '__main__':
    main()
