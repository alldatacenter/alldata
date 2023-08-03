from feathrcli.cli import init
from click.testing import CliRunner
import os
import glob


def test_workspace_creation():
    """
    Test CLI init() is working properly.
    """
    runner = CliRunner()
    with runner.isolated_filesystem():

        result = runner.invoke(init, [])

        assert result.exit_code == 0
        assert os.path.isdir("./feathr_user_workspace")

        total_yaml_files = glob.glob('./feathr_user_workspace/*.yaml', recursive=True)
        # we should have exact 1 yaml file
        assert len(total_yaml_files) == 1
        # result = runner.invoke(init, [])

        test_folder_name = 'test_folder'
        result = runner.invoke(init, ['--name', test_folder_name])

        assert result.exit_code == 0

        total_yaml_files = glob.glob(os.path.join(test_folder_name, '*.yaml'), recursive=True)
        # we should have exact 1 yaml file
        assert len(total_yaml_files) == 1

        result = runner.invoke(init, ["--name", test_folder_name])

        assert result.exit_code == 2
        # use output for test for now
        expected_out = f'Feathr workspace ({test_folder_name}) already exist. Please use a new folder name.\n'
        assert expected_out in result.output
