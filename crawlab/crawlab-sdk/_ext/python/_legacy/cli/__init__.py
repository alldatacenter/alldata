import os

import click

from crawlab import constants
from legacy.core import client
from legacy.core import config
from crawlab.config import VERSION


def print_version(ctx, param, value):
    if not value or ctx.resilient_parsing:
        return
    click.echo(f'v{VERSION}')
    ctx.exit()


@click.group()
@click.option('--version', '-v', is_flag=True, callback=print_version, expose_value=False, is_eager=True,
              help='show version of SDK')
def cli():
    pass


@click.command('login', help='login to the platform and get token')
@click.option('--username', '-u')
@click.password_option('--password', '-p', confirmation_prompt=False)
@click.option('--api_address', '-a')
def login(username=None, password=None, api_address=None):
    if username is not None:
        config.data.username = username
    if password is not None:
        config.data.password = password
    if api_address is not None:
        config.data.api_address = api_address
    config.save()
    client.update_token()


@click.command('config', help='set config info')
@click.option('--username', '-u')
@click.password_option('--password', '-p', confirmation_prompt=False)
@click.option('--api_address', '-a')
def config_(username=None, password=None, api_address=None):
    if username is not None:
        config.data.username = username
    if password is not None:
        config.data.password = password
    if api_address is not None:
        config.data.api_address = api_address
    config.save()
    print('config has been saved')


@click.command('check', help='check the connection and update token')
def check():
    client.update_token()


@click.command('nodes', help='list the nodes')
def nodes():
    client.list_nodes()


@click.command('spiders', help='list the spiders')
def spiders():
    client.list_spiders()


@click.command('schedules', help='list the schedules')
def schedules():
    client.list_schedules()


@click.command('tasks', help='list the tasks')
@click.option('--number', '-n', help='number of tasks')
def tasks(number=None):
    client.list_tasks(number)


@click.command('upload', help='upload a spider')
@click.option('--type', '-t', type=click.Choice(['customized', 'configurable']), default=constants.Spider.CUSTOMIZED,
              help='spider type')
@click.option('--directory', '-d', help='directory path, for customized spiders')
@click.option('--name', '-n', help='spider name')
@click.option('--col', '-c', help='spider results collection')
@click.option('--display_name', '-N', help='spider display name')
@click.option('--command', '-m', help='spider execution command')
@click.option('--id', '-i', help='spider id')
@click.option('--spiderfile', '-f', help='Spiderfile path')
def upload(type=None, directory=None, name=None, col=None, display_name=None, command=None, id=None, spiderfile=None):
    # TODO: finish all functionality
    if type is None:
        type = constants.Spider.CUSTOMIZED

    if type == constants.Spider.CUSTOMIZED:
        # customized spider
        if directory is None:
            directory = os.path.abspath(os.curdir)
        client.upload_customized_spider(directory, name, col, display_name, command, id)
    elif type == constants.Spider.CONFIGURABLE:
        # configurable spider
        pass


@click.command('settings', help='get settings of a scrapy project')
@click.option('--directory', '-d', help='directory path, for scrapy spiders')
def settings(directory=None):
    client.settings(directory)


@click.command('items', help='get items of a scrapy project')
@click.option('--directory', '-d', help='directory path, for scrapy spiders')
def items(directory=None):
    client.items(directory)


@click.command('pipelines', help='get pipelines of a scrapy project')
@click.option('--directory', '-d', help='directory path, for scrapy spiders')
@click.option('--name', '-n', help='pipeline name')
@click.option('--delete', '-D', help='whether to delete the pipeline')
def pipelines(directory=None, name=None, delete=None):
    client.pipelines(directory, name, delete)


@click.command('find_spider_filepath', help='get filepath from spider name of a scrapy project')
@click.option('--directory', '-d', help='directory path, for scrapy spiders')
@click.option('--spider_name', '-n', help='spider name')
def find_spider_filepath(directory=None, spider_name=None):
    if spider_name is None:
        print('spider_name is required')
        return
    client.find_spider_filepath(directory, spider_name)


def main():
    cli.add_command(check)
    cli.add_command(config_)
    cli.add_command(find_spider_filepath)
    cli.add_command(items)
    cli.add_command(login)
    cli.add_command(nodes)
    cli.add_command(pipelines)
    cli.add_command(settings)
    cli.add_command(schedules)
    cli.add_command(tasks)
    cli.add_command(spiders)
    cli.add_command(upload)

    cli()


if __name__ == '__main__':
    main()
