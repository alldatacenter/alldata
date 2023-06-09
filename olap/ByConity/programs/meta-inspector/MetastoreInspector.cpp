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


#include <Poco/Util/Application.h>
//#include <Catalog/MetastoreByteKVImpl.h>
#include <Catalog/MetastoreFDBImpl.h>
#include <Catalog/StringHelper.h>
#include <Catalog/CatalogConfig.h>
#include <Protos/data_models.pb.h>
#include <common/LineReader.h>
#include <brpc/server.h>
#include <gflags/gflags.h>
#include <iostream>

namespace brpc
{
namespace policy
{
    DECLARE_string(consul_agent_addr);
}
}

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int METASTORE_EXCEPTION;
}

using MetastorePtr = std::shared_ptr<Catalog::IMetaStore>;

enum class MetaCommandType
{
    HELP,
    LIST,
    GET,
    DELETE,
    COUNT,
    CLEAR
};

class MetaCommand
{
public:
    MetaCommand() = delete;
    MetaCommand(const MetaCommandType & type_, const std::string & key_)
        :type(type_), key(key_) {}

    static MetaCommand parse(const std::string & cmd_text)
    {
        std::vector<std::string> tokens;
        const char * pos = cmd_text.data();
        const char * end = cmd_text.data() + cmd_text.size();
        const char * token_begin = pos;
        while (pos < end)
        {
            if (*pos == ' ')
            {
                if (pos == token_begin)
                {
                    pos++;
                    token_begin++;
                }
                else
                {
                    tokens.emplace_back(std::string(token_begin, pos));
                    pos++;
                    token_begin = pos;
                }
            }
            else
                pos++;
        }
        if (pos>token_begin)
            tokens.emplace_back(std::string(token_begin, pos));

        if (tokens.size()==1 && tokens[0]=="help")
            return MetaCommand(MetaCommandType::HELP, "");

        if (tokens.size()==2)
        {
            if (tokens[0] == "get")
                return MetaCommand(MetaCommandType::GET, tokens[1]);
            else if (tokens[0] == "list")
                return MetaCommand(MetaCommandType::LIST, tokens[1]);
            else if (tokens[0] == "delete")
                return MetaCommand(MetaCommandType::DELETE, tokens[1]);
            else if (tokens[0] == "count")
                return MetaCommand(MetaCommandType::COUNT, tokens[1]);
            else if (tokens[0] == "clear")
                return MetaCommand(MetaCommandType::CLEAR, tokens[1]);
            else
                throw Exception(ErrorCodes::BAD_ARGUMENTS, "Unsupported metadata command {}", tokens[0]);
        }
        else
        {
            throw Exception( ErrorCodes::BAD_ARGUMENTS, "Invalid comand.");
        }
    }

    MetaCommandType type;
    std::string key;
};

template <typename T>
std::string formatDataModel(const std::string & part_data)
{
    T data_model;
    data_model.ParseFromString(part_data);
    return data_model.DebugString();
};

void dumpMetadata(const std::string & key, const std::string & metadata)
{
    if (key.starts_with("TB_"))
        std::cout << formatDataModel<DB::Protos::DataModelTable>(metadata) << std::endl;
    else if (key.starts_with("PT_"))
        std::cout << formatDataModel<DB::Protos::DataModelPart>(metadata) << std::endl;
    else if (key.starts_with("TR_"))
        std::cout << formatDataModel<DB::Protos::DataModelTransactionRecord>(metadata) << std::endl;
    else
        std::cout << metadata << std::endl;
};

class MetastoreInspector : public Poco::Util::Application
{
public:
    MetastoreInspector() = default;

protected:
    void defineOptions(Poco::Util::OptionSet & options) override
    {
        options.addOption(
        Poco::Util::Option("help", "h", "show help message")
            .required(false)
            .repeatable(false)
            .binding("help"));
        options.addOption(
            Poco::Util::Option("config-file", "C", "config file path")
                .required(true)
                .repeatable(false)
                .argument("<file>", true)
                .binding("config-file"));
        options.addOption(
            Poco::Util::Option("namespace", "n", "namespace of metadata")
                .required(false)
                .repeatable(false)
                .argument("namespace", true)
                .binding("namespace"));
        options.addOption(
            Poco::Util::Option("exec", "E", "command to be executed")
                .required(false)
                .repeatable(false)
                .argument("exec", true)
                .binding("exec"));
    }

