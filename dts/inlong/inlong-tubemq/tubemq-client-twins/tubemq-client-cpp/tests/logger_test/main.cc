/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include <chrono>
#include <exception>
#include <iostream>
#include <string>
#include <thread>

#include "buffer.h"
#include "logger.h"
#include "tubemq/tubemq_atomic.h"

using namespace std;
using namespace tubemq;

AtomicInteger ati;

void logfunc() {
  while (1) {
    LOG_TRACE("atomic:%d", ati.IncrementAndGet());
  }
}

int main() {
  {
    auto buf = std::make_shared<Buffer>();
    std::string data = "abcdef";
    buf->Write(data.data(), data.size());
    auto buf2 = buf->Slice();
    buf2->ReadUint32();
    buf->Write(data.data(), data.size());
    printf("%s\n", buf->String().c_str());
    printf("%s\n", buf2->String().c_str());
  }

  ati.GetAndSet(1);
  GetLogger().Init("./tubemq", tubemq::Logger::Level(0));
  std::thread t1(logfunc);
  std::thread t2(logfunc);
  std::thread t3(logfunc);
  std::thread t4(logfunc);
  t1.join();
  return 0;
}

