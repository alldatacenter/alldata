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

#include <fstream>
#include <shared_mutex>
#include <unordered_set>
#include <Columns/ColumnString.h>
#include <DataTypes/DataTypeString.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/IFunction.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/Context.h>
#include <Poco/DirectoryIterator.h>
#include <Poco/Util/Timer.h>


#include <common/logger_useful.h>

extern "C" {
#include <ipdb.h>
#include <maxminddb.h>
}


namespace DB
{
namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int ILLEGAL_QUERY_TYPE;
    extern const int ILLEGAL_IP_ADDRESS;
    extern const int FILE_NOT_FOUND;
    extern const int LOOKUP_ERROR;
    extern const int RENAME_ERROR;
    extern const int REMOVE_FILE_ERROR;
    extern const int ILLEGAL_LOCATOR_TYPE;
    extern const int ILLEGAL_COLUMN;
}

class IP2GeoManager
{
public:
    IP2GeoManager()
    {
        log = &Poco::Logger::get("IP2GeoUDF");
        location_map["country"] = 0;
        location_map["province"] = 1;
        location_map["city"] = 2;
        location_map["owner"] = 3;
        location_map["isp"] = 4;
        location_map["latitude"] = 5;
        location_map["longitude"] = 6;
        location_map["timezone"] = 7;
        location_map["utc_offset"] = 8;
        location_map["china_admin_code"] = 9;
        location_map["idd_code"] = 10;
        location_map["country_code"] = 11;
        location_map["continent_code"] = 12;
        location_map["asn"] = -1;
        location_map["aso"] = -1;
    }

    void getSettings(ContextPtr context);

    static IP2GeoManager & getInstance()
    {
        static IP2GeoManager managerInstance;
        return managerInstance;
    }


    /*
     * Check whether files have been open, if not, open them
     */
    void openCheck(String ip_type, String query_type, String locator_type, bool is_oversea);

    /*
     * update all files when the certain time comes
     */
    void updateAll(bool oversea);

    /*
     * search the geo info for an IP
     */
    String evaluate(String ip, String ip_type, String query_type, String locator_type, String language, int location_info_type = -1);

    void freeMemory();

    std::unordered_map<String, int> & getLocationMap() { return location_map; }

private:
    Poco::Logger * log;
    Poco::Util::Timer timer;
    mutable std::shared_mutex mutex_;

    std::unordered_map<String, int> location_map;

    String IPV4_FILE;
    String IPV6_FILE;
    String GEOIP_ASN_FILE;
    String GEOIP_ISP_FILE;
    String GEOIP_CITY_FILE;
    String LOCAL_PATH;
    String LOCAL_PATH_OVERSEA;
    bool UPDATE_FROM_HDFS;

    ~IP2GeoManager() { freeMemory(); }

    void openfile(String filename);

    class IPIPLocator
    {
    public:
        static IPIPLocator & getInstance()
        {
            static IPIPLocator ipipLocatorInstance;
            return ipipLocatorInstance;
        }

        String locateIpv4(String ip, int locationType);

        String locateIpv6(String ip, int locationType);

        String getString(String ip, int locationType, ipdb_reader * reader);

        void open(String path, String ip_type);

        void freeIpipReader();

        void freeIpv6Reader();

        bool ipipreaderEmpty() { return ipipreader == nullptr; }

        bool ipv6readerEmpty() { return ipv6reader == nullptr; }

        ipdb_reader * getIPIPIpdbReader() { return ipipreader; }

        ipdb_reader * getIPv6IpdbReader() { return ipv6reader; }

    private:
        int ipiperr;
        int ipv6err;
        ipdb_reader *ipipreader, *ipv6reader;
        Poco::Logger * log;
        mutable std::shared_mutex ipipmutex_;

        IPIPLocator() { log = &Poco::Logger::get("IPIPLocator"); }
    };

    class GeoIPLocator
    {
    public:
        static GeoIPLocator & getInstance()
        {
            static GeoIPLocator geoIpLocatorInstance;
            return geoIpLocatorInstance;
        }

        String locateIpv4(String ip, String queryType, String language);

        void open(String path, String file_type);

        void freeCityMMDB();

        void freeASNMMDB();

        void freeISPMMDB();

        bool citymmdbEmpty() { return citymmdb.filename == nullptr; }

        bool asnmmdbEmpty() { return asnmmdb.filename == nullptr; }

        bool ispmmdbEmpty() { return ispmmdb.filename == nullptr; }

        MMDB_s getCityMMDB_s() { return citymmdb; }

        MMDB_s getASNMMDB_s() { return asnmmdb; }

        MMDB_s getISPMMDB_s() { return ispmmdb; }

    private:
        Poco::Logger * log;
        mutable std::shared_mutex geoipmutex_;
        MMDB_s citymmdb, ispmmdb, asnmmdb;
        GeoIPLocator() { log = &Poco::Logger::get("GEOIPLocator"); }
    };
};


class StringUtil
{
public:
    static void strSplit(const String & str, const String & sign, std::vector<String> & results);

    static String getDateTime();

    static String ipAddressType(String & ip);
};


class IP2GeoUDF : public IFunction
{
public:
    static constexpr auto name = "ip_to_geo";

    IP2GeoUDF(ContextPtr cur_context) : context(cur_context)
    {
        log = &Poco::Logger::get("IP2GeoFunc");
        IP2GeoManager::getInstance().getSettings(context);
    }

