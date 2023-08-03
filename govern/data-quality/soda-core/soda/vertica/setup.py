#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-vertica"
package_version = "3.0.47"
description = "Soda Core Vertica Package"

requires = [f"soda-core=={package_version}", "vertica-python>=1.0.3, <2.0"]

setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
