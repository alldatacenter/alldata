from argparse import Namespace

from pymongo import MongoClient
from pymongo.database import Database

from crawlab.actions.migrate import migrate


def cli_migrate(args: Namespace):
    # mongo settings
    mongo_host = args.mongo_host
    mongo_port = args.mongo_port
    mongo_db_name = args.mongo_db
    mongo_username = args.mongo_username
    mongo_password = args.mongo_password
    mongo_auth_source = args.mongo_auth_source

    # mongo client
    mongo_client = MongoClient(host=mongo_host, port=mongo_port, username=mongo_username, password=mongo_password,
                               authSource=mongo_auth_source)

    # mongo db
    mongo_db = mongo_client[mongo_db_name]

    # migrate
    migrate(mongo_db=mongo_db)
