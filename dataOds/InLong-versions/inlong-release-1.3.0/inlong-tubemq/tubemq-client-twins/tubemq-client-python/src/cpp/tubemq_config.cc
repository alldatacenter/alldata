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
#include "tubemq/tubemq_config.h"

namespace py = pybind11;

using namespace tubemq;
using std::map;
using std::set;
using std::string;

PYBIND11_MODULE(tubemq_config, m) {
    py::enum_<ConsumePosition>(m, "ConsumePosition")
        .value("kConsumeFromFirstOffset", ConsumePosition::kConsumeFromFirstOffset)
        .value("kConsumeFromLatestOffset", ConsumePosition::kConsumeFromLatestOffset)
        .value("kComsumeFromMaxOffsetAlways", ConsumePosition::kComsumeFromMaxOffsetAlways)
        .export_values();

    py::class_<ConsumerConfig>(m, "ConsumerConfig")
        .def(py::init<>())
        .def("setRpcReadTimeoutMs", &ConsumerConfig::SetRpcReadTimeoutMs)
        .def("setMasterAddrInfo", &ConsumerConfig::SetMasterAddrInfo)
        .def("setGroupConsumeTarget", static_cast<bool(ConsumerConfig::*) \
        (string&, const string&, const set<string>&)>(&ConsumerConfig::SetGroupConsumeTarget), "")
        .def("setGroupConsumeTarget", static_cast<bool(ConsumerConfig::*) \
        (string&, const string&, const map<string, set<string>>&)> \
        (&ConsumerConfig::SetGroupConsumeTarget), "")
        .def("getRpcReadTimeoutMs", &ConsumerConfig::GetRpcReadTimeoutMs)
        .def("setConsumePosition", &ConsumerConfig::SetConsumePosition)
        .def("getMasterAddrInfo", &ConsumerConfig::GetMasterAddrInfo);
}