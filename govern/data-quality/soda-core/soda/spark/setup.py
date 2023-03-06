#!/usr/bin/env python
import sys

from setuptools import find_namespace_packages, setup

if sys.version_info < (3, 7):
    print("Error: Soda Core requires at least Python 3.7")
    print("Error: Please upgrade your Python version to 3.7 or later")
    sys.exit(1)

package_name = "soda-core-spark"
package_version = "3.0.26"
description = "Soda Core Spark Package"

requires = [f"soda-core=={package_version}"]

extras = {
    "hive": [
        "PyHive[hive]",
    ],
    "odbc": [
        "pyodbc",
    ],
    "databricks": ["databricks-sql-connector"],
}
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
    extras_require=extras,
)
