#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-db2"
package_version = "3.0.47"
# TODO Add proper description
description = "Soda Core IBM DB2 Package"

requires = [f"soda-core=={package_version}", "ibm-db==3.1.2"]
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
