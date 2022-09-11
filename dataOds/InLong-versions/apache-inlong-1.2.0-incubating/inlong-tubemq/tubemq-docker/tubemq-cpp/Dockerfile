#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM rikorose/gcc-cmake:gcc-4
RUN apt-get remove openssl -y
RUN curl -LOk https://www.openssl.org/source/openssl-1.1.0f.tar.gz \
    && tar -xzvf openssl-1.1.0f.tar.gz \
    && rm openssl-1.1.0f.tar.gz && cd openssl-1.1.0f \
    && ./config && make && make install
RUN curl -LOk https://github.com/protocolbuffers/protobuf/releases/download/v3.13.0/protobuf-cpp-3.13.0.tar.gz \
    && tar -xzvf protobuf-cpp-3.13.0.tar.gz && rm protobuf-cpp-3.13.0.tar.gz \
    && cd protobuf-3.13.0 && ./autogen.sh \
    && ./configure CXXFLAGS=-fPIC && make \
    && make install && cd /usr/local/lib \
    && ln -snf libprotobuf.so libprotobuf.so.24
ENV LD_LIBRARY_PATH=/usr/local/lib:/usr/local/lib64/
WORKDIR /tubemq-cpp/
