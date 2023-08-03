/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once
#include <Interpreters/StorageID.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <CloudServices/CnchServerClient.h>
#include <Interpreters/Context_fwd.h>

namespace DB::DaemonManager
{

class ITargetServerCalculator
{
public:
    virtual CnchServerClientPtr getTargetServer(const StorageID &, UInt64) const = 0;
    virtual ~ITargetServerCalculator() = default;
};

class TargetServerCalculator : public ITargetServerCalculator
{
public:
    TargetServerCalculator(Context & context, CnchBGThreadType type, Poco::Logger * log);
    CnchServerClientPtr getTargetServer(const StorageID &, UInt64) const override;
private:
    CnchServerClientPtr getTargetServerForCnchMergeTree(const StorageID &, UInt64) const;
    CnchServerClientPtr getTargetServerForCnchKafka(const StorageID &, UInt64) const;
    const CnchBGThreadType type;
    Context & context;
    Poco::Logger * log;
};

}
