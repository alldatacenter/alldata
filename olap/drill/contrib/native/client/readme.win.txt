/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Building the Drill C++ Client on Windows

This document lists the steps to build the Drill C++ Client on Windows. The
steps and examples are for Windows 7, using Visual Studio 2010 Express. Newer
Windows platforms should be more or less similar.

1 Tools and software

1.1 The following git tools would be useful to get source code and apply patches -
    MSysGit and GitForWindows - http://msysgit.github.io/
    Tortoise SVN  - http://tortoisesvn.net/

1.2 Windows SDK
    The Windows SDK is required to enable 64 bit options for Visual Studio 2010
    Express. You can get it here:
    http://www.microsoft.com/en-us/download/details.aspx?id=8279
    Note: For 64 bit builds, change the platform toolset to Windows SDK for your
    project.
    (Root node, i.e. the projectname) Properties ->Configuration Properties->General->Platform Toolset = Windows7.1SDK
    If you are running Windows 7 and having problem isntalling windows SDK follow the instructions here http://stackoverflow.com/questions/19366006/error-when-installing-windows-sdk-7-1

1.3 [Optional] Windows Driver kit 
    The Windows Driver kit is required to get the 64 bit assembler ml64. The
    32 bit assembler masm is included in VS2010. The assembler is required to build
    boost and can be ignored for Drill
 
    ML64(the 64 bit assembler) can be downloaded from here:
        http://www.microsoft.com/en-us/download/details.aspx?id=11800
    MASM(the 32 bit assembler) should be installed in VC/bin already. If not it
    can be downloaded from here: 
        http://www.microsoft.com/en-us/download/details.aspx?id=12654

    Add the paths to the assemblers in your path environment variable. ml64 can be
    found in (or the appropriate path to the WinDDK installation on your system):
        C:\WinDDK\7600.16385.1\bin\x86\amd64

2 Dependencies

2.0
    a) The Drill client library requires the following third party libraries -
            boost
            zookeeper C API
            protobufs
            cppunit
        The Drill client is linked with STATIC versions of these libraries. The
        libraries are themselves linked with the DYNAMIC C Runtime DLLs. It is
        important that the libraries all have the same linkage model, otherwise the
        Drill Client library will not link successfully.
    b) The build assumes that Zookeeper is not availble as a
        binary installer and is available in source form only for windows. The
        location of the header files, as a result is different from Unix like
        systems. This will be important later with Cmake.
    c) The document below will refer to the following as the directories where
        the thirdparty library source is installed.
            BOOST_HOME - Directory where boost source is installed. 
            ZOOKEEPER_HOME - Directory where Zookeeper source is installed. Note that
            this is the directory for the full Zookeeper source not just the
            source for the C library.
            PROTOBUF_HOME - Directory where Protobuf source is installed.
            CPPUNIT_HOME - Directory where CPPUnit source is installed
    d) The build assumes that Powershell is installed

2.1 Boost (version 1.55)
    a) Download Boost from:
        i) http://www.boost.org/users/history/version_1_55_0.html
        ii) open boost_1_55_0\boost/archive/iterators/transform_width.hpp and add the following to the include statements: #include <algorithm>
        iii) Yes somehow this header was not included and has been missed! See here for more info: https://svn.boost.org/trac/boost/ticket/8757
    b) i) Boost 32 bit build - 
        Open a  Visual Studio command prompt from the Visual Studio IDE
      ii) Boost 64 bit build -
          Open a command prompt from the Windows SDK menu 
            Start->All Programs->Windows SDK 7.1->Start Command Prompt
    c) In the command prompt window - 
        C:> cd <BOOST_HOME>
        C:> .\bootstrap.bat
    d) Choose the build type (64 bit, 32 bit) and the variant type (debug, release)
    and build the libraries. Boost build will write the libraries to
    <BOOST_HOME>/stage/lib. Copy them to an appropriately named directory

        C:> .\b2 variant=debug link=static threading=multi address-model=64 toolset=msvc-10.0 runtime-link=shared
        C:>  mkdir Debug64
        C:>  copy stage\lib\* Debug64

        C:> .\b2 variant=release link=static threading=multi address-model=64 toolset=msvc-10.0 runtime-link=shared
        C:>  mkdir Release64
        C:>  copy stage\lib\* Release64

        C:> .\b2 variant=debug link=static threading=multi address-model=32 toolset=msvc-10.0 runtime-link=shared
        C:>  mkdir Debug32
        C:>  copy stage\lib\* Debug32

        C:> .\b2 variant=release link=static threading=multi address-model=32 toolset=msvc-10.0 runtime-link=shared
        C:>  mkdir Release32
        C:>  copy stage\lib\* Release32
    e) Notes: 
        i) For more information on Boost build 
            http://www.boost.org/doc/libs/1_55_0/more/getting_started/windows.html#the-boost-distribution
        ii) Detail options for b2 -
            http://www.boost.org/boost-build2/doc/html/bbv2/overview/invocation.html
        iii) If you do not have the 64 bit assembler installed, boost-context does not
        build. It is safe to ignore it as boost-context is not needed for Drill

