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

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <stdlib.h>
#include "tubemq/tubemq_return.h"

namespace py = pybind11;

using namespace tubemq;
using std::string;

PYBIND11_MODULE(tubemq_return, m) {
    py::class_<ConsumerResult>(m, "ConsumerResult")
        .def(py::init<>())
        .def(py::init<const ConsumerResult&>())
        .def(py::init<int32_t, string>())
        .def("setFailureResult", static_cast<void(ConsumerResult::*) \
        (int32_t, string)>(&ConsumerResult::SetFailureResult), "set Failure Result")
        .def("isSuccess", &ConsumerResult::IsSuccess)
        .def("getErrCode", &ConsumerResult::GetErrCode)
        .def("getErrMessage", &ConsumerResult::GetErrMessage)
        .def("getTopicName", &ConsumerResult::GetTopicName)
        .def("getPeerInfo", &ConsumerResult::GetPeerInfo)
        .def("getConfirmContext", &ConsumerResult::GetConfirmContext)
        .def("getMessageList", &ConsumerResult::GetMessageList)
        .def("getPartitionKey", &ConsumerResult::GetPartitionKey)
        .def("getCurrOffset", &ConsumerResult::GetCurrOffset);
}