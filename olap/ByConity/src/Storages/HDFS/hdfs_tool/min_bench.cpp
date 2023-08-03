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

#include <algorithm>
#include <atomic>
#include <numeric>
#include <sstream>
#include <random>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/HDFS/HDFSFileSystem.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <IO/ReadBufferFromString.h>
#include <IO/copyData.h>
#include <boost/program_options.hpp>
#include <hdfs/hdfs.h>
#include <Poco/AutoPtr.h>
#include <Poco/ConsoleChannel.h>
#include <Poco/FormattingChannel.h>
#include <Poco/Logger.h>
#include <Poco/Path.h>
#include <Poco/PatternFormatter.h>
#include <Common/Exception.h>
#include <Common/ThreadPool.h>
#include <Common/ThreadPool.h>
#include <Common/Stopwatch.h>
#include <Common/filesystemHelpers.h>
#include <common/scope_guard.h>
#include <ServiceDiscovery/registerServiceDiscovery.h>

using namespace DB;


String randomString(size_t length)
{
    static const char alphanum[] = "0123456789"
                                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                   "abcdefghijklmnopqrstuvwxyz";

    static thread_local std::mt19937 generator;
    std::uniform_int_distribution<int> distribution(0, sizeof(alphanum) / sizeof(char));

    //    srand((unsigned) time(NULL) * getpid());

    String str(length, '\0');
    for (size_t i = 0; i < length; i++)
    {
        str[i] = alphanum[distribution(generator)];
    }
    return str;
}

class ThreadStatistics
{
public:
    virtual ~ThreadStatistics() { }

    virtual String str() { return ""; }
};

class BucketedTimeStatistics : public ThreadStatistics
{
public:
    BucketedTimeStatistics(int bucket_interval_ , int bucket_size_ )
        : bucket_interval(bucket_interval_)
        , bucket_size(bucket_size_)
        , count(0)
        , total_time_ms(0)
        , min_time_ms(std::numeric_limits<int>::max())
        , max_time_ms(0)
        , buckets(bucket_size + 1, 0)
    {
    }

    virtual ~BucketedTimeStatistics() override { }

    virtual String str() override
    {
        std::stringstream ss;
        ss << "TotalCount: " << count << ", TotalTime: " << total_time_ms << ", MinTime: " << min_time_ms << ", MaxTime: " << max_time_ms
           << ", Avg: " << (total_time_ms / count) << "\n";
        ss << "distribution"
           << "\n";
        for (size_t i = 0; i < buckets.size(); i++)
        {
            ss << '\t' << i * bucket_interval;
        }
        ss << '\n';
        for (size_t i = 0; i < buckets.size(); i++)
        {
            ss << '\t' << buckets[i];
        }
        ss << '\n';

        return ss.str();
    }

    void update(int time_ms)
    {
        count++;
        total_time_ms += time_ms;

        min_time_ms = std::min(min_time_ms, time_ms);
        max_time_ms = std::max(max_time_ms, time_ms);

        int ind = time_ms / bucket_interval;
        if (ind >= int(buckets.size()))
        {
            buckets[buckets.size() - 1] += 1;
        }
        else
        {
            buckets[ind] += 1;
        }
    }

    static std::shared_ptr<BucketedTimeStatistics> nullStats(int bucket_interval, int bucket_size)
    {
        auto ret = std::make_shared<BucketedTimeStatistics>(bucket_interval, bucket_size);
        ret->count = 0;
        ret->total_time_ms = 0;
        ret->min_time_ms = std::numeric_limits<int>::max();
        ret->max_time_ms = 0;
        return ret;
    }
    static void combine(std::shared_ptr<BucketedTimeStatistics> & ret, std::vector<std::shared_ptr<BucketedTimeStatistics>> & thread_stats)
    {
        for (auto & s : thread_stats)
        {
            ret->count = ret->count + s->count;
            ret->total_time_ms = ret->total_time_ms + s->total_time_ms;
            ret->min_time_ms = std::min(ret->min_time_ms, s->min_time_ms);
            ret->max_time_ms = std::max(ret->max_time_ms, s->max_time_ms);
            for (size_t i = 0; i < ret->buckets.size(); ++i)
            {
                ret->buckets[i] = ret->buckets[i] + s->buckets[i];
            }
        }
    }

public:
    int bucket_interval;
    int bucket_size;
private:
    int count;
    int total_time_ms;
    int min_time_ms;
    int max_time_ms;
    std::vector<int> buckets;
};
class BenchRunner {
public:
    BenchRunner(int run_count, int thread_num):
        run_count_(run_count), thread_num_(thread_num) {}

