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


#!/bin/bash

rm tubemq -rf
rm lib -rf
rm *.tar.gz

mkdir tubemq
mkdir tubemq/lib
mkdir tubemq/include
mkdir tubemq/conf
mkdir tubemq/demo
mkdir lib

cp ../build/src/libtubemq.a lib/
cp ../build/third_party/lib/liblog4cplus.a lib/
cp ../build/third_party/lib64/libsnappy.a lib/
cp ../build/proto/libtubemq_proto.a lib/
cp /usr/local/lib/libprotobuf.a lib/
cp -rf ../include/*  tubemq/include/
cp ../conf/* tubemq/conf/
cp ../example/consumer/test_consumer.cc tubemq/demo/
cd lib
for file in *.a
do
    echo "ar x "$file
    ar x $file
done
ar cru libtubemq_rel.a *.o
ranlib libtubemq_rel.a
rm *.o
mv libtubemq_rel.a ../tubemq/lib/
cd ..

cd tubemq/demo
g++ -g -std=c++11 -c test_consumer.cc -I../include/
#g++ -g -std=c++11 -o test_consumer test_consumer.o -L../lib/  -ltubemq -ltubemq_proto -lssl -lcrypto -lpthread -lprotobuf -llog4cplus -lsnappy -lrt
g++ -g -std=c++11 -o test_consumer test_consumer.o -L../lib/  -ltubemq_rel -lssl -lcrypto -lpthread -lrt
rm *.o
cd -
tar -czvf tubemq.tar.gz tubemq/
