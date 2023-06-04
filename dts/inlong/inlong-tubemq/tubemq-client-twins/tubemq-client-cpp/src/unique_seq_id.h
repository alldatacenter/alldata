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

#ifndef TUBEMQ_UNIQUESEQID_H
#define TUBEMQ_UNIQUESEQID_H

#include <stdint.h>

#include <atomic>

namespace tubemq {

class UniqueSeqId {
 public:
  UniqueSeqId() : id_(0) {}

  uint32_t Next() { return id_.fetch_add(1, std::memory_order_relaxed); }

 protected:
  std::atomic<uint32_t> id_;
};

}  // namespace tubemq

#endif
