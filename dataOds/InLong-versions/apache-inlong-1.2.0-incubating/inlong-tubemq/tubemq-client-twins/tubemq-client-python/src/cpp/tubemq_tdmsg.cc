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
#include "tubemq/tubemq_tdmsg.h"

namespace py = pybind11;

using namespace tubemq;
using std::string;

PYBIND11_MODULE(tubemq_tdmsg, m) {
    py::class_<TubeMQTDMsg>(m, "TubeMQTDMsg")
        .def(py::init<>())
        .def("parseTDMsg", static_cast<bool(TubeMQTDMsg::*) \
        (const vector<char>&, string&)>(&TubeMQTDMsg::ParseTDMsg), "parse TDMsg")
        .def("clear", &TubeMQTDMsg::Clear)
        .def("getVersion", &TubeMQTDMsg::GetVersion)
        .def("isNumBid", &TubeMQTDMsg::IsNumBid)
        .def("getAttrCount", &TubeMQTDMsg::GetAttrCount)
        .def("getCreateTime", &TubeMQTDMsg::GetCreateTime)
        .def("parseAttrValue", &TubeMQTDMsg::ParseAttrValue)
        .def("getAttr2DataMap", &TubeMQTDMsg::GetAttr2DataMap);

    py::class_<DataItem>(m, "DataItem")
        .def(py::init<>())
        .def(py::init<const DataItem&>())
        .def(py::init<const uint32_t, const char*>())
        .def("getLength", &DataItem::GetLength)
        .def("getData", [](const DataItem& t) {
                const char* data = t.GetData();
                size_t length = t.GetLength();
                return py::bytes(data, length);
        });
}
