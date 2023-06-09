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

#include<algorithm>
#include<iostream>
#include<fstream>
#include<string>
#include<array>

using namespace std;

static const string FAKE_ZONE_DOMAIN = "cnch.svc.cluster.local.";
static const string PTR_DOMAIN = "in-addr.arpa.";
static const string HEADLESS_TAIL = "-headless";
static const string SRV_TEMPLATE = "_port[id]._tcp.[vw_name]	   IN   SRV   0 50 8081 .";
static const string SOA_TEMPLATE = "$ORIGIN {}\n"
                    "@       3601 IN SOA {} {} (\n"
                    "                                2017042745 ; serial\n"
                    "                                7200       ; refresh (2 hours)\n"
                    "                                3600       ; retry (1 hour)\n"
                    "                                1209600    ; expire (2 weeks)\n"
                    "                                3601       ; minimum (1 hour)\n"
                    "                                )";

static const int ENDPOINTS_THRESHOLD_FOR_ONE_SERVICE = 100;

std::ofstream ptr_info_stream, zone_info_stream;

/**
 *
 * @param s  origin string in place
 * @param occurStr pattern
 * @param replaceStr replacement
 * @return the string s after replace all occurStr string by replaceStr string
 */
std::string findAndReplaceAll(std::string s, std::string occurStr, std::string replaceStr){
    std::size_t pos = s.find(occurStr);
    while (pos!=std::string::npos){
        s.replace(pos, occurStr.size(), replaceStr);
        pos = s.find(occurStr, pos + replaceStr.size());
    }
    return s;
}

/**
 *
 * @param vw_name_template faked virtual warehouse name
 * @param num_total number of total endpoints intended to register
 * @param num_expected number of expected endpoints return when querying by DNS SD module
 * @param startIP the start faked IP
 */
void fakedEndpointRegister(std::string vw_name_template, int num_total, int num_expected, array<int, 4> startIP){
    auto nextIP = [](array<int, 4> ip) -> array<int, 4>{
        for (int i = 3; i >= 0; i--){
            ip[i]++;
            if (ip[i] <= 255){
                break;
            }
            ip[i] = 0; continue;
        }
        return ip;
    };
    int vw_id = 1;
    // A & PTR records
    {
        std::string vw_name = findAndReplaceAll(vw_name_template, "{}", to_string(vw_id));
        zone_info_stream << vw_name << HEADLESS_TAIL;
        int cur = 0;
        for (int i = 1; i <= num_total; i++) {
            if (i > num_expected){
                if (cur % ENDPOINTS_THRESHOLD_FOR_ONE_SERVICE == 0){ // beginning of a new service registration;
                    vw_id++;
                    vw_name = findAndReplaceAll(vw_name_template, "{}", to_string(vw_id));
                    zone_info_stream << vw_name << HEADLESS_TAIL;
                }
                cur++;
            }
            zone_info_stream << " IN A " << startIP[0] << "." << startIP[1] << "." << startIP[2] << "."
                             << startIP[3] << std::endl;
            ptr_info_stream << startIP[3] << "." << startIP[2] << "." << startIP[1] << "." << startIP[0]
                            << " IN PTR "
                            << vw_name << HEADLESS_TAIL << "." << FAKE_ZONE_DOMAIN << std::endl;
            startIP = nextIP(startIP);
        }
    }
    // SRV records
    {
        while (vw_id > 0){
            std::string vw_name = findAndReplaceAll(vw_name_template, "{}", to_string(vw_id));
            for (int port_id = 0; port_id <= 6; port_id++){
                if (port_id!=3 && port_id!=4){ // PORT3 and PORT4 not used for lookup process
                    zone_info_stream << findAndReplaceAll(findAndReplaceAll(SRV_TEMPLATE, "[id]",to_string(port_id)),
                                                    "[vw_name]", vw_name) << std::endl;
                }
            }
            vw_id--;
        }
    }
}

int main(int argc, char** args){
    if (argc != 5){
        std::cout << "Usage:  ./fake_endpoints_registration [PATH_TO_domain_zone_file] [PATH_TO_in-addr.arpa] [TOTAL_NO_ENDPOINT] [EXPECTED_NO_ENDPOINT]" << std::endl;
        return -1;
    }
    static const string zone_info_location = args[1];
    static const string ptr_info_location = args[2];
    static const int num_total = atoi(args[3]);
    static const int num_expected = atoi(args[4]);
    ptr_info_stream.open(ptr_info_location);
    zone_info_stream.open(zone_info_location);
    if (num_total < num_expected){
        std::cout << "Invalid number of expected endpoints" << std::endl;
        return -1;
    }
    if (num_expected > ENDPOINTS_THRESHOLD_FOR_ONE_SERVICE){
        std::cout << "Number of expected endpoints is bigger than allowed threshold" << std::endl;
        return -1;
    }
    // SOA record;
    {
        ptr_info_stream << findAndReplaceAll(SOA_TEMPLATE, "{}", PTR_DOMAIN) << std::endl;
        zone_info_stream << findAndReplaceAll(SOA_TEMPLATE, "{}", FAKE_ZONE_DOMAIN) << std::endl;
    }
    fakedEndpointRegister("vw{}-default", num_total, num_expected, {0, 0, 0, 0});
    ptr_info_stream.close();
    zone_info_stream.close();
    return 0;
}

