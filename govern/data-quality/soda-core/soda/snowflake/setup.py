#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-snowflake"
package_version = "3.0.47"
description = "Soda Core Snowflake Package"

requires = [f"soda-core=={package_version}", "snowflake-connector-python~=3.0"]
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