    virtual ~BenchRunner() {}

    void run() {
        globalSetUp();
        SCOPE_EXIT({globalTearDown();});

        for (int rc = 0; rc < run_count_; rc++) {
            caseSetUp(rc);
            SCOPE_EXIT({caseTearDown(rc);});

            ThreadPool pool(thread_num_);

            std::vector<std::shared_ptr<ThreadStatistics>> stats(thread_num_, nullptr);

            for (int t_id = 0; t_id < thread_num_; t_id++) {
                std::shared_ptr<ThreadStatistics>& thread_stats = stats[t_id];
                pool.trySchedule([this, t_id, &thread_stats]() {
                    thread_stats = perf(t_id);
                });
            }

            pool.wait();

            std::cout << "--- Run " << rc << std::endl;
            for (int t_id = 0; t_id < thread_num_; t_id++) {
                std::cout << "ThreadStats: [ " << t_id << " ]\n";
                if (stats[t_id] == nullptr) {
                    std::cout << "    No statistics for thread\n"<< t_id << std::endl;
                } else {
                    std::cout << "    " << stats[t_id]->str() << "\n" << std::endl;
                }
            }

            if(stats.size() >0) {
                if(stats[0] != nullptr) {
                    auto a = stats[0];
                    std::shared_ptr<BucketedTimeStatistics> ptr = std::dynamic_pointer_cast<BucketedTimeStatistics>(a);
                    if(a) {
                        auto ret = BucketedTimeStatistics::nullStats(ptr->bucket_interval, ptr->bucket_size);
                        std::vector<std::shared_ptr<BucketedTimeStatistics>> vec;
                        for(auto & p : stats)
                        {
                            vec.push_back(std::dynamic_pointer_cast<BucketedTimeStatistics>(p));
                        }
                        BucketedTimeStatistics::combine(ret, vec );
                        std::cout << "ThreadStats: \n" ;
                        std::cout << ret->str() << "\n" << std::endl;
                    }
                }
            }





            std::cout << std::endl;
        }
    }

protected:
    virtual void globalSetUp() {}
    virtual void globalTearDown() {}
    virtual void caseSetUp(int ) {}
    virtual void caseTearDown(int ) {}

    virtual std::shared_ptr<ThreadStatistics> perf(int thread_id) = 0;

    int run_count_;
    int thread_num_;
};

class ThreadTimeStatistics : public ThreadStatistics
{
public:
    ThreadTimeStatistics() : count(0), total_time_ms(0), min_time_ms(std::numeric_limits<int>::max()), max_time_ms(0) { }

    virtual ~ThreadTimeStatistics() override { }

    virtual String str() override
    {
        std::stringstream ss;
        ss << "TotalCount: " << count << ", TotalTime: " << total_time_ms << ", MinTime: " << min_time_ms << ", MaxTime: " << max_time_ms
           << ", Avg: " << (total_time_ms / count);
        return ss.str();
    }

    void update(int time_ms)
    {
        count++;
        total_time_ms += time_ms;

        min_time_ms = std::min(min_time_ms, time_ms);
        max_time_ms = std::max(max_time_ms, time_ms);
    }

    int count;
    int total_time_ms;

    int min_time_ms;
    int max_time_ms;
};





