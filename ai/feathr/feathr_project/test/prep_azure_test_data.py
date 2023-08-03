
import sys
import os
# We have to append user's current path to sys path so the modules can be resolved
# Otherwise we will got "no module named feathr" error
sys.path.append(os.path.abspath(os.getcwd()))

from feathrcli.cli import init
from click.testing import CliRunner
from feathr import FeathrClient

def initialize_data():
    """
    Initialize the test data to Azure for testing.
    WARNING: It will override the existing test data.
    """
    print('Creating test data. This might override existing test data.')
    client = FeathrClient()
    # materialize feature to online store
    client._materialize_features_with_config('feature_gen_conf/test_feature_gen_1.conf')
    client._materialize_features_with_config('feature_gen_conf/test_feature_gen_2.conf')
    client._materialize_features_with_config('feature_gen_conf/test_feature_gen_snowflake.conf')
    print('Test data push job has started. It will take some time to complete.')


runner = CliRunner()
with runner.isolated_filesystem():
    runner.invoke(init, [])
    # Need to be in the workspace so it won't complain
    os.chdir('feathr_user_workspace')
    initialize_data()
