# Developer documentation

This is a guide providing pointers to all tasks related to the development of Soda Core and Soda Checks Language.

## Get started

To contribute, fork the `sodadata/soda-core` GitHub repo.

## Folder structure

```
soda-core project root folder
├── soda                  # Root for all Python packages
│   ├── core              # Root for the soda-core package
│   │   ├── soda          # Python source code for the soda-core package
│   │   └── tests         # Test suite code and artefacts for soda-core package
│   ├── scientific        # Root for the scientific package
│   ├── postgres          # Root for the soda-core-postgres package
│   ├── snowflake         # Root for the soda-core-snowflake package
│   └── ...               # Root for the other data source packages
├── scripts               # Scripts for developer workflows
├── dev-requirements.in   # Test suite dependencies
├── dev-requirements.txt  # Generated test suite dependencies
├── requirements.txt      # Generated test suite dependencies
├── LICENSE               # Apache 2.0 license
└── README.md             # Pointer to the online docs for end users and github home page
```

## Requirements

* Python 3.8 or greater.

To check the version of your existing Python install, use:
```
> python --version
Python 3.8.12
>
```

Although not required, we recommend using [pyenv](https://github.com/pyenv/pyenv) or [virtualenv](https://virtualenv.pypa.io/en/latest/) to more easily manage multiple Python
versions.

## Create a virtual environment

This repo includes a convenient script to create a virtual environment.

The `scripts/recreate_venv.sh` script installs the dependencies in your virtual environment.  Review the contents of the file
as inspiration if you want to manage the virtual environment yourself.

```
> scripts/recreate_venv.sh
Requirement already satisfied: pip in ./.venv/lib/python3.8/site-packages (21.1.1)
Collecting pip
  Using cached pip-21.3.1-py3-none-any.whl (1.7 MB)
Installing collected packages: pip
  Attempting uninstall: pip

...lots of output and downloading...

Successfully installed Jinja2-2.11.3 MarkupSafe-2.0.1 cffi-1.15.0 click-8.0.3 cryptography-3.3.2 pycparser-2.21 ruamel.yaml-0.17.17 ruamel.yaml.clib-0.2.6 soda-sql-core-v3-3.0.0-prerelease-1
>
```

## Activate a virtual environment

```
source .venv/bin/activate
```
To deactivate the virtual environment, use the following command:

```
deactivate
```

## Running tests

### Postgres test database as a docker container

Running the test suite requires a Postgres DB running on localhost having a user `sodasql`
without a password, database `sodasql` with a `public` schema.  Simplest way to get one
up and running is

```shell
docker-compose -f soda/postgres/docker-compose.yml up --remove-orphans
```
This will launch a docker container with postgres on your machine available on the default postgres port
needed for running the test suite.

This command is also available as `scripts/start_postgres_container.sh`

### Running the basic test suite

This requires an [active virtual environment](#activate-a-virtual-environment).

```shell
> python3 -m pytest soda/core/tests/
```

Output may show warnings and should look like:
```
> python3 -m pytest soda/core/tests/
=============================================================== test session starts ===============================================================
platform darwin -- Python 3.8.12, pytest-7.0.1, pluggy-1.0.0
...
soda/core/tests/unit/test_telemetry.py::test_fail_secret[something-secret] PASSED                                                           [ 98%]
soda/core/tests/unit/test_telemetry.py::test_non_soda_span_filtering PASSED                                                                 [ 99%]
soda/core/tests/unit/test_variables.py::test_variables PASSED                                                                               [100%]

================================================================ warnings summary =================================================================
.venv/lib/python3.8/site-packages/opentelemetry/sdk/trace/__init__.py:1144
  /Users/tom/Code/soda-core/.venv/lib/python3.8/site-packages/opentelemetry/sdk/trace/__init__.py:1144: DeprecationWarning: Call to deprecated method __init__. (You should use InstrumentationScope) -- Deprecated since version 1.11.1.
    InstrumentationInfo(

-- Docs: https://docs.pytest.org/en/stable/how-to/capture-warnings.html
=================================================== 129 passed, 21 skipped, 1 warning in 3.47s ====================================================

```

Activating the virtual environment and running the tests is also available as `scripts/run_tests.sh`

Before pushing commits or asking to review a pull request, we ask that you verify successful execution of
the following test suite on your machine.

### Running tests in your IDE

Configure the following source paths:
* `soda/core`
* `soda/scientific`
* `soda/athena`
* `soda/bigquery`
* `soda/postgres`
* `soda/snowflake`
* `soda/spark`
* `soda/spark_df`

### Running local tests on a specific data source

Copy `.env.example` to `.env` and update the contents.  Ask one of the other engineers to help get the credentials.

By default, the test suite will run on [your local postgres](#postgres-test-database-as-a-docker-container).

In your `.env`, uncomment one of the following environment variables to run on a different data source:
```
# test_data_source=athena
# test_data_source=bigquery
# test_data_source=postgres
# test_data_source=redshift
# test_data_source=snowflake
# test_data_source=spark
# test_data_source=spark_df
```

## Testing with Tox

The CI environment uses [Tox]() to run the test suite matrix combinations.

```
tox -- soda -k soda/snowflake
```

I believe this will launch separate test container(s), even if you already have a local postgres running.

## CI

CI is configured in `.github/workflows/workflow.yml`

The secrets used in that file are configured in GitHub: [https://github.com/sodadata/soda-core/settings/secrets/actions](https://github.com/sodadata/soda-core/settings/secrets/actions)

### Testing cross cutting concerns

There are a couple of cross cutting concerns that need to be tested over a variety of functional
test scenarios.  To do this, we introduce environment variables that if set, activate the cross
cutting feature while executing the full test suite.

TODO update this list!  I think some of these are obsolete.

* `export WESTMALLE=LEKKER` : activates soda cloud connection
* `export CHIMAY=YUMMIE` : activates local storage of files
* `export ROCHEFORT=HMMM` : activates notifications


## Code guidelines and style

- CI runs [pre-commit](http://pre-commit.com) hooks and uses [pre-commit CI](https://pre-commit.ci) to do all code style and formatting for us, so we do not need to sweat about it. In case you want, you can run the hooks locally using `pre-commit run --all-files`.
- Try to strike a balance between “self-explanatory clean code” and using comments.
- Follow [git commit](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) message guidelines. Always reference a github issue in the body of the commit message using `#xxx` format.
- Use latest Python code style whenever applicable and reasonable:
    - 3.9+ style type annotations, e.g. `dict` over `Dict`, use  `str | None` over `Optional[str]` etc.