    void initialize(Application& self) override
    {
        std::string conf_path = config().getString("config-file");
        if (conf_path.empty())
            throw Exception("config-file can not be empty.", ErrorCodes::BAD_ARGUMENTS);
        std::cout << "laod config from file : " << conf_path << std::endl;
        if (config().has("namespace"))
            name_space = config().getString("namespace") + "_";
        loadConfiguration(conf_path);
        initializeMetastore();

        Application::initialize(self);
    }

    int main(const std::vector<std::string> &) override
    {
        if (config().has("help"))
        {
            std::cout << "Usage: \n";
            std::cout << "./clickhouse meta-inspector --config-file <file> --namespace name_space --exec cmd" << std::endl;
            return Application::EXIT_OK;
        }

        bool is_interactive = true;

        if (config().has("exec"))
            is_interactive = false;

        if (!is_interactive)
        {
            std::string input_cmd = config().getString("exec");
            execute(input_cmd);
        }
        else
        {
            LineReader::Patterns extenders = {"\\"};
            LineReader::Patterns delimiters = {";"};
            LineReader lr("place_holder", false, extenders, delimiters);
            do
            {
                auto input_cmd = lr.readLine(":> ", "");
                if (exit_strings.count(input_cmd))
                    break;
                execute(input_cmd);
            } while(true);
        }

        return Application::EXIT_OK;
    }

private:
    using StringSet = std::unordered_set<std::string>;
    StringSet exit_strings {"exit", "quit", "q"};

    void printHelpMessage()
    {
        std::cout << "Usage: command search_key\n";
        std::cout << "Optional command are: \n";
        std::cout << "\tget\n";
        std::cout << "\tlist\n";
        std::cout << "\tcount\n";
        std::cout << "\tdelete\n";
        std::cout << "\tclear\n";
        std::cout << std::endl;
    }

    void initializeMetastore()
    {
        Catalog::CatalogConfig catalog_conf(config());
        const char * consul_http_host = getenv("CONSUL_HTTP_HOST");
        const char * consul_http_port = getenv("CONSUL_HTTP_PORT");
        if (consul_http_host != nullptr && consul_http_port != nullptr)
            brpc::policy::FLAGS_consul_agent_addr = "http://" + std::string(consul_http_host) + ":" + std::string(consul_http_port);

        if (catalog_conf.type == Catalog::StoreType::FDB)
        {
            metastore_ptr = std::make_shared<Catalog::MetastoreFDBImpl>(catalog_conf.fdb_conf.cluster_conf_path);
        }
        else
        {
            throw Exception(ErrorCodes::METASTORE_EXCEPTION, "Catalog must be correctly configured. Only support foundationdb and bytekv now.");
        }
    }

    void execute(const std::string & command)
    {
        try
        {
            MetaCommand cmd = MetaCommand::parse(command);
            std::string full_key = Catalog::escapeString(name_space) + cmd.key;
            switch (cmd.type)
            {
                case MetaCommandType::HELP:
                {
                    printHelpMessage();
                    break;
                }
                case MetaCommandType::GET:
                {
                    std::string value;
                    metastore_ptr->get(full_key, value);
                    dumpMetadata(cmd.key, value);
                    break;
                }
                case MetaCommandType::LIST:
                case MetaCommandType::COUNT:
                {
                    bool need_print = cmd.type == MetaCommandType::LIST;
                    Catalog::IMetaStore::IteratorPtr it = metastore_ptr->getByPrefix(full_key);
                    size_t counter = 0;
                    while(it->next())
                    {
                        if (need_print)
                            std::cout << it->key().substr(name_space.size(), std::string::npos) << std::endl;
                        counter++;
                    }
                    std::cout << "Total: " << counter << std::endl;
                    break;
                }
                case MetaCommandType::DELETE:
                {
                    metastore_ptr->drop(full_key);
                    std::cout << "Deleted by key: " << full_key << std::endl;
                    break;
                }
                case MetaCommandType::CLEAR:
                {
                    metastore_ptr->clean(full_key);
                    std::cout << "Deleted all by prefix: " << full_key << std::endl;
                    break;
                }
            }
        }
        catch(Exception & e)
        {
            std::cout << e.message() << std::endl;
        }
    }

    std::string name_space = "default";
    MetastorePtr metastore_ptr;
};
}

int mainEntryClickhouseMetaInspector(int argc, char ** argv)
{
    try
    {
        DB::MetastoreInspector inspector;
        inspector.init(argc, argv);
        return inspector.run();
    }
    catch (const DB::Exception & e)
    {
        std::string text = e.displayText();
        std::cerr << "Code: " << e.code() << ". " << text << std::endl;
        return 1;
    }
    catch (...)
    {
        std::cerr << DB::getCurrentExceptionMessage(true) << std::endl;
        return 1;
    }
}