class CreateFileRunner : public BenchRunner
{
public:
    CreateFileRunner(int file_num, int thread_num, String file_path, int file_size, HDFSConnectionParams hdfs_params)
        : BenchRunner(1, thread_num)
        , file_num_(file_num)
        , file_path_(file_path)
        , file_size_(file_size)
        , hdfs_params_(hdfs_params)
        , logger(Poco::Logger::get("CreateFileRunner"))
    {
    }

protected:
    static std::atomic<int64_t> file_order;

    virtual void globalSetUp() override
    {
        auto& hdfs_fs = getDefaultHdfsFileSystem();
        if (hdfs_fs->exists(file_path_))
        {
            LOG_INFO(&logger, "{} not exist",file_path_);
        }
        else
        {
            hdfs_fs->createDirectory(file_path_);
        }
    }

    virtual void globalTearDown() override { LOG_INFO(&logger, "created {} in {}" , file_num_, file_path_); }


    virtual std::shared_ptr<ThreadStatistics> perf([[maybe_unused]] int thread_id) override
    {
        ThreadPool pool(thread_num_);
        std::shared_ptr<BucketedTimeStatistics> stats = std::make_shared<BucketedTimeStatistics>(10 ,30);
        while (1)
        {
            std::stringstream ss;
            int64_t order = file_order.fetch_add(1, std::memory_order::seq_cst);
            if (order > file_num_)
            {
                break;
            }

            ss << file_path_ << "/" << order;
            String file_name = ss.str();
            LOG_TRACE(&logger, "write file {}", file_name);
            String content = randomString(file_size_);

            Stopwatch watch;
            SCOPE_EXIT({ stats->update(watch.elapsedMilliseconds()); });
            DB::ReadBufferFromString reader(content);
            DB::WriteBufferFromHDFS wb(file_name, hdfs_params_);
            DB::copyData(reader, wb);
            wb.next();
        }
        return stats;
    }


private:
    int file_num_;
    String file_path_;
    int file_size_;
    HDFSConnectionParams hdfs_params_;
    Poco::Logger & logger;
};


class ReadFileRunner : public BenchRunner
{
public:
    ReadFileRunner(
        int read_num,
        int file_num,
        int thread_num,
        int file_size,
        String file_path,
        HDFSConnectionParams hdfs_params,
        int bucket_interval,
        int bucket_size)
        : BenchRunner(1, thread_num)
        , read_num_(read_num)
        , file_num_(file_num)
        , file_path_(file_path)
        , file_size_(file_size)
        , hdfs_params_(hdfs_params)
        , bucket_interval_(bucket_interval)
        , bucket_size_(bucket_size)
        , logger(Poco::Logger::get("ReadFileRunner"))
    {
        LOG_INFO(&Poco::Logger::root(), "passed bucket_size {}", bucket_size_);
    }

protected:
    static std::atomic<int64_t> file_order;

    virtual void globalSetUp() override { }

    virtual void globalTearDown() override
    {
        //        LOG_INFO(&logger, "created " << file_num_ << " in " << file_path_);
    }


    virtual std::shared_ptr<ThreadStatistics> perf([[maybe_unused]] int thread_id) override
    {
        ThreadPool pool(thread_num_);
        std::shared_ptr<BucketedTimeStatistics> stats = std::make_shared<BucketedTimeStatistics>(bucket_interval_, bucket_size_);
        static thread_local std::mt19937 generator;
        std::uniform_int_distribution<int> distribution(0, file_num_);
        while (1)
        {
            std::stringstream ss;
            int64_t order = file_order.fetch_add(1, std::memory_order::seq_cst);
            if (order > read_num_)
            {
                break;
            }
            if (order % 1000 == 0)
            {
                LOG_DEBUG(&logger, "read {} timdes", order);
            }

            ss << file_path_ << "/" << distribution(generator);
            String file_name = ss.str();
            LOG_TRACE(&logger, "read file {}", file_name);


            std::uniform_int_distribution<int> off_dist(0, std::max(1, file_size_ - 1024 * 1024));

            DB::ReadBufferFromByteHDFS rb(file_name, true, hdfs_params_);
            rb.seek(off_dist(generator));
            Stopwatch watch;
            {
                SCOPE_EXIT({ stats->update(watch.elapsedMilliseconds()); });
                rb.eof();
            }
        }
        return stats;
    }


private:
    int read_num_;
    int file_num_;
    String file_path_;
    int file_size_;
    HDFSConnectionParams hdfs_params_;
    int bucket_interval_;
    int bucket_size_;
    Poco::Logger & logger;
};


