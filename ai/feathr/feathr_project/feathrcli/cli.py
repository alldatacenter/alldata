import click
from py4j.java_gateway import JavaGateway
import pathlib
import os
from pathlib import Path
import distutils.dir_util
import subprocess
import urllib.request
from feathr.client import FeathrClient
from feathr.registry._feathr_registry_client import _FeatureRegistry

@click.group()
@click.pass_context
def cli(ctx: click.Context):
    """
    Feathr CLI tool. Visit https://github.com/feathr-ai/feathr for more details.
    """
    pass


def check_user_at_root():
    """
    Checks if the user is running the CLI from the root of the user workspace.

    We require the user to be running the CLI from the root of the user workspace so that path-related functionalities
    can work correctly.
    """
    # use this file as a anchor point to identify the root of the repo
    anchor_file = 'feathr_config.yaml'
    user_workspace_dir = Path(".")
    anchor_file_path = user_workspace_dir / anchor_file
    if not anchor_file_path.exists():
        raise click.UsageError('You are NOT at the root of your user workspace("/feathr_user_workspace"). Please '
                               'execute the command under your user workspace root.')


@cli.command()
@click.option('--name', default="feathr_user_workspace", help='Specify the workspace name.')
@click.option('--git/--no-git', default=False, help='When enabled, a git-based workspace will be created.')
def init(name, git):
    """
    Initializes a Feathr project to create and manage features. A team should share a same Feathr project usually via
    git. By default, git is NOT used to manage the workspace.
    """
    # Check if workspace already exists
    workspace_dir = Path(os.path.join(Path.cwd(), name))
    workspace_exist = os.path.isdir(workspace_dir)
    if workspace_exist:
        # workspace already exist. Just exit.
        raise click.UsageError(f'Feathr workspace ({name}) already exist. Please use a new folder name.')

    output_str = f'Creating workspace {name} with sample config files and mock data ...'
    click.echo(output_str)
    default_workspace = str(Path(Path(__file__).parent / 'data' / 'feathr_user_workspace').absolute())

    # current feathr_user_workspace directory w.r.t. where the init command is executed
    pathlib.Path(name).mkdir(parents=True, exist_ok=True)

    # create some default folders and example Feathr configs
    distutils.dir_util.copy_tree(default_workspace, str(workspace_dir))

    # Create a git repo for the workspace
    if git:
        os.chdir(workspace_dir)
        process = subprocess.Popen(['git', 'init'], stdout=subprocess.PIPE)
        output = process.communicate()[0]
        click.echo(output)
        click.echo(click.style('Git init completed for your workspace. Please read the '
                               'wiki to learn how to manage '
                               'your workspace with git.', fg='green'))
    click.echo(click.style('Feathr initialization completed.', fg='green'))


@cli.command()
@click.option('--save_to', default="./", help='Specify the path to save the output HOCON config(relative to current path).')
def hocon(save_to):
    """
    Scan all Python-based feature definitions recursively under current directory,
     convert them to HOCON config and save to a given path (relative to current directory).
    """
    scan_dir = Path.cwd()
    save_to = Path(os.path.join(scan_dir, save_to))
    _FeatureRegistry.save_to_feature_config(scan_dir, save_to)


@cli.command()
@click.argument('filepath', default='feature_join_conf/feature_join.conf', type=click.Path(exists=True))
def join(filepath):
    """
    Creates the offline training dataset with the requested features.
    """

    check_user_at_root()

    click.echo(click.style('Batch joining features with config: ' + filepath, fg='green'))
    with open(filepath) as f:
        lines = []
        for line in f:
            click.echo(line, nl=False)
            lines.append(line)
    click.echo()
    click.echo()

    client = FeathrClient()
    client._get_offline_features_with_config(filepath)
    click.echo(click.style('Feathr feature join job submitted. Visit '
                           'https://ms.web.azuresynapse.net/en-us/monitoring/sparkapplication for detailed job '
                           'result.', fg='green'))

