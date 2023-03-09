
import sys
import os
# We have to append user's current path to sys path so the modules can be resolved
# Otherwise we will got "no module named feathr" error
sys.path.append(os.path.abspath(os.getcwd()))

from feathrcli.cli import init
from click.testing import CliRunner
from feathr import FeathrClient


def clean_data():
    """
    Remove the test data(feature table: nycTaxiDemoFeature) in Azure.
    """
    client = FeathrClient()
    table_name = 'nycTaxiDemoFeature'
    client._clean_test_data(table_name)
    print('Redis table cleaned: ' + table_name)


runner = CliRunner()
with runner.isolated_filesystem():
    runner.invoke(init, [])
    # Need to be in the workspace so it won't complain
    os.chdir('feathr_user_workspace')
    clean_data()
