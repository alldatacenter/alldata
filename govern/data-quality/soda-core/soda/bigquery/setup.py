#!/usr/bin/env python

from setuptools import find_namespace_packages, setup

package_name = "soda-core-bigquery"
package_version = "3.0.47"
description = "Soda Core Bigquery Package"

requires = [
    f"soda-core=={package_version}",
    "google-cloud-bigquery>=2.25.0, <4.0",
]
# TODO Fix the params
setup(
    name=package_name,
    version=package_version,
    install_requires=requires,
    packages=find_namespace_packages(include=["soda*"]),
)