    static FunctionPtr create(ContextPtr context) { return std::make_shared<IP2GeoUDF>(context); }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        if (arguments.size() < 2 || arguments.size() > 5)
            throw Exception(
                "Number of arguments for function " + getName() + " doesn't match: passed " + toString(arguments.size())
                    + ", should be 2 or 3 or 4 or 5",
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        for (size_t i = 0; i < arguments.size(); i++)
        {
            if (!isString(arguments[i]))
                throw Exception(
                    "The " + toString(i + 1) + "-th argument for function" + getName() + " must be String",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        return std::make_shared<DataTypeString>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t input_rows_count) const override
    {
        if (const ColumnString * ip_col = checkAndGetColumn<ColumnString>(arguments[0].column.get()))
        {
            const typename ColumnString::Offsets & offsets = ip_col->getOffsets();
            const auto size = offsets.size();
            auto res_col = DataTypeString().createColumn();

            String ip, query_type, locator_type, language, oversea;
            bool is_oversea = false;

            query_type = checkAndGetColumnConst<ColumnString>(arguments[1].column.get())->getValue<String>();
            if (arguments.size() > 2)
            {
                locator_type = checkAndGetColumnConst<ColumnString>(arguments[2].column.get())->getValue<String>();
                if (arguments.size() > 3)
                {
                    language = checkAndGetColumnConst<ColumnString>(arguments[3].column.get())->getValue<String>();
                }
                if (arguments.size() > 4)
                {
                    oversea = checkAndGetColumnConst<ColumnString>(arguments[4].column.get())->getValue<String>();
                    is_oversea = (oversea == "true") ? true : false;
                }
            }
            if (locator_type.empty())
                locator_type = "ipip";
            if (language.empty())
                language = "zh-CN";

            transform(locator_type.begin(), locator_type.end(), locator_type.begin(), ::tolower);
            bool ip_v4_check = false;
            bool ip_v6_check = false;
            int location_info_type;
            std::unordered_map<String, int>::iterator iter = IP2GeoManager::getInstance().getLocationMap().find(query_type);
            if (iter != IP2GeoManager::getInstance().getLocationMap().end())
            {
                location_info_type = iter->second;
            }
            else
                throw Exception("Illegal query type " + query_type, ErrorCodes::ILLEGAL_QUERY_TYPE);

            for (size_t i = 0; i < size; ++i)
            {
                String cur_ip = ip_col->getDataAt(i).toString();
                String ip_type = StringUtil::ipAddressType(cur_ip);

                if (ip_type == "ipv6" && locator_type != "ipip")
                    throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);

                if ((ip_type == "ipv4" && !ip_v4_check) || (ip_type == "ipv6" && !ip_v6_check))
                {
                    IP2GeoManager::getInstance().openCheck(ip_type, query_type, locator_type, is_oversea);
                    if (ip_type == "ipv4")
                        ip_v4_check = true;
                    if (ip_type == "ipv6")
                        ip_v6_check = true;
                }

                String res = IP2GeoManager::getInstance().evaluate(cur_ip, ip_type, query_type, locator_type, language, location_info_type);
                res_col->insert(res);
            }
            return res_col;
        }
        else if (checkAndGetColumnConst<ColumnString>(arguments[0].column.get()))
        {
            String ip, query_type, locator_type, language, oversea;
            bool is_oversea = false;
            ip = checkAndGetColumnConst<ColumnString>(arguments[0].column.get())->getValue<String>();
            query_type = checkAndGetColumnConst<ColumnString>(arguments[1].column.get())->getValue<String>();
            if (arguments.size() > 2)
            {
                locator_type = checkAndGetColumnConst<ColumnString>(arguments[2].column.get())->getValue<String>();
                if (arguments.size() > 3)
                {
                    language = checkAndGetColumnConst<ColumnString>(arguments[3].column.get())->getValue<String>();
                }
                if (arguments.size() > 4)
                {
                    oversea = checkAndGetColumnConst<ColumnString>(arguments[4].column.get())->getValue<String>();
                    is_oversea = (oversea == "true") ? true : false;
                }
            }
            if (locator_type.empty())
                locator_type = "ipip";
            if (language.empty())
                language = "zh-CN";

            transform(locator_type.begin(), locator_type.end(), locator_type.begin(), ::tolower);
            String ip_type = StringUtil::ipAddressType(ip);
            if (ip_type == "ipv6" && locator_type != "ipip")
                throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
            int location_info_type;
            std::unordered_map<String, int>::iterator iter = IP2GeoManager::getInstance().getLocationMap().find(query_type);
            if (iter != IP2GeoManager::getInstance().getLocationMap().end())
            {
                location_info_type = iter->second;
            }
            else
                throw Exception("Illegal query type " + query_type, ErrorCodes::ILLEGAL_QUERY_TYPE);

            IP2GeoManager::getInstance().openCheck(ip_type, query_type, locator_type, is_oversea);
            String res = IP2GeoManager::getInstance().evaluate(ip, ip_type, query_type, locator_type, language, location_info_type);
            return DataTypeString().createColumnConst(input_rows_count, res);
        }
        else
        {
            throw Exception(
                "Illegal column " + arguments[0].column->getName() + " of first argument of function " + getName(),
                ErrorCodes::ILLEGAL_COLUMN);
        }
    }

private:
    ContextPtr context;
    Poco::Logger * log;
};
}
