import sys
import os
from setuptools import setup, find_packages
from pathlib import Path


# Use the README.md from /docs
root_path = Path(__file__).resolve().parent.parent
readme_path = root_path / "docs/README.md"
if readme_path.exists():
    long_description = readme_path.read_text(encoding="utf8")
else:
    # In some build environments (specifically in conda), we may not have the README file
    # readily available. In these cases, just set long_description to the URL of README.md.
    long_description = "See https://github.com/feathr-ai/feathr/blob/main/docs/README.md"

try:
    exec(open("feathr/version.py").read())
except IOError:
    print("Failed to load Feathr version file for packaging.",
          file=sys.stderr)
    # Temp workaround for conda build. For long term fix, Jay will need to update manifest.in file.
    VERSION = "1.0.0"

VERSION = __version__  # noqa
os.environ["FEATHR_VERSION"] = VERSION

extras_require=dict(
    dev=[
        "black>=22.1.0",    # formatter
        "isort",            # sort import statements
        "pytest>=7",
        "pytest-cov",
        "pytest-xdist",
        "pytest-mock>=3.8.1",
    ],
    notebook=[
        "azure-cli==2.37.0",
        "jupyter>=1.0.0",
        "matplotlib>=3.6.1",
        "papermill>=2.1.2,<3",      # to test run notebooks
        "scrapbook>=0.5.0,<1.0.0",  # to scrap notebook outputs
        "scikit-learn",             # for notebook examples
        "plotly",                   # for plotting
    ],
)
extras_require["all"] = list(set(sum([*extras_require.values()], [])))

setup(
    name='feathr',
    version=VERSION,
    long_description=long_description,
    long_description_content_type="text/markdown",
    author_email="feathr-technical-discuss@lists.lfaidata.foundation",
    description="An Enterprise-Grade, High Performance Feature Store",
    url="https://github.com/feathr-ai/feathr",
    project_urls={
        "Bug Tracker": "https://github.com/feathr-ai/feathr/issues",
    },
    packages=find_packages(),
    include_package_data=True,
    # consider
    install_requires=[
        "click<=8.1.3",
        "py4j<=0.10.9.7",
        "loguru<=0.6.0",
        "pandas",
        "redis<=4.4.0",
        "requests<=2.28.1",
        "tqdm<=4.64.1",
        "pyapacheatlas<=0.14.0",
        "pyhocon<=0.3.59",
        "pandavro<=1.7.1",
        "pyyaml<=6.0",
        "Jinja2<=3.1.2",
        "pyarrow<=9.0.0",
        "pyspark>=3.1.2",  # TODO upgrade the version once pyspark publishes new release to resolve `AttributeError: module 'numpy' has no attribute 'bool'`
        "python-snappy<=0.6.1",
        "deltalake>=0.6.2",
        "graphlib_backport<=1.0.3",
        "protobuf<=3.19.4,>=3.0.0",
        "confluent-kafka<=1.9.2",
        "databricks-cli<=0.17.3",
        "avro<=1.11.1",
        "azure-storage-file-datalake<=12.5.0",
        "azure-synapse-spark",
        # Synapse's aiohttp package is old and does not work with Feathr. We pin to a newer version here.
        "aiohttp==3.8.3",
        # fixing Azure Machine Learning authentication issue per https://stackoverflow.com/a/72262694/3193073
        "azure-identity>=1.8.0",
        "azure-keyvault-secrets<=4.6.0",
        # In 1.23.0, azure-core is using ParamSpec which might cause issues in some of the databricks runtime.
        # see this for more details:
        # https://github.com/Azure/azure-sdk-for-python/pull/22891
        # using a version lower than that to workaround this issue.
        "azure-core<=1.22.1",
        # azure-core 1.22.1 is dependent on msrest==0.6.21, if an environment(AML) has a different version of azure-core (say 1.24.0),
        # it brings a different version of msrest(0.7.0) which is incompatible with azure-core==1.22.1. Hence we need to pin it.
        # See this for more details: https://github.com/Azure/azure-sdk-for-python/issues/24765
        "msrest<=0.6.21",
        "typing_extensions>=4.2.0"
    ],
    tests_require=[  # TODO: This has been depricated
        "pytest",
    ],
    extras_require=extras_require,
    entry_points={
        'console_scripts': ['feathr=feathrcli.cli:cli']
    },
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.7"
)