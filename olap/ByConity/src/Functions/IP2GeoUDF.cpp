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

//
// Created by bytedance on 2020/7/7.
//

#include <Functions/FunctionFactory.h>
#include <Functions/IP2GeoUDF.h>
#include <boost/asio.hpp>
#include <sys/stat.h>
#include <sys/types.h>
#include <common/logger_useful.h>

extern "C" {
#include <ipdb.h>
#include <maxminddb.h>
}


namespace DB
{
void registerFunctionIP2Geo(FunctionFactory & factory)
{
    factory.registerFunction<IP2GeoUDF>(FunctionFactory::CaseInsensitive);
}

void IP2GeoManager::getSettings(ContextPtr context)
{
    IPV4_FILE = context->getSettings().ipv4_file;
    IPV6_FILE = context->getSettings().ipv6_file;
    GEOIP_ASN_FILE = context->getSettings().geoip_asn_file;
    GEOIP_ISP_FILE = context->getSettings().geoip_isp_file;
    GEOIP_CITY_FILE = context->getSettings().geoip_city_file;
    LOCAL_PATH = context->getSettings().ip2geo_local_path;
    LOCAL_PATH_OVERSEA = context->getSettings().ip2geo_local_path_oversea;
    UPDATE_FROM_HDFS = context->getSettings().ip2geo_update_from_hdfs;

    if (UPDATE_FROM_HDFS)
        throw Exception("Cannot not support hdfs file", ErrorCodes::LOGICAL_ERROR);


    if (access(LOCAL_PATH.c_str(), 0) == -1)
    {
        if (mkdir(LOCAL_PATH.c_str(), 0777) == 0)
        {
            log->debug("Create local path for IPDB files");
        }
        else
        {
            throw Exception("Cannot make directory " + LOCAL_PATH, ErrorCodes::FILE_NOT_FOUND);
        }
    }

    if (access(LOCAL_PATH_OVERSEA.c_str(), 0) == -1)
    {
        if (mkdir(LOCAL_PATH_OVERSEA.c_str(), 0777) == 0)
        {
            log->debug("Create local path for IPDB files for oversea");
        }
        else
        {
            throw Exception("Cannot make directory " + LOCAL_PATH_OVERSEA, ErrorCodes::FILE_NOT_FOUND);
        }
    }
}

/*
     *  This function is to check whether the file has been open, if not, open the file
     */
void IP2GeoManager::openCheck(String ip_type, String query_type, String locator_type, bool is_oversea)
{
    std::unique_lock<std::shared_mutex> lock(mutex_);
    String local_path = LOCAL_PATH;
    if (is_oversea)
        local_path = LOCAL_PATH_OVERSEA;

    if (ip_type == "ipv6")
    {
        if (ip_type == "ipv6" && locator_type != "ipip")
            throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
        ipdb_reader * ipdbReader = IPIPLocator::getInstance().getIPv6IpdbReader();
        if (ipdbReader == nullptr)
        {
            IPIPLocator::getInstance().open(local_path + IPV6_FILE + ".ipdb", "ipv6");
        }
    }
    else if (ip_type == "ipv4")
    {
        if (locator_type == "ipip")
        {
            ipdb_reader * ipdbReader = IPIPLocator::getInstance().getIPIPIpdbReader();
            if (ipdbReader == nullptr)
            {
                IPIPLocator::getInstance().open(local_path + IPV4_FILE + ".ipdb", "ipv4");
            }
        }
        else
        {
            if (locator_type == "all")
            {
                ipdb_reader * ipdbReader = IPIPLocator::getInstance().getIPIPIpdbReader();
                if (ipdbReader == nullptr)
                {
                    IPIPLocator::getInstance().open(local_path + IPV4_FILE + ".ipdb", "ipv4");
                }
            }
            if (locator_type == "all" || locator_type == "geoip")
            {
                if (query_type == "asn" || query_type == "aso")
                {
                    MMDB_s asnmmdb = GeoIPLocator::getInstance().getASNMMDB_s();
                    if (asnmmdb.filename == nullptr)
                    {
                        GeoIPLocator::getInstance().open(local_path + GEOIP_ASN_FILE + ".mmdb", "asn");
                    }
                }
                else if (query_type == "isp")
                {
                    MMDB_s ispmmdb = GeoIPLocator::getInstance().getISPMMDB_s();
                    if (ispmmdb.filename == nullptr)
                    {
                        GeoIPLocator::getInstance().open(local_path + GEOIP_ISP_FILE + ".mmdb", "isp");
                    }
                }
                else
                {
                    MMDB_s citymmdb = GeoIPLocator::getInstance().getCityMMDB_s();
                    if (citymmdb.filename == nullptr)
                    {
                        GeoIPLocator::getInstance().open(local_path + GEOIP_CITY_FILE + ".mmdb", "city");
                    }
                }
            }
            else
                throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
        }
    }
}

void IP2GeoManager::openfile(String filename)
{
    if (filename.find(IPV6_FILE) != String::npos)
        IPIPLocator::getInstance().open(filename, "ipv6");
    if (filename.find(IPV4_FILE) != String::npos)
        IPIPLocator::getInstance().open(filename, "ipv4");
    if (filename.find(GEOIP_CITY_FILE) != String::npos)
        GeoIPLocator::getInstance().open(filename, "city");
    if (filename.find(GEOIP_ISP_FILE) != String::npos)
        GeoIPLocator::getInstance().open(filename, "isp");
    if (filename.find(GEOIP_ASN_FILE) != String::npos)
        GeoIPLocator::getInstance().open(filename, "asn");
}

String IP2GeoManager::evaluate(String ip, String ip_type, String query_type, String locator_type, String language, int location_info_type)
{
    std::shared_lock<std::shared_mutex> lock(mutex_);
    if (ip.empty() || query_type.empty() || locator_type.empty())
        throw Exception("Empty argument ip, queryType or locatorType", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (location_info_type != -1)
    {
        if (ip_type == "ipv4")
        {
            if (locator_type == "ipip")
            {
                return IPIPLocator::getInstance().locateIpv4(ip, location_info_type);
            }
            else if (locator_type == "geoip")
            {
                return GeoIPLocator::getInstance().locateIpv4(ip, query_type, language);
            }
            else if (locator_type == "all")
            {
                String result = IPIPLocator::getInstance().locateIpv4(ip, location_info_type);
                if (result.empty())
                {
                    result = GeoIPLocator::getInstance().locateIpv4(ip, query_type, language);
                }
                return result;
            }
            else
                throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
        }
        else if (ip_type == "ipv6")
        {
            if (locator_type != "ipip")
                throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
            return IPIPLocator::getInstance().locateIpv6(ip, location_info_type);
        }
        else
            throw Exception("Illegal IP address " + ip, ErrorCodes::ILLEGAL_IP_ADDRESS);
    }
    else //May be looking for asn or aso
    {
        if (ip_type == "ipv4")
        {
            if (locator_type == "ipip")
            {
                throw Exception("Illegal queryType for IPIP, queryType: " + query_type, ErrorCodes::ILLEGAL_QUERY_TYPE);
            }
            else if (locator_type == "geoip" || locator_type == "all")
            {
                return GeoIPLocator::getInstance().locateIpv4(ip, query_type, language);
            }
            else
                throw Exception("Illegal locator type " + locator_type, ErrorCodes::ILLEGAL_LOCATOR_TYPE);
        }
        else if (ip_type == "ipv6")
            throw Exception("Illegal query type " + query_type, ErrorCodes::ILLEGAL_QUERY_TYPE);
        else
            throw Exception("Illegal IP address " + ip, ErrorCodes::ILLEGAL_IP_ADDRESS);
    }
}

void IP2GeoManager::freeMemory()
{
    std::unique_lock<std::shared_mutex> lock(mutex_);
    IPIPLocator & ipip_instance = IPIPLocator::getInstance();
    GeoIPLocator & geoip_instance = GeoIPLocator::getInstance();

    ipip_instance.freeIpipReader();
    ipip_instance.freeIpv6Reader();
    geoip_instance.freeCityMMDB();
    geoip_instance.freeISPMMDB();
    geoip_instance.freeASNMMDB();
}

/*
     * Functions of IPIPLocator
    */

void IP2GeoManager::IPIPLocator::open(String path, String ip_type)
{
    std::unique_lock<std::shared_mutex> lock(ipipmutex_);
    if (ip_type == "ipv4")
    {
        if (!ipipreader)
        {
            log->debug("IPv4 reader is empty");
            ipiperr = ipdb_reader_new(path.c_str(), &ipipreader);
            if (ipiperr)
                throw Exception("Cannot open file " + path, ErrorCodes::FILE_NOT_FOUND);
        }
    }

    if (ip_type == "ipv6")
    {
        if (!ipv6reader)
        {
            log->debug("IPv6 reader is empty");
            ipv6err = ipdb_reader_new(path.c_str(), &ipv6reader);
            if (ipv6err)
                throw Exception("Cannot open file " + path, ErrorCodes::FILE_NOT_FOUND);
        }
    }
}


String IP2GeoManager::IPIPLocator::locateIpv4(String ip, int location_type)
{
    std::shared_lock<std::shared_mutex> lock(ipipmutex_);
    if (ipipreader)
        return getString(ip, location_type, ipipreader);
    log->debug("ipiperr " + toString(ipiperr));
    throw Exception("IPDB file is not open or memory has been freed", ErrorCodes::FILE_NOT_FOUND);
}

String IP2GeoManager::IPIPLocator::locateIpv6(String ip, int location_type)
{
    std::shared_lock<std::shared_mutex> lock(ipipmutex_);
    if (ipv6reader)
        return getString(ip, location_type, ipv6reader);
    log->debug("ipv6err" + toString(ipv6err));
    throw Exception("IPDB file is not open or memory has been free", ErrorCodes::FILE_NOT_FOUND);
}

String IP2GeoManager::IPIPLocator::getString(String ip, int location_type, ipdb_reader * reader)
{
    char body[512];
    if (reader == nullptr)
        throw Exception("IPDB file is not open or memory has been freed ", ErrorCodes::FILE_NOT_FOUND);
    int err = ipdb_reader_find(reader, ip.c_str(), "CN", body);
    if (!err)
    {
        char tmp[64];
        int f = 0, p1 = 0, p2 = -1;
        do
        {
            if (*(body + p1) == '\t' || !*(body + p1))
            {
                if (f == location_type)
                {
                    strncpy(tmp, body + p2 + 1, static_cast<size_t>(p1 - p2));
                    tmp[p1 - p2] = 0;
                    return tmp;
                }
                else
                {
                    p2 = p1;
                    ++f;
                }
            }
        } while (*(body + p1++));
    }
    return "NULL";
}

void IP2GeoManager::IPIPLocator::freeIpipReader()
{
    if (ipipreader)
    {
        log->debug("Free ipipreader");
        ipdb_reader_free(&ipipreader);
    }
}

void IP2GeoManager::IPIPLocator::freeIpv6Reader()
{
    if (ipv6reader)
    {
        log->debug("Free ipv6reader");
        ipdb_reader_free(&ipv6reader);
    }
}

/*
     *  Functions of GeoIPLocator
     */

void IP2GeoManager::GeoIPLocator::open(String path, String file_type)
{
    std::unique_lock<std::shared_mutex> lock(geoipmutex_);
    if (file_type == "city")
    {
        if (!citymmdb.filename)
        {
            log->debug("CityMMDB is empty");
            int cityStatus = MMDB_open(path.c_str(), MMDB_MODE_MMAP, &citymmdb);
            if (MMDB_SUCCESS != cityStatus)
            {
                String err_msg;
                err_msg.append("Cannot open file ").append(path).append(" - ").append(MMDB_strerror(cityStatus)).append("\n");
                log->debug(err_msg);
                if (MMDB_IO_ERROR == cityStatus)
                {
                    String err_msg_io;
                    err_msg_io.append("IO error: ").append(strerror(errno)).append("\n");
                    log->debug(err_msg_io);
                }
                throw Exception(err_msg, ErrorCodes::LOOKUP_ERROR);
            }
        }
    }

    if (file_type == "isp")
    {
        if (!ispmmdb.filename)
        {
            log->debug("ISPMMDB is empty");
            int ispStatus = MMDB_open(path.c_str(), MMDB_MODE_MMAP, &ispmmdb);
            if (MMDB_SUCCESS != ispStatus)
            {
                String err_msg;
                err_msg.append("Cannot open file ").append(path).append(" - ").append(MMDB_strerror(ispStatus)).append("\n");
                log->debug(err_msg);
                if (MMDB_IO_ERROR == ispStatus)
                {
                    String err_msg_io;
                    err_msg_io.append("IO error: ").append(strerror(errno)).append("\n");
                    log->debug(err_msg_io);
                }
                throw Exception(err_msg, ErrorCodes::LOOKUP_ERROR);
            }
        }
    }

    if (file_type == "asn")
    {
        if (!asnmmdb.filename)
        {
            log->debug("ASNMMDB is empty");
            int asnStatus = MMDB_open(path.c_str(), MMDB_MODE_MMAP, &asnmmdb);
            if (MMDB_SUCCESS != asnStatus)
            {
                String err_msg;
                err_msg.append("Cannot open file ").append(path).append(" - ").append(MMDB_strerror(asnStatus)).append("\n");
                log->debug(err_msg);
                if (MMDB_IO_ERROR == asnStatus)
                {
                    String err_msg_io;
                    err_msg_io.append("IO error: ").append(strerror(errno)).append("\n");
                    log->debug(err_msg_io);
                }
                throw Exception(err_msg, ErrorCodes::LOOKUP_ERROR);
            }
        }
    }
}

String IP2GeoManager::GeoIPLocator::locateIpv4(String ip, String query_type, String language)
{
    std::shared_lock<std::shared_mutex> lock(geoipmutex_);
    int gai_error, mmdb_error;
    MMDB_lookup_result_s result;
    MMDB_entry_data_s entry_data;
    String res;

    if (query_type == "asn" || query_type == "aso")
    {
        if (asnmmdb.filename == nullptr)
            throw Exception("GeoIP file has not been opened or memory has freed", ErrorCodes::FILE_NOT_FOUND);
        result = MMDB_lookup_string(&asnmmdb, ip.c_str(), &gai_error, &mmdb_error);
    }
    else if (query_type == "isp")
    {
        if (ispmmdb.filename == nullptr)
            throw Exception("GeoIP file has not been opened or memory has freed", ErrorCodes::FILE_NOT_FOUND);
        result = MMDB_lookup_string(&ispmmdb, ip.c_str(), &gai_error, &mmdb_error);
    }
    else
    {
        if (citymmdb.filename == nullptr)
            throw Exception("GeoIP file has not been opened or memory has freed", ErrorCodes::FILE_NOT_FOUND);
        result = MMDB_lookup_string(&citymmdb, ip.c_str(), &gai_error, &mmdb_error);
    }

    if (gai_error != 0)
    {
        String err_msg;
        err_msg.append("Error from getaddrinfo for ").append(ip).append(gai_strerror(gai_error)).append("\n");
        log->debug(err_msg);
        throw Exception(err_msg, ErrorCodes::LOOKUP_ERROR);
    }
    if (MMDB_SUCCESS != mmdb_error)
    {
        String err_msg;
        err_msg.append("Got an error from libmaxminddb: ").append(MMDB_strerror(mmdb_error)).append("\n");
        log->debug(err_msg);
        throw Exception(err_msg, ErrorCodes::LOOKUP_ERROR);
    }
    if (result.found_entry)
    {
        int status;

        if (query_type == "asn")
            status = MMDB_get_value(&result.entry, &entry_data, "autonomous_system_number", NULL);
        else if (query_type == "aso")
            status = MMDB_get_value(&result.entry, &entry_data, "autonomous_system_organization", NULL);
        else if (query_type == "isp")
            status = MMDB_get_value(&result.entry, &entry_data, "isp", NULL);
        else
        {
            if (query_type == "city" || query_type == "country")
                status = MMDB_get_value(&result.entry, &entry_data, query_type.c_str(), "names", language.c_str(), NULL);
            else if (query_type == "longitude" || query_type == "latitude" || query_type == "timezone")
            {
                if (query_type == "timezone")
                    query_type = "time_zone";
                status = MMDB_get_value(&result.entry, &entry_data, "location", query_type.c_str(), NULL);
            }
            else if (query_type == "continent_code")
                status = MMDB_get_value(&result.entry, &entry_data, "continent", "code", NULL);
            else if (query_type == "country_code")
                status = MMDB_get_value(&result.entry, &entry_data, "country", "iso_code", NULL);
            else if (query_type == "province")
                status = MMDB_get_value(&result.entry, &entry_data, "subdivisions", "0", "names", language.c_str(), NULL);
            else
                throw Exception("Illegal query type " + query_type, ErrorCodes::ILLEGAL_QUERY_TYPE);
        }

        if (MMDB_SUCCESS == status)
        {
            if (entry_data.has_data)
            {
                if (entry_data.type == MMDB_DATA_TYPE_UTF8_STRING)
                {
                    String tmp(entry_data.utf8_string);
                    tmp = tmp.substr(0, entry_data.data_size);
                    res = tmp;
                }
                else if (entry_data.type == MMDB_DATA_TYPE_DOUBLE)
                {
                    String tmp(toString(entry_data.double_value));
                    res = tmp;
                }
                else if (entry_data.type == MMDB_DATA_TYPE_UINT32)
                {
                    String tmp(toString(entry_data.uint32));
                    res = tmp;
                }
                else
                {
                    String err_msg = "Data type = " + toString(entry_data.type);
                    log->debug(err_msg);
                    res = "NULL";
                }
            }
            else
            {
                String err_msg = "MMDB_get_value not found ";
                err_msg.append("\n");
                log->debug(err_msg);
                res = "NULL";
            }
        }
        else
        {
            String err_msg;
            err_msg.append("MMDB_get_value failed, ").append(MMDB_strerror(status)).append("\n");
            log->debug(err_msg);
            res = "NULL";
        }
    }

    return res;
}

void IP2GeoManager::GeoIPLocator::freeCityMMDB()
{
    if (citymmdb.filename)
    {
        log->debug("Free citymmdb");
        MMDB_close(&citymmdb);
    }
}

void IP2GeoManager::GeoIPLocator::freeISPMMDB()
{
    if (ispmmdb.filename)
    {
        log->debug("Free ispmmdb");
        MMDB_close(&ispmmdb);
    }
}

void IP2GeoManager::GeoIPLocator::freeASNMMDB()
{
    if (asnmmdb.filename)
    {
        log->debug("Free asnmmdb");
        MMDB_close(&asnmmdb);
    }
}

/*
     *  Functions of StringUtil
     */
void StringUtil::strSplit(const String & str, const String & sign, std::vector<String> & results)
{
    String::size_type pos;
    size_t size = str.size();
    for (size_t i = 0; i < size; ++i)
    {
        pos = str.find(sign, i);
        if (pos == str.npos)
        {
            String s = str.substr(i, size);
            results.push_back(s);
            break;
        }
        if (pos < size)
        {
            String s = str.substr(i, pos - i);
            results.push_back(s);
            i = pos;
        }
    }
}

String StringUtil::getDateTime()
{
    time_t t = time(nullptr);
    char timestampArr[32] = {'\0'};
    strftime(timestampArr, sizeof(timestampArr), "%Y%m%d", localtime(&t));
    String timestamp = timestampArr;
    return timestamp;
}

String StringUtil::ipAddressType(String & ip)
{
    boost::system::error_code ec;

    boost::asio::ip::make_address_v4(ip, ec);
    if (!ec)
        return "ipv4";

    boost::asio::ip::make_address_v6(ip, ec);
    if (!ec)
        return "ipv6";
    return "neither";
}
}
