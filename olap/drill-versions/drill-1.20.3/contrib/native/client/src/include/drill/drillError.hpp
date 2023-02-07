/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef DRILL_ERROR_H
#define DRILL_ERROR_H

#include "drill/common.hpp"

#if defined _WIN32 || defined __CYGWIN__
  #ifdef DRILL_CLIENT_EXPORTS
      #define DECLSPEC_DRILL_CLIENT __declspec(dllexport)
  #else
    #ifdef USE_STATIC_LIBDRILL
      #define DECLSPEC_DRILL_CLIENT
    #else
      #define DECLSPEC_DRILL_CLIENT  __declspec(dllimport)
    #endif
  #endif
#else
  #if __GNUC__ >= 4
    #define DECLSPEC_DRILL_CLIENT __attribute__ ((visibility ("default")))
  #else
    #define DECLSPEC_DRILL_CLIENT
  #endif
#endif

namespace exec{
    namespace shared{
        class DrillPBError;
    };
};

namespace Drill{

class DECLSPEC_DRILL_CLIENT DrillClientError{
    public:
        static const uint32_t CONN_ERROR_START = 100;
        static const uint32_t QRY_ERROR_START =  200;

        DrillClientError(uint32_t s, uint32_t e, char* m){status=s; errnum=e; msg=m;};
        DrillClientError(uint32_t s, uint32_t e, std::string m){status=s; errnum=e; msg=m;};
        //copy ctor
        DrillClientError(const DrillClientError& err){status=err.status; errnum=err.errnum; msg=err.msg;};

        static DrillClientError*  getErrorObject(const exec::shared::DrillPBError& e);

        // To get the error number we add a error range start number to
        // the status code returned (either status_t or connectionStatus_t)
        uint32_t status; // could be either status_t or connectionStatus_t
        uint32_t errnum;
        std::string msg;
};

} // namespace Drill

#endif
