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
#include <functional>
#include <iostream>
#include <string>
#include <thread>

#include "executor_pool.h"

using namespace std;
using namespace tubemq;

void handler(int a, const asio::error_code& error) {
  if (!error) {
    // Timer expired.
    std::cout << "handlertimeout:" << a << endl;
  }
}

int main() {
  using namespace std::placeholders;  // for _1, _2, _3...
  ExecutorPool pool(4);
  auto timer = pool.Get()->CreateSteadyTimer();
  timer->expires_after(std::chrono::seconds(1));
  std::cout << "startwait" << endl;
  timer->wait();
  std::cout << "endwait" << endl;

  timer->expires_after(std::chrono::milliseconds(100));
  std::cout << "startsyncwait" << endl;
  timer->async_wait(std::bind(handler, 5, _1));
  std::cout << "endsyncwait" << endl;
  std::this_thread::sleep_for(std::chrono::seconds(1));
  timer->expires_after(std::chrono::milliseconds(100));
  timer->async_wait(std::bind(handler, 6, _1));
  std::this_thread::sleep_for(std::chrono::seconds(1));
  return 0;
}

