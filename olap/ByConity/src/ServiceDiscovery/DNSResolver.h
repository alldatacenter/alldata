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

#include <string>
#include <vector>
#include <memory>
#include <netinet/in.h>
#include <udns/udns.h>
#include <stdexcept>

namespace dns {

struct DNSResolveException : public std::runtime_error {
	DNSResolveException(int err_code_, std::string const & message);
    int err_code;
};

class DNSResolver {
public:
    using A4RecPtr = std::shared_ptr<dns_rr_a4>;
    using SrvRecPtr = std::shared_ptr<dns_rr_srv>;
    using PtrRecPtr = std::shared_ptr<dns_rr_ptr>;

    DNSResolver();
    ~DNSResolver();

    DNSResolver(const DNSResolver &) = delete;
    DNSResolver& operator=(const DNSResolver &) = delete;

    // udp only using udns.h library
    // A4 Record
    A4RecPtr resolveA4(const std::string & name);

    // SRV Record
    SrvRecPtr resolveSrv(const std::string & full_name);
    SrvRecPtr resolveSrv(const std::string & name, const std::string & service, const std::string & protocol);

    // PTR Record
    PtrRecPtr resolvePtr(const std::string & host);

    // tcp only using getaddrinfo
    static std::vector<std::string> resolveA4ByTCP(const std::string & name);

    // helper methods
    static std::string inAddrToStr(const in_addr_t & ip);
    static in_addr_t inStrToAddr(const std::string & host);
private:
    dns_ctx * ctx;

    void check_status();
    void init_ctx();
    void open_ctx();
    void free_ctx();
};

}
