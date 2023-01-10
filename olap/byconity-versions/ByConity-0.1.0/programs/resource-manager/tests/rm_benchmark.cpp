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

#include <atomic>
#include <iostream>
#include <signal.h>
#include <thread>
#include <bytejournal/sdk/client.h>
#include <gflags/gflags.h>

#include <bthread/bthread.h>
#include <Common/Exception.h>
#include <Interpreters/Context.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <WAL/ByteJournalCommon.h>
#include <Poco/Util/LayeredConfiguration.h>
#include "Common/Config/ConfigProcessor.h"

class Worker;

// Concurrent clients' config
DEFINE_uint32(clients, 1, "clients");
DEFINE_uint64(clients_step_sleep_seconds, 1, "sleep seconds in each step group");

// Client operation count
DEFINE_uint64(operation, 9999, "operation execute count");
DEFINE_string(vw_name, "vw_default", "Virtual warehouse name of workers");
DEFINE_string(worker_group_id, "wg_default", "Virtual warehouse name of workers");

// Brpc config
DEFINE_string(election_ns, "rm_namespace_default", "election namespace");
DEFINE_string(election_point, "rm_point_default", "election point");
DEFINE_uint64(connection_timeout, 300, "read timeout");

// Reporter config
DEFINE_int64(report_intervals, 10, "report interval seconds");
DEFINE_bool(enable_output_group, true, "wrap output with Begin End");

// Executable config
DEFINE_string(config_path, "config.xml", "Configuration file path");

#undef ASSERT
#define ASSERT(v) \
    if (!(v)) \
    { \
        std::cerr << __PRETTY_FUNCTION__ << ":" << __LINE__ << " assert 0" << std::endl; \
        abort(); \
    }

namespace
{
inline uint64_t NowUs()
{
    auto duration = std::chrono::steady_clock::now().time_since_epoch();

    return std::chrono::duration_cast<std::chrono::microseconds>(duration).count();
}
}

std::atomic<bool> quit{false};
using WorkerPtr = std::shared_ptr<Worker>;
std::vector<WorkerPtr> clients{0};

class Histogram
{
public:
    Histogram(std::string name)
        : name_(std::move(name)), report_intervals_(FLAGS_report_intervals), previous_count_(0), latency_recorder_(report_intervals_)
    {
    }
    void Measure(uint64_t latency_us) { latency_recorder_ << latency_us; }

    std::string Summary()
    {
        std::stringstream stream;
        auto count = latency_recorder_.count();

        stream << "[" << name_ << "] "
               << "Clients: " << clients.size() << ", "
               << "Takes(s): " << FLAGS_report_intervals << ", "
               << "Count: " << count - previous_count_ << ", "
               << "OPS: " << latency_recorder_.qps(report_intervals_) << ", "
               << "Avg(us): " << latency_recorder_.latency_percentile(0.5) << ", "
               << "P95(us): " << latency_recorder_.latency_percentile(0.95) << ", "
               << "P99(us): " << latency_recorder_.latency_percentile(0.99) << ", "
               << "Max(us): " << latency_recorder_.max_latency();

        previous_count_ = count;

        return stream.str();
    }

private:
    std::string name_;
    int64_t report_intervals_; // seconds
    int64_t previous_count_;
    bvar::LatencyRecorder latency_recorder_;
};
class ReportUtil

{
public:
    ReportUtil(std::string name) : exited_(false), histogram_(std::move(name))
    {
        reporter_ = std::thread([this]() {
            while (!exited_)
            {
                bthread_usleep(FLAGS_report_intervals * 1000 * 1000);
                report();
            }
        });
    }

    ~ReportUtil()
    {
        exited_ = true;
        reporter_.join();

        report();
    }

    void Measure(uint64_t latency_us) { histogram_.Measure(latency_us); }

private:
    void report()
    {
        std::string summary = histogram_.Summary();
        if (summary.empty())
        {
            return;
        }

        if (FLAGS_enable_output_group)
        {
            std::cout << "------- Begin report -------" << std::endl;
        }
        std::cout << summary << std::endl;
        if (FLAGS_enable_output_group)
        {
            std::cout << "------- End report -------" << std::endl;
        }
    }

    std::atomic<bool> exited_;
    std::thread reporter_;
    Histogram histogram_;
};

class RMClientWrapper
{
public:
    RMClientWrapper(DB::Context & context, const std::string worker_id_)
        : client_(context, FLAGS_election_ns, FLAGS_election_point)
        , worker_id(worker_id_)
    {}

