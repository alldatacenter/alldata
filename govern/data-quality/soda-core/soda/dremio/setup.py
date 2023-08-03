#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-dremio"
package_version = "3.0.47"
description = "Soda Core Dremio Package"

requires = [f"soda-core=={package_version}", "pyodbc", "pyarrow"]

setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