@cli.command()
@click.argument('filepath', default='feature_gen_conf/feature_gen.conf', type=click.Path(exists=True))
def deploy(filepath):
    """
    Deploys the features to online store based on the feature generation config.
    """
    check_user_at_root()

    click.echo(click.style('Deploying feature generation config: ' + filepath, fg='green'))
    with open(filepath) as f:
        lines = []
        for line in f:
            click.echo(line, nl=False)
            lines.append(line)
    click.echo()
    click.echo()

    client = FeathrClient()
    client._materialize_features_with_config(filepath)
    click.echo()
    click.echo(click.style('Feathr feature deployment submitted. Visit '
                           'https://ms.web.azuresynapse.net/en-us/monitoring/sparkapplication for detailed job '
                           'result.', fg='green'))


@cli.command()
@click.option('--git/--no-git', default=False, help='If git-enabled, the new changes will be added and committed.')
@click.option('--msg', help='The feature name.')
def register(git, msg):
    """
    Register your feature metadata to your metadata registry.
    """

    check_user_at_root()

    # The register command is not integrated with Azure Atlas yet.
    click.echo(click.style('Registering your metadata to metadata service...', fg='green'))

    if git:
        click.echo(click.style('Git: adding all files.', fg='green'))
        click.echo(msg)
        process = subprocess.Popen(['git', 'add', '-A'], stdout=subprocess.PIPE)
        output = process.communicate()[0]
        click.echo(output)

        click.echo(click.style('Git: committing.', fg='green'))
        process2 = subprocess.Popen(['git', 'commit', '-m', msg], stdout=subprocess.PIPE)
        output2 = process2.communicate()[0]
        click.echo(output2)
    client = FeathrClient()
    client.register_features()
    click.echo(click.style('Feathr registration completed successfully!', fg='green'))


@cli.command()
def start():
    """
    Starts a local Feathr engine for local experimentation and testing.

    Feathr local test requires feathr local engine, a java jar, to stay running locally. You can download the jar
    yourself from feathr website or use this command to download. The jar should be placed under the root of the
    feathr_user_workspace. After the jar is downloaded, the command will run this jar. The jar needs to be running(
    don't close the terminal) while you want to use 'feathr test'.
    """
    def run_jar():
        cmd = ['java', '-jar', jar_name]
        with subprocess.Popen(cmd, stdout=subprocess.PIPE, bufsize=1, universal_newlines=True) as p:
            # Need to continuously pump the results from jar to terminal
            for line in p.stdout:
                print(line, end='')


    check_user_at_root()
    # The jar should be placed under the root of the user workspace
    jar_name = 'feathr_local_engine.jar'

    # Download the jar if it doesn't exist
    if not os.path.isfile(jar_name):
        url = 'https://azurefeathrstorage.blob.core.windows.net/public/' + jar_name
        file_name = url.split('/')[-1]
        u = urllib.request.urlopen(url)
        f = open(file_name, 'wb')
        meta = u.info()
        file_size = int(meta.get('Content-Length'))
        click.echo(click.style('There is no local feathr engine(jar) in the workspace. Will download the feathr jar.',
                               fg='green'))
        click.echo('Downloading feathr jar for local testing: %s Bytes: %s from %s' % (file_name, file_size, url))

        file_size_dl = 0
        block_sz = 8192
        with click.progressbar(length=file_size,
                               label='Download feathr local engine jar') as bar:
            while True:
                buffer = u.read(block_sz)
                if not buffer:
                    break

                file_size_dl += len(buffer)
                f.write(buffer)
                bar.update(block_sz)

        f.close()

    click.echo(click.style(f'Starting the local feathr engine: {jar_name}.'))
    click.echo(click.style(f'Please keep this open and start another terminal to run feathr test. This terminal shows '
                           f'the debug message.', fg='green'))

    run_jar()


@cli.command()
@click.option('--features', prompt='Your feature names, separated by comma', help='The feature name.')
def test(features):
    """
    Tests a single feature definition locally via local spark mode with mock data. Mock data has to be provided by the
    users. Please execute "feathr start" before "feathr test" to setup the local engine.
    """
    check_user_at_root()
    click.echo('\nProducing feature values for requested features ... ')

    gateway = JavaGateway()
    # User should run this command at user workspace dir root
    user_workspace_dir = os.path.abspath(".")

    # for py4j, it's always named as entry_point
    stack_entry_point_result = gateway.entry_point.getResult(user_workspace_dir, features)

    click.echo('\nFeature computation completed.')
    click.echo(stack_entry_point_result)
