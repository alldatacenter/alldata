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

#ifndef TUBEMQ_CLIENT_CONST_ERR_CODE_H_
#define TUBEMQ_CLIENT_CONST_ERR_CODE_H_

#include <string>

namespace tubemq {

namespace err_code {

/**
 * Collection of return codes
 */
enum Result {
  kErrSuccess = 200,
  kErrNotReady = 201,
  kErrMoved = 301,
  kErrBadRequest = 400,
  kErrUnAuthorized = 401,
  kErrForbidden = 403,
  kErrNotFound = 404,
  kErrNoPartAssigned = 406,
  kErrAllPartWaiting = 407,
  kErrAllPartInUse = 408,
  kErrPartitionOccupied = 410,
  kErrHbNoNode = 411,
  kErrDuplicatePartition = 412,
  kErrCertificateFailure = 415,
  kErrServerOverflow = 419,
  kErrConsumeGroupForbidden = 450,
  kErrConsumeSpeedLimit = 452,
  kErrConsumeContentForbidden = 455,

  kErrServerError = 500,
  kErrRcvThrowError = 501,
  kErrServiceUnavilable = 503,
  kErrServerMsgsetNullError = 510,
  kErrWaitServerRspTimeout = 550,
  kErrNetWorkTimeout = 1000,
  kErrNetworkError = 1001,
  kErrServerStop = 2001,
  kErrMQServiceStop = 2002,
  kErrClientStop = 2003,
  kErrConfirmTimeout = 2004,
  kErrParseFailure = 5001,
};
}  // namespace err_code

// Class to represent an error code value.
class ErrorCode {
 public:
  // Default constructor.
  ErrorCode() : value_(err_code::kErrSuccess), error_msg_("") {}

  // Construct with specific error code and category.
  ErrorCode(int v, const std::string& c) : value_(v), error_msg_(c) {}

  // Clear the error value to the default.
  void Clear() {
    value_ = 0;
    error_msg_.clear();
  }

  // Assign a new error value.
  void Assign(int v, const std::string& c) {
    value_ = v;
    error_msg_ = c;
  }

  /// Get the error value.
  int Value() const { return value_; }

  // Get the error message.
  const std::string& Message() const { return error_msg_; }

  // Operator to test if the error represents success.
  bool operator!() const { return value_ == 0; }

  // Equality operator to compare two error objects.
  friend bool operator==(const ErrorCode& e1, const ErrorCode& e2) {
    return e1.value_ == e2.value_;
  }

  // Inequality operator to compare two error objects.
  friend bool operator!=(const ErrorCode& e1, const ErrorCode& e2) {
    return e1.value_ != e2.value_;
  }

 private:
  // The value associated with the error code.
  int value_;

  // The error msg associated with the error code.
  std::string error_msg_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_CONST_ERR_CODE_H_
