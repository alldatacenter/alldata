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
#include "drill/drillError.hpp"
#include "errmsgs.hpp"
#include "logger.hpp"
#include "UserBitShared.pb.h"

namespace exec{
    namespace shared{
        class DrillPBError;
    };
};

namespace Drill{

DrillClientError* DrillClientError::getErrorObject(const exec::shared::DrillPBError& e){
    std::string s=Drill::getMessage(ERR_QRY_FAILURE, e.message().c_str());
    DrillClientError* err=NULL;
    err=new DrillClientError(QRY_FAILURE, QRY_ERROR_START+QRY_FAILURE, s);
    return err;
}


} // namespace Drill
