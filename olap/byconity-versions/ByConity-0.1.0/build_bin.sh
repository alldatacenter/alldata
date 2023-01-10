#!/bin/bash

set -e
set -x
PROJECT="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P  )"

export PATH=`echo $PATH | sed -e 's/:\/opt\/tiger\/typhoon-blade//'`

rm -rf output/
mkdir -p output

export CMAKE_BUILD_TYPE=${CUSTOM_CMAKE_BUILD_TYPE:-RelWithDebInfo}
export CMAKE_FLAGS="-DCMAKE_INSTALL_PREFIX=../output -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE} -DUSE_BYTEDANCE_RDKAFKA=${CUSTOM_USE_BYTEDANCE_RDKAFKA:-1} ${CMAKE_FLAGS}"
CMAKE_FLAGS="-DCMAKE_INSTALL_PREFIX=../output ${CMAKE_FLAGS}"
CMAKE_FLAGS="-DCMAKE_BUILD_TYPE=${CUSTOM_CMAKE_BUILD_TYPE:-RelWithDebInfo} $CMAKE_FLAGS"
CMAKE_FLAGS="-DENABLE_BREAKPAD=ON $CMAKE_FLAGS" # enable minidump
[[ -n "$CUSTOM_SANITIZE" ]] && CMAKE_FLAGS="-DSANITIZE=$CUSTOM_SANITIZE $CMAKE_FLAGS"
[[ -n "$CUSTOM_MAX_LINKING_JOBS" ]] && CMAKE_FLAGS="-DPARALLEL_LINK_JOBS=${CUSTOM_MAX_LINKING_JOBS} ${CMAKE_FLAGS}"
[[ -n "$CUSTOM_MAX_COMPILE_JOBS" ]] && CMAKE_FLAGS="-DPARALLEL_COMPILE_JOBS=${CUSTOM_MAX_COMPILE_JOBS} ${CMAKE_FLAGS}"
export CMAKE_FLAGS


rm -rf build && mkdir build && cd build

source /etc/os-release
if [ "$NAME" == "CentOS Linux" ] && [ "$VERSION_ID" == "7" ] && hash scl 2>/dev/null; then
    echo "Found Centos 7 and scl"
    scl enable devtoolset-9 "CC=clang CXX=clang++ cmake3 ${CMAKE_FLAGS} -DCMAKE_MAKE_PROGRAM:FILEPATH=/usr/bin/ninja ../"
    scl enable devtoolset-9 "ninja"
    scl enable devtoolset-9 "ninja install"
else
    export CC=/usr/bin/clang
    export CXX=/usr/bin/clang++
    cmake ../ ${CMAKE_FLAGS} && ninja
fi

# copy shared libaries
cp ${PROJECT}/contrib/foundationdb/lib/libfdb_c.so ../output/lib

# create the `usr/bin` directory to keep it same with old version
mkdir -p ../output/usr
mv ../output/bin ../output/usr/

# create symlink to make CI tests happy
cd ../output
ln -s usr/bin bin
