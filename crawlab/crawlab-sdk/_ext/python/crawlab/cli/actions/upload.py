import os
from argparse import Namespace

from crawlab.actions.upload import upload_dir


def cli_upload(args: Namespace):
    # spider id
    spider_id = args.id

    # directory path
    dir_path = args.dir
    if dir_path is None:
        dir_path = os.path.abspath('')

    # exclude path
    exclude_path = args.exclude_path
    if exclude_path and not isinstance(exclude_path, list):
        exclude_path = [exclude_path]

    # upload directory
    upload_dir(
        dir_path=dir_path, create=args.create, spider_id=spider_id, name=args.name, description=args.description,
        mode=args.mode, priority=args.priority, cmd=args.cmd, param=args.param, col_name=args.col_name,
        exclude_path=exclude_path
    )
