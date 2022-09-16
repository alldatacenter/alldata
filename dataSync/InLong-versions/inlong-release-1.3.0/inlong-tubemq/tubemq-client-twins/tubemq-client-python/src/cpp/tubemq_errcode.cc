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
#include "tubemq/tubemq_errcode.h"

namespace py = pybind11;

using namespace tubemq;
using namespace tubemq::err_code;
using std::string;

PYBIND11_MODULE(tubemq_errcode, m) {
    py::enum_<Result>(m, "Result")
        .value("kErrSuccess", Result::kErrSuccess)
        .value("kErrNotReady", Result::kErrNotReady)
        .value("kErrMoved", Result::kErrMoved)
        .value("kErrBadRequest", Result::kErrBadRequest)
        .value("kErrUnAuthorized", Result::kErrUnAuthorized)
        .value("kErrForbidden", Result::kErrForbidden)
        .value("kErrNotFound", Result::kErrNotFound)
        .value("kErrNoPartAssigned", Result::kErrNoPartAssigned)
        .value("kErrAllPartWaiting", Result::kErrAllPartWaiting)
        .value("kErrAllPartInUse", Result::kErrAllPartInUse)
        .value("kErrPartitionOccupied", Result::kErrPartitionOccupied)
        .value("kErrHbNoNode", Result::kErrHbNoNode)
        .value("kErrDuplicatePartition", Result::kErrDuplicatePartition)
        .value("kErrCertificateFailure", Result::kErrCertificateFailure)
        .value("kErrServerOverflow", Result::kErrServerOverflow)
        .value("kErrConsumeGroupForbidden", Result::kErrConsumeGroupForbidden)
        .value("kErrConsumeSpeedLimit", Result::kErrConsumeSpeedLimit)
        .value("kErrConsumeContentForbidden", Result::kErrConsumeContentForbidden)
        .export_values();

    py::class_<ErrorCode>(m, "ErrorCode")
        .def(py::init<>())
        .def(py::init<int, const string&>())
        .def("clear", &ErrorCode::Clear)
        .def("assign", &ErrorCode::Assign)
        .def("value", &ErrorCode::Value)
        .def("message", &ErrorCode::Message);
}