# -*- coding: utf-8 -*-

from setuptools import setup, find_packages
from codecs import open
from os import path

__author__ = 'adonis'
version_info = (2, 1, 1)
__version__ = ".".join([str(v) for v in version_info])
# from teslafaas import __version__, __author__

here = path.abspath(path.dirname(__file__))

print 'here: ', here
# Get the long description from the README file
with open(path.join(here, 'README.rst'), encoding='utf-8') as f:
    long_description = f.read()

# read requirements
with open(path.join(here, 'requirements.txt'), encoding='utf-8') as f:
    requires = [l.strip() for l in f.readlines()]


setup(
    name='tesla-faas2',

    version=__version__,

    description='Tesla Faas',
    long_description=long_description,

    # The project's main homepage.
    url='http://gitlab.alibaba-inc.com/alisre/tesla-faas',

    # Author details
    author=__author__,

    author_email='dongdong.ldd@alibaba-inc.com',

    long_description_content_type='text/markdown',

    # Choose your license
    license='ALIBABA',

    scripts=[],

    # See https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[
        # How mature is this project? Common values are
        #   3 - Alpha
        #   4 - Beta
        #   5 - Production/Stable
        'Development Status :: 4 - Beta',

        # Indicate who your project is intended for
        'Intended Audience :: Developers',
        'Topic :: Software Development :: Tesla Middleware',

        # Pick your license as you wish (should match "license" above)
        'License :: ALIBABA',

        # Specify the Python versions you support here. In particular, ensure
        # that you indicate whether you support Python 2, Python 3 or both.
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7'
    ],

    # What does your project relate to?
    keywords='Tesla FaaS, ServerLess',

    # You can just specify the packages manually here if your project is
    # simple. Or you can use find_packages().
    # packages=find_packages(exclude=['contrib', 'docs', 'tests', 'bin']),
    packages=['teslafaas', ],
    # include resource in MANIFIST.in, it is more general and works for sdist source tar
    # package_data={'': ['teslafaas/logging.json']},
    include_package_data=True,

    # test_suite='setup.test_suite',
    # test_suite='test_suite_loader.test_suite',
    # tests_require=[
    #     'six',
    #     'httpretty',
    #     'sure',
    #     'mocket',
    # ],

    # Alternatively, if you want to distribute just a my_module.py, uncomment
    # this:
    #   py_modules=["my_module"],

    # List run-time dependencies here.  These will be installed by pip when
    # your project is installed. For an analysis of "install_requires" vs pip's
    # requirements files see:
    # https://packaging.python.org/en/latest/requirements.html
    install_requires=requires,

    dependency_links=[
        # Tesla SDK
        'http://gitlab.alibaba-inc.com/pe3/tesla-sdk-python.git@relase/5.7.4#egg=tesla_sdk',
        # 'http://gitlab-sc.alibaba-inc.com/pe3/tesla-sdk-python.git@relase/5.7.4',
    ]

    # List additional groups of dependencies here (e.g. development
    # dependencies). You can install these using the following syntax,
    # for example:
    # $ pip install -e .[dev,test]
    # extras_require={
    #    'dev': ['check-manifest'],
    #    'test': ['coverage'],
    # },

    # If there are data files included in your packages that need to be
    # installed, specify them here.  If using Python 2.6 or less, then these
    # have to be included in MANIFEST.in as well.
    # package_data={
    #    'sample': ['package_data.dat'],
    # },

    # Although 'package_data' is the preferred approach, in some case you may
    # need to place data files outside of your packages. See:
    # http://docs.python.org/3.4/distutils/setupscript.html#installing-additional-files # noqa
    # In this case, 'data_file' will be installed into '<sys.prefix>/my_data'
    # data_files=[('my_data', ['data/data_file'])],

    # To provide executable scripts, use entry points in preference to the
    # "scripts" keyword. Entry points provide cross-platform support and allow
    # pip to create the appropriate form of executable for the target platform.
    # entry_points={
    #    'console_scripts': [
    #        'sample=sample:main',
    #    ],
    # },
)
