#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-mysql"
package_version = "3.0.47"
description = "Soda Core MySQL Package"

requires = [
    f"soda-core=={package_version}",
    "mysql-connector-python==8.0.30",
]
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
