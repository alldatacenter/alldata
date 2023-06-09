/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Poco/ConsoleChannel.h>
#include <Poco/Logger.h>
#include <Coordination/KeeperStateMachine.h>
#include <Common/ZooKeeper/ZooKeeperCommon.h>
#include <Common/ZooKeeper/ZooKeeperIO.h>
#include <Common/Exception.h>
#include <libnuraft/nuraft.hxx>
#include <Coordination/KeeperLogStore.h>
#include <Coordination/Changelog.h>
#include <common/logger_useful.h>

#include <limits>

using namespace Coordination;
using namespace DB;

void dumpMachine(std::shared_ptr<KeeperStateMachine> machine)
{
    auto & storage = machine->getStorage();
    std::queue<std::string> keys;
    keys.push("/");

    while (!keys.empty())
    {
        auto key = keys.front();
        keys.pop();
        std::cout << key << "\n";
        auto value = storage.container.getValue(key);
        std::cout << "\tStat: {version: " << value.stat.version <<
            ", mtime: " << value.stat.mtime <<
            ", emphemeralOwner: " << value.stat.ephemeralOwner <<
            ", czxid: " << value.stat.czxid <<
            ", mzxid: " << value.stat.mzxid <<
            ", numChildren: " << value.stat.numChildren <<
            ", dataLength: " << value.stat.dataLength <<
            "}" << std::endl;
        std::cout << "\tData: " << storage.container.getValue(key).getData() << std::endl;

        for (const auto & child : value.getChildren())
        {
            if (key == "/")
                keys.push(key + child.toString());
            else
                keys.push(key + "/" + child.toString());
        }
    }
    std::cout << std::flush;
}

int main(int argc, char *argv[])
{
    if (argc != 3)
    {
        std::cerr << "usage: " << argv[0] << " snapshotpath logpath" << std::endl;
        return 3;
    }
    else
    {
        Poco::AutoPtr<Poco::ConsoleChannel> channel(new Poco::ConsoleChannel(std::cerr));
        Poco::Logger::root().setChannel(channel);
        Poco::Logger::root().setLevel("trace");
    }
    auto * logger = &Poco::Logger::get("keeper-dumper");
    ResponsesQueue queue{std::numeric_limits<size_t>::max()};
    SnapshotsQueue snapshots_queue{1};
    CoordinationSettingsPtr settings = std::make_shared<CoordinationSettings>();
    KeeperContextPtr keeper_context = std::make_shared<DB::KeeperContext>();
    auto state_machine = std::make_shared<KeeperStateMachine>(queue, snapshots_queue, argv[1], settings, keeper_context);
    state_machine->init();
    size_t last_commited_index = state_machine->last_commit_index();

    LOG_INFO(logger, "Last committed index: {}", last_commited_index);

    DB::KeeperLogStore changelog(argv[2], 10000000, true, settings->compress_logs);
    changelog.init(last_commited_index, 10000000000UL); /// collect all logs
    if (changelog.size() == 0)
        LOG_INFO(logger, "Changelog empty");
    else
        LOG_INFO(logger, "Last changelog entry {}", changelog.next_slot() - 1);

    for (size_t i = last_commited_index + 1; i < changelog.next_slot(); ++i)
    {
        if (changelog.entry_at(i)->get_val_type() == nuraft::log_val_type::app_log)
            state_machine->commit(i, changelog.entry_at(i)->get_buf());
    }

    dumpMachine(state_machine);

    return 0;
}
