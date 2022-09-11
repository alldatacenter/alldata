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
#include "tubemq/tubemq_client.h"

namespace py = pybind11;

using namespace tubemq;
using std::string;

PYBIND11_MODULE(tubemq_client, m) {
    m.def("startTubeMQService", (bool (*)(string&, const string&) ) &StartTubeMQService, "start TubeMQ Service");
    m.def("stopTubeMQService", &StopTubeMQService, "stop TubeMQ Service");

    py::class_<TubeMQConsumer>(m, "TubeMQConsumer")
        .def(py::init<>())
        .def("start", &TubeMQConsumer::Start)
        .def("shutDown", &TubeMQConsumer::ShutDown)
        .def("getClientId", &TubeMQConsumer::GetClientId)
        .def("getMessage", &TubeMQConsumer::GetMessage)
        .def("confirm", &TubeMQConsumer::Confirm)
        .def("getCurConsumedInfo", &TubeMQConsumer::GetCurConsumedInfo);
}