    void Start()
    {
        DB::ResourceManagement::WorkerNodeResourceData data;
        data.vw_name = FLAGS_vw_name;
        data.worker_group_id = FLAGS_worker_group_id;
        data.id = worker_id;
        client_.registerWorker(data);
    }

    void Run()
    {
        DB::ResourceManagement::WorkerNodeResourceData data;
        client_.reportResourceUsage(data);
    }

    void Stop()
    {
        client_.removeWorker(worker_id, FLAGS_vw_name, FLAGS_worker_group_id);
    }

private:
    DB::ResourceManagement::ResourceManagerClient client_;
    std::string worker_id;
};


class Worker
{
public:
    Worker( uint64_t count, std::shared_ptr<ReportUtil> reporter, DB::Context & global_context, const std::string worker_id)
        : count_(count), reporter_(reporter), client_(global_context, worker_id)
    {}

    void Start()
    {
        if (quit.load(std::memory_order_relaxed))
        {
            return;
        }

        uint64_t start_us = NowUs();
        client_.Start();
        uint64_t end_us = NowUs();
        reporter_->Measure(end_us - start_us);
    }

    void Run()
    {

        for (uint64_t i = 0; i < count_; ++i)
        {
            if (quit.load(std::memory_order_relaxed))
            {
                break;
            }
            bthread_usleep(FLAGS_clients_step_sleep_seconds);

            uint64_t start_us = NowUs();
            client_.Run();
            uint64_t end_us = NowUs();
            reporter_->Measure(end_us - start_us);
        }
    }

    void Stop()
    {
        if (quit.load(std::memory_order_relaxed))
        {
            return;
        }

        uint64_t start_us = NowUs();
        client_.Stop();
        uint64_t end_us = NowUs();
        reporter_->Measure(end_us - start_us);
    }

private:
    uint64_t count_;
    std::shared_ptr<ReportUtil> reporter_;
    RMClientWrapper client_;
};

void handle_signal(int signum)
{
    /* in case we registered this handler for multiple signals */
    if (signum == SIGINT)
    {
        quit = true;
    }
}


int main(int argc, char ** argv)
{
    signal(SIGINT, handle_signal);
    gflags::ParseCommandLineFlags(&argc, &argv, true);

    clients.reserve(FLAGS_clients);

    bthread_list_t thread_list;
    bthread_list_init(&thread_list, FLAGS_clients, FLAGS_clients);

    std::shared_ptr<ReportUtil> reporter = std::make_shared<ReportUtil>("RM");
    auto global_context = std::make_unique<DB::Context>(DB::Context::createGlobal());
    DB::ConfigProcessor config_processor(FLAGS_config_path, false, true);
    config_processor.setConfigPath(Poco::Path(FLAGS_config_path).makeParent().toString());
    auto loaded_config = config_processor.loadConfig(/* allow_zk_includes = */ true);
    global_context->setGlobalContext(*global_context);
    global_context->setConfig(loaded_config.configuration);
    global_context->setApplicationType(DB::Context::ApplicationType::SERVER);
    global_context->initByteJournalClient();

    for (size_t i = 0; i < FLAGS_clients; ++i)
    {
        auto creator = [reporter, &i, &global_context](bthread_list_t * thread_list)
        {
            bthread_t thread_id;
            auto worker_id = "worker_" + std::to_string(i);
            auto worker = std::make_shared<Worker>(FLAGS_operation, reporter, *global_context, worker_id);
            clients.emplace(clients.begin() + i, worker);
            auto executor = [](void * args) -> void * {
                auto worker = reinterpret_cast<Worker *>(args);
                try
                {
                    worker->Start();
                    worker->Run();
                    worker->Stop();
                } catch (const DB::Exception & e)
                {
                    std::cout << e.message() << std::endl;
                    std::cout << e.getStackTrace().toString() << std::endl;
                }
                return nullptr;
            };
            int res = bthread_start_background(&thread_id, nullptr, executor, worker.get());
            ASSERT(res == 0);
            res = bthread_list_add(thread_list, thread_id);
            ASSERT(res == 0);

        };

        creator(&thread_list);
    }

    std::cout << "\nrun with clients " << FLAGS_clients << "\n" << std::endl;

    bthread_list_join(&thread_list);
    bthread_list_destroy(&thread_list);

    return 0;
}
