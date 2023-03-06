#!/usr/bin/env python
import sys

from setuptools import find_namespace_packages, setup

if sys.version_info < (3, 7):
    print("Error: Soda Core requires at least Python 3.7")
    print("Error: Please upgrade your Python version to 3.7 or later")
    sys.exit(1)

package_name = "soda-core-redshift"
package_version = "3.0.26"
description = "Soda Core Redshift Package"

requires = [f"soda-core=={package_version}", "boto3", "psycopg2-binary>=2.8.5, <3.0"]
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