std::atomic<int64_t> CreateFileRunner::file_order{0};
std::atomic<int64_t> ReadFileRunner::file_order{0};


class HDFSBenchRunner : public BenchRunner
{
public:
    HDFSBenchRunner(
        int run_count,
        int thread_num,
        const String & nnproxy,
        const String & path,
        int file_size,
        int step_size,
        bool verify,
        bool using_existing_file,
        bool auto_clean)
        : BenchRunner(run_count, thread_num)
        , nnproxy_(nnproxy)
        , bench_file_path_(using_existing_file ? path : joinPaths({path, randomString(16)}, false))
        , file_size_(file_size)
        , step_size_(step_size)
        , verify_(verify)
        , using_existing_file_(using_existing_file)
        , auto_clean_(auto_clean)
    {
        if (using_existing_file_ && verify_)
        {
            throw Exception("Can't verify read while using existing file", ErrorCodes::BAD_ARGUMENTS);
        }
        if (using_existing_file_ && auto_clean_)
        {
            throw Exception("Can't auto clean existing file", ErrorCodes::BAD_ARGUMENTS);
        }
    }

protected:
    virtual void globalSetUp() override
    {
        std::cout << "Using file " << bench_file_path_ << std::endl;

        if (!using_existing_file_)
        {
            content_ = randomString(file_size_);
            HDFSConnectionParams params(HDFSConnectionParams::CONN_NNPROXY, "clickhouse", nnproxy_);
            ReadBufferFromString reader(content_);
            WriteBufferFromHDFS writer(bench_file_path_, params);
            copyData(reader, writer);

            if (!verify_)
            {
                content_ = "";
            }
        }
    }

    virtual void globalTearDown() override
    {
        if (!using_existing_file_ && auto_clean_)
        {
            auto& hdfs_fs = getDefaultHdfsFileSystem();
            hdfs_fs->remove(bench_file_path_);
        }
    }

    virtual std::shared_ptr<ThreadStatistics> perf(int thread_id) override
    {
        (void)thread_id;

        std::shared_ptr<ThreadTimeStatistics> stats = std::make_unique<ThreadTimeStatistics>();
        HDFSConnectionParams params =  HDFSConnectionParams::parseFromMisusedNNProxyStr(nnproxy_);
        ReadBufferFromByteHDFS reader(bench_file_path_, true, params, step_size_);

        size_t buffer_offset = 0;

        bool eof = false;
        do
        {
            Stopwatch watch;
            {
                SCOPE_EXIT({ stats->update(watch.elapsedMilliseconds()); });
                eof = reader.eof();
            }

            size_t buffer_size = reader.buffer().size();

            if (verify_ && memcmp(content_.data() + buffer_offset, reader.buffer().begin(), buffer_size))
            {
                throw Exception("Content verification failed at " + std::to_string(buffer_offset), ErrorCodes::BAD_ARGUMENTS);
            }

            buffer_offset += buffer_size;
            reader.position() += buffer_size;
        } while(!eof);

        if (verify_ && buffer_offset != content_.size()) {
            throw Exception("Readed length " + std::to_string(buffer_offset) + " not matched with content length "
                            + std::to_string(content_.size()), ErrorCodes::BAD_ARGUMENTS);
        }

        return std::dynamic_pointer_cast<ThreadStatistics>(stats);
    }


private:
    String content_;

