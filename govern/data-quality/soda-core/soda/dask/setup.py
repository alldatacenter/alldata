#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-pandas-dask"
package_version = "3.0.47"
description = "Soda Core Dask Package"

requires = [f"soda-core=={package_version}", "dask>=2022.10.0", "dask-sql>=2022.12.0,<2023.6.0"]

setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
