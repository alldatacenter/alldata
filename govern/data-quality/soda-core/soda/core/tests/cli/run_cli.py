#  Copyright 2020 Soda
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import os
import sys
import traceback

from click.testing import CliRunner
from dotenv import load_dotenv
from soda.cli.cli import main


def run_cli(args):
    run_result = None
    try:
        load_dotenv(".env", override=True)

        runner = CliRunner()
        runner.file = sys.stdout
        args = [arg for arg in args if isinstance(arg, str)]
        print("soda " + (" ".join(args)))
        run_result = runner.invoke(
            main,
            args,
        )
    except Exception as e:
        traceback.print_exc()
    if run_result:
        print(f"\n{os.path.basename(__file__)} console output from CliRunner:\n")
        print(run_result.output)
    print(f"\n{os.path.basename(__file__)} exit code: {run_result.exit_code}")

    return run_result


if __name__ == "__main__":
    run_cli(
        [
            "scan",
            "--help",
        ]
    )