    String nnproxy_;
    String bench_file_path_;

    int file_size_;
    int step_size_;

    bool verify_;
    bool using_existing_file_;
    bool auto_clean_;
};

namespace po = boost::program_options;

int main(int argc, char** argv) {
    po::options_description desc("");
    desc.add_options()
        ("hdfs", po::value<bool>()->default_value(true))
        ("using_existing_file", po::value<bool>()->default_value(true))
        ("enable_verify", po::value<bool>()->default_value(true))
        ("auto_clean", po::value<bool>()->default_value(true))
        ("enable_logging", po::value<bool>()->default_value(true))
        ("type",po::value<String>()->default_value("read"),"read/prepare_files")
        ("total_size", po::value<int>()->default_value(0), "")
        ("step_size", po::value<int>()->default_value(1048576), "read buffer size, 1048576")
        ("run_count", po::value<int>()->default_value(1), "")
        ("thread_num", po::value<int>()->default_value(20), "")
        ("file_num", po::value<int>()->default_value(1000), "file numbers")
        ("nnproxy", po::value<String>()->default_value("nnproxy"), "")
        ("path", po::value<String>()->default_value("/user/test/test_files"), "")
        ("read_num", po::value<int>()->default_value(10000), "read times")
        ("bucket_interval", po::value<int>()->default_value(30), "bucket interval")
        ("bucket_size", po::value<int>()->default_value(10), "bucket size")
        ("logging_level", po::value<String>()->default_value("debug"), "logging level")
        ;
    po::variables_map options;
    po::store(po::parse_command_line(argc, argv, desc), options);

    if (options.count("enable_logging"))
    {
        Poco::AutoPtr<Poco::PatternFormatter> formatter(new Poco::PatternFormatter("%Y.%m.%d %H:%M:%S.%F <%p> %s: %t"));
        Poco::AutoPtr<Poco::ConsoleChannel> console_chanel(new Poco::ConsoleChannel);
        Poco::AutoPtr<Poco::FormattingChannel> channel(new Poco::FormattingChannel(formatter, console_chanel));
        Poco::Logger::root().setLevel(options["logging_level"].as<String>());
        Poco::Logger::root().setChannel(channel);
    }

    bool enable_verify = options.count("enable_verify");
    bool using_existing_file = options.count("using_existing_file");
    bool auto_clean = options.count("auto_clean");
    int step_size = options["step_size"].as<int>();
    int run_count = options["run_count"].as<int>();
    int thread_num = options["thread_num"].as<int>();
    int total_size = using_existing_file ? 0 : options["total_size"].as<int>();

    DB::registerServiceDiscovery();
    if (options["hdfs"].as<bool>())
    {
        String nnproxy = options["nnproxy"].as<String>();
        String path = options["path"].as<String>();
        HDFSConnectionParams params(HDFSConnectionParams::CONN_NNPROXY, "clickhouse", nnproxy);
        registerDefaultHdfsFileSystem(params, 100000, 100, 1);

        int file_num = options["file_num"].as<int>();
        int read_num = options["read_num"].as<int>();
        int bucket_interval = options["bucket_interval"].as<int>();
        int bucket_size = options["bucket_size"].as<int>();
        String run_type = options["type"].as<String>();

        if (run_type == "read")
        {
            HDFSBenchRunner runner(
                run_count, thread_num, nnproxy, path, total_size, step_size, enable_verify, using_existing_file, auto_clean);

            runner.run();
        }
        else if (run_type == "prepare_files")
        {
            CreateFileRunner runner(file_num, thread_num, path, step_size, params);
            runner.run();
        }
        else if (run_type == "pread")
        {
            ReadFileRunner runner(read_num, file_num, thread_num, step_size, path, params, bucket_interval, bucket_size);
            runner.run();
        }
    }

    return 0;
}
