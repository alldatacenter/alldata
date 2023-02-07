from crawlab.actions.login import login


def cli_login(args):
    login(api_address=args.api_address, username=args.username, password=args.password)
