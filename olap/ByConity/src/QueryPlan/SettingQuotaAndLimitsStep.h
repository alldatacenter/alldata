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

#include <Interpreters/Context_fwd.h>
#include <QueryPlan/ITransformingStep.h>
#include <Storages/TableLockHolder.h>
#include <DataStreams/StreamLocalLimits.h>

namespace DB
{

class IStorage;
using StoragePtr = std::shared_ptr<IStorage>;

struct StorageInMemoryMetadata;
using StorageMetadataPtr = std::shared_ptr<const StorageInMemoryMetadata>;

class EnabledQuota;

/// Add limits, quota, table_lock and other stuff to pipeline.
/// Doesn't change DataStream.
class SettingQuotaAndLimitsStep : public ITransformingStep
{
public:
    SettingQuotaAndLimitsStep(
        const DataStream & input_stream_,
        StoragePtr storage_,
        TableLockHolder table_lock_,
        StreamLocalLimits & limits_,
        SizeLimits & leaf_limits_,
        std::shared_ptr<const EnabledQuota> quota_,
        ContextPtr context_);

    String getName() const override { return "SettingQuotaAndLimits"; }

    Type getType() const override { return Type::SettingQuotaAndLimits; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    void serialize(WriteBuffer & buf) const override;

    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr context);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    ContextPtr context;
    StoragePtr storage;
    TableLockHolder table_lock;
    StreamLocalLimits limits;
    SizeLimits leaf_limits;
    std::shared_ptr<const EnabledQuota> quota;
};

}
