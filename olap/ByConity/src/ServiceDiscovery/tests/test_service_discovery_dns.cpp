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

#include <ServiceDiscovery/ServiceDiscoveryDNS.h>
#include <iostream>
#include <string>
#include <arpa/inet.h>
#include <Poco/AutoPtr.h>
#include <Poco/Util/XMLConfiguration.h>
#include <Common/ThreadPool.h>
#include <Common/Stopwatch.h>

// current as the scope of the testing the value of those ports are set to be the same value;
// in the future, if require testing with different port value, could just change the value of those ports here!
constexpr auto PORT0 = 8081;
constexpr auto PORT1 = 8081;
constexpr auto PORT2 = 8081;
[[maybe_unused]] constexpr auto PORT5 = 8081;
[[maybe_unused]] constexpr auto PORT6 = 8081;
constexpr auto PSM = "data.cnch.vw";
constexpr auto HOSTNAME_TAIL = "-headless.cnch.svc.cluster.local";

UInt64 runLookup(std::shared_ptr<DB::ServiceDiscoveryDNS> &sd, std::string vw_name, bool show_detail, int expected_size){
    Stopwatch watch;
    auto endpoints = sd->lookup(PSM, DB::ComponentType::WORKER, vw_name);
    watch.stop();
    if (static_cast<int>(endpoints.size()) != expected_size){
	    std::cerr << "Lookup endpoint size incorrect: expected size [" << expected_size
	          << "], actual size [" << endpoints.size() << "]" << std::endl;
	return 0;
    }

    UInt64 elapsed_time = watch.elapsedMicroseconds();
    if (show_detail){
	    std::cout << "Lookup [" << endpoints.size() << "] endpoints, time elapsed: "
	          <<  elapsed_time << " microseconds" << std::endl;
    }
    return elapsed_time;
}

bool functionality_test(std::shared_ptr<DB::ServiceDiscoveryDNS> &sd, std::string vw_name, bool enable_cache = false){
    std::cout << "Start functionality test." << std::endl;

    auto isValidHost = [](std::string host) -> bool{
	 struct sockaddr_in sa;
	 int result = inet_pton(AF_INET, host.c_str(), &(sa.sin_addr));
	 return result != 0;
    };

    auto isValidHostName = [](std::string hostname, std::string vw_name_) -> bool{
	 return hostname == vw_name_.append(HOSTNAME_TAIL);
    };

    // Test worker
    {
       // lookup test
       DB::HostWithPortsVec endpoints;
       std::string HOST = vw_name + HOSTNAME_TAIL;
       if (enable_cache){
	  sd->clearCache();
	  endpoints = sd->lookup(PSM, DB::ComponentType::WORKER, vw_name); // Warm up cache
       }
       endpoints = sd->lookup(PSM, DB::ComponentType::WORKER, vw_name);
       if (endpoints.empty()){
	   std::cerr << "fakedLookup worker endpoints return empty result" << std::endl;
	   return false;
       }

       if (!isValidHost(endpoints[0].getHost()) || !isValidHostName(endpoints[0].id, vw_name) ||
            	endpoints[0].tcp_port != PORT0 ||  endpoints[0].rpc_port != PORT1 || endpoints[0].http_port != PORT2){
		std::cerr << "fakedLookup worker endpoints invalid" << std::endl;
		return false;
 	}
	 std::cout << "fakedLookup worker functionality test passed!" << std::endl;
    }

    return true;
}

void performance_test(std::shared_ptr<DB::ServiceDiscoveryDNS> &sd, std::string vw_name, int expected_size, int concurrency = 1, bool enable_cache = false){
   std::cout << "Start performace test." << std::endl;
   ThreadPool thread_pool(concurrency);
   std::atomic<UInt64> total_time_us {0};

   // concurrency test for runFakedLookup
   {
	total_time_us = 0;
	if (enable_cache){
	    sd->clearCache();
	    runLookup(sd, vw_name, false, expected_size);
	}
	for (int i = 0; i < concurrency; i++){
	    auto task = [&]
	    {
		total_time_us += runLookup(sd, vw_name, false, expected_size);
   	    };
	    thread_pool.trySchedule(task);
	}
	thread_pool.wait();
	std::cout << "Concurrent [" << concurrency << "] lookup [" << expected_size
	          << "] endpoints, avg time elapsed: " << total_time_us / concurrency
		      << " microseconds" << std::endl;
   }

   std::cout << "Performance test passed." << std::endl << std::endl;
}

int main(int arg, char** argv){
    int concurrency = 0;
    int num_eps = 0;
    std::string vw_name = "vw-default";
    bool enable_cache = false;
    auto toLower = [](char* s) -> std::string{
	std::string res = "";
	for (int i = 0; i < static_cast<int>(strlen(s)); i++){
	    if (s[i] >= 'A' && s[i] <= 'Z'){
		res += (s[i] - ('A' - 'a'));
	    }
	    else{
		res += s[i];
	    }
	}
	return s;
    };
    if (arg >= 4){
       concurrency = atoi(argv[1]);
       num_eps = atoi(argv[2]);
       vw_name = argv[3];
       if (arg >= 5){
	 std::string cacheOption = toLower(argv[4]);
	 if (cacheOption == "y"){
            enable_cache = true;
	 }
	 else if (cacheOption != "n"){
	    std::cout << "Invalid value for [enable_cache], please enter Y(y) or N(n)" << std::endl;
	    return -1;
	 }
       }
       if (concurrency <= 0){
	  std::cerr << "Concurrency threads number should be a positive number" << std::endl;
	  return -1;
       }
    }
    else{
	std::cerr << "Usage: ./test_service_discovery_dns [*concurrency] [*num_total_endpoints] [*vw_name] [enable_cache(y/n)]" << std::endl;
	std::cerr << "* means compulsory parameters" << std::endl;
	return -1;
    }

    using Poco::AutoPtr;
    using Poco::Util::XMLConfiguration;

    AutoPtr<XMLConfiguration> pconf(new XMLConfiguration());

    std::cout << "DNS mode " <<  (enable_cache ? "with" : "without")
              << " cache" << std::endl
	          << std::endl;

    pconf->setBool("service_discovery.disable_cache", !enable_cache);

    auto sd = std::make_shared<DB::ServiceDiscoveryDNS>(*pconf.get());

    if (functionality_test(sd, vw_name, enable_cache) == false){
       std::cout << "Functionality test failed!" << std::endl;
       return -1;
    }

    std::cout << "Total endpoints: " << num_eps << std::endl
              << "Concurrency: " << concurrency << std::endl
     	      << std::endl;

    performance_test(sd, vw_name, num_eps, concurrency, enable_cache);

    return 0;
}
