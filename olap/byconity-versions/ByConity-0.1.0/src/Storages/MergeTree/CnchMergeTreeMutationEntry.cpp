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

#include <Storages/MergeTree/CnchMergeTreeMutationEntry.h>

#include <IO/Operators.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromString.h>

namespace DB
{

void CnchMergeTreeMutationEntry::writeText(WriteBuffer & out) const
{
    out << "format version: 1\n"
        << "txn_id: " << txn_id.toUInt64() << "\n"
        << "commit_ts: " << commit_time.toUInt64() << "\n"
        << "storage_commit_ts: " << columns_commit_time.toUInt64() << "\n";

    out << "commands: ";
    commands.writeText(out);
    out << "\n";
}

void CnchMergeTreeMutationEntry::readText(ReadBuffer & in)
{
    UInt64 txn_id_, commit_ts_, columns_commit_ts_;
    in >> "format version: 1\n" >> "txn_id: " >> txn_id_ >> "\n" >> "commit_ts: " >> commit_ts_ >> "\n" >> "storage_commit_ts: "
        >> columns_commit_ts_ >> "\n";

    txn_id = txn_id_;
    commit_time = commit_ts_;
    columns_commit_time = columns_commit_ts_;
    in >> "commands: ";
    commands.readText(in);
    in >> "\n";
}

String CnchMergeTreeMutationEntry::toString() const
{
    WriteBufferFromOwnString out;
    writeText(out);
    return out.str();
}

CnchMergeTreeMutationEntry CnchMergeTreeMutationEntry::parse(const String & str)
{
    CnchMergeTreeMutationEntry res;

    ReadBufferFromString in(str);
    res.readText(in);
    assertEOF(in);

    return res;
}

bool CnchMergeTreeMutationEntry::isReclusterMutation() const
{
    if (commands.size() == 1 && commands[0].type==MutationCommand::Type::RECLUSTER)
        return true;
    else
        return false;
}

}
