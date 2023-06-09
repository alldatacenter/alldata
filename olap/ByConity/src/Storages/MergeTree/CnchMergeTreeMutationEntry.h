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

#include <Core/Types.h>
#include <IO/WriteHelpers.h>
#include <Storages/MutationCommands.h>
#include <Transaction/TxnTimestamp.h>
#include <Common/Exception.h>


namespace DB
{

class ReadBuffer;
class WriteBuffer;

struct CnchMergeTreeMutationEntry
{
    void writeText(WriteBuffer & out) const;
    void readText(ReadBuffer & in);

    String toString() const;
    static CnchMergeTreeMutationEntry parse(const String & str);
    bool isReclusterMutation() const;

    TxnTimestamp txn_id;
    TxnTimestamp commit_time;

    /*
     * for BUILD_BITMAP, we wouldn't update storage version in CnchMergeTree::alter.
     * so in this case, we use columns_commit_time to judge whether we should execute MutationCommands for parts.
     * */

    TxnTimestamp columns_commit_time;

    /// Mutation commands which will give to MUTATE_PART entries
    MutationCommands commands;
};

}