2.2 Protobuf (3.6.1)
    Get protobuf from here: https://github.com/protocolbuffers/protobuf/releases/download/v3.6.1/protoc-3.6.1-win32.zip

    a) Protobuf builds static libraries
    b) In Visual Studio, open <PROTOBUF_HOME>/vsprojects/protobuf.sln. The IDE may
    update the solution file. This should go thru successfully.
    c) If build for 64 bit, add a 64 bit project configuration for each project. (Make sure the
        platform toolset is set to Windows7.1SDK)
    d) Build the protobuf project first (not the solution)
    e) Build the solution!

2.3 Zookeeper (3.4.6) 
    a) Set the ZOOKEEPER_HOME environment variable
    b) The 3.4.6 release of Zookeeper does not build correctly on 64 bit windows. To
    fix that for the 64 bit build, apply patch zookeeper-3.4.6-x64.patch
    For example in Msysgit 
        $ cd <ZOOKEEPER_HOME> && git apply <DRILL_HOME>/contrib/native/client/patches/zookeeper-3.4.6-x64.patch
    c) In Visual Studio 2010 Express open <ZOOKEEPER_HOME>/src/c/zookeeper.sln
        i) Add a 64 bit project configuration for each project. (Make sure the
            platform toolset is set to Windows7.1SDK)
       ii) Change the output type for the zookeeper project to a static lib
            Properties->Configuration Properties->General->Configuration Type = Static Library
      iii) In the cli project add the preprocessor define USE_STATIC_LIB
       iv) Build. Build zookeeper lib first, then build cli 

2.4 CppUnit (3.4.6) 
    a) Download cppunit and unzip/untar it. 
       Latest version is available at: http://dev-www.libreoffice.org/src/cppunit-1.13.2.tar.gz
       More informations: https://www.freedesktop.org/wiki/Software/cppunit/
    b) Set the CPPUNIT_HOME environment variable
    c) InVisual Studio 2010 Express open <CPPUNIT_HOME>/src/CppUnitLibraries2010.sln
       i) Build cppunit project

2.5 Install or build Cyrus SASL
   To build your own see readme.sasl for instructions

2.6 Install OpenSSL
   Download from https://slproweb.com/products/Win32OpenSSL.html
   At the time of writing the compatible version is Win32OpenSSL-1_0_2L
   OpenSSL is installed into C:\OpenSSL-Win64, If you install DLL's into bin directory, make sure the directory is added to the PATH


3 Building Drill Clientlib
3.1 SET the following environment variables
    set BOOST_LIBRARYDIR=<BOOST_HOME>\BUILD_TYPE
    set BOOST_INCLUDEDIR=<BOOST_HOME>

3.2 Generate the Visual Studio Solutions file
    C:> cd <DRILL_HOME>/contrib/native/client
    C:> mkdir build 
    C:> cd build

    a) For the 32 bit build :
        C:> cmake -G "Visual Studio 10" -D ZOOKEEPER_HOME=<ZOOKEPER_HOME> -D PROTOBUF_SRC_ROOT_FOLDER=<PROTOBUF_HOME> -D CPPUNIT_HOME=<CPPUNIT_HOME> -D SASL_LIBRARY="<SASL_HOME>\lib64_debug\libsasl.lib" -D SASL_HOME=<SASL_HOME> -D OPENSSL_ROOT=<OPENSSL_HOME> -D CMAKE_BUILD_TYPE=Debug   ..

    b) For the 64 bit build :
        C:> cmake -G "Visual Studio 10 Win64 " -D ZOOKEEPER_HOME=<ZOOKEPER_HOME> -D PROTOBUF_SRC_ROOT_FOLDER=<PROTOBUF_HOME> -D CPPUNIT_HOME=<CPPUNIT_HOME> -D SASL_LIBRARY="<SASL_HOME>\lib64_debug\libsasl.lib" -D SASL_HOME=<SASL_HOME> -D OPENSSL_ROOT=<OPENSSL_HOME> -D CMAKE_BUILD_TYPE=Debug   ..

3.3 Open the generated <DRILL_HOME>/contrib/native/client/build/drillclient.sln 
    file in Visual Studio.
    a) If doing a Debug build, check the link libraries for the clientlib
    project. You might see the protobuf library being linked from the Release
    build. Correct that to pick up the library from the Debug build. (See 4.2
    below)

3.4 Select the ALL_BUILD project and build.

4 Common Problems 
4.1 In the 64 bit build, If you get: " error LNK1112: module machine type 'x64' conflicts with target machine type 'X86'"
    a) Some library is a 32 bit library and you're linking it into a 64 bit Drill
    Client library, or vice versa. You can check the type of the libraryi with
    dumpbin. 
        dumpbin library_or_dll /headers
    b) Check the linker settings for the project.
        Properties->Configuration Properties->Linker->Command Line->Additional Options. 
    If you're doing a 64 bit build and you see "/machine:X86", remove it and rebuild

4.2) error LNK2038: mismatch detected for '_ITERATOR_DEBUG_LEVEL': value '0' doesn't match value '2' in decimalUtils.obj
    a) This happens if you are doing a Debug build and linking a Release build
    version of some library, or vice versa. Check the link libraries. 
        Properties->Configuration Properties->Linker->Input->Additional Dependencies. 
    Check the libraries are all the same as your configuration (all debug, or all
    release). 
    In particular, for debug builds, check the path of the protobuf library.
	
5 Testing with querySubmitter
querySubmitter query="select * from INFORMAITON_SCHEMA.SCHEMATA" type=sql connectStr=local=192.168.39.43:31010 api=sync logLevel=trace user=yourUserName password=yourPassWord
