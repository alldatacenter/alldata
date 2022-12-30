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
#include <stdint.h>
#include <stdio.h>

#include "tubemq/tubemq_message.h"

namespace py = pybind11;

using namespace tubemq;
using std::string;
using std::map;

PYBIND11_MODULE(tubemq_message, m) {
    py::class_<Message>(m, "Message")
        .def(py::init<>())
        .def(py::init<const Message&>())
        .def(py::init<const string&, const char*, uint32_t>())
        .def("getMessageId", &Message::GetMessageId)
        .def("setMessageId", &Message::SetMessageId)
        .def("getTopic", &Message::GetTopic)
        .def("setTopic", &Message::SetTopic)
        .def("getMsgData", &Message::GetVectorData)
        .def("getDataLength", &Message::GetDataLength)
        .def("getFlag", &Message::GetFlag)
        .def("setFlag", &Message::SetFlag)
        .def("getProperties", &Message::GetProperties)
        .def("hasProperty", &Message::HasProperty)
        .def("getProperty", &Message::GetProperty)
        .def("getFilterItem", &Message::GetFilterItem)
        .def("addProperty", &Message::AddProperty);
}