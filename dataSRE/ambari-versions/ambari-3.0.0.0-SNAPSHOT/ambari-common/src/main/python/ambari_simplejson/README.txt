Simplejson 3.16.1
-------------------

Standard python json library with included speedups library for better performance. For speedup it uses
 C library _speedups.so, without this library it works the same as bundled json module.


_speedups folder content:
/
|-__init__.py
|-win  # speedups for win x64 platform
|  |-_speedups.pyd
|  |-__init__.py
|-linux
|  |-usc2 # linux speedups for python compiled with PyUnicodeUCS2_* functions set
|  |  |-_speedups.so
|  |  |-__init__.py
|  |-usc4 # linux speedups for python compiled with PyUnicodeUCS4_* functions set
|     |-_speedups.so
|     |-__init__.py
|-ppc
   |-_speedups.so
   |-__init__.py

USC2/USC4 Explanation:
     - https://docs.python.org/2/faq/extending.html#when-importing-module-x-why-do-i-get-undefined-symbol-pyunicodeucs2

How to build _speedups.so manually for custom distributive or architecture
-----------------------------------------------------------------------
 - Install development tools, for example on CentOS it could be done by command: yum group install "Development Tools"
 - Install python-devel or python-dev package (depends on linux distribution)
 - create setup.py file in the some folder as _speedups.c with content like below:

 from distutils.core import setup, Extension
 setup(name='simplejson', version='3.16.1',
       ext_modules=[
         Extension("ambari_commons._speedups", ["_speedups.c"])
       ])

 - Run python setup.py build
 - Check build folder for compiled library
 - Place resulting file to "ambari_simplejson/_speedups/<your platform>/" folder and create empty __init__.py at the same path
 - Add newly added library path to c extension loader at ambari_simplejson/_import_paths.py#get -> _import_paths
