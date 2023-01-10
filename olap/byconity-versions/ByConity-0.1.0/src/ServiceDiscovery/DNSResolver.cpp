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

#include "DNSResolver.h"
#include <errno.h>
#include <mutex>
#include <arpa/inet.h>
#include <netdb.h>
#include <cstring>

namespace dns {
// udns returns err code from -1 to -6
#define DNS_E_GETADDRINFOFAIL -7

std::once_flag init_defctx;

DNSResolveException::DNSResolveException(int err_code_, std::string const & message)
    : std::runtime_error(message), err_code(err_code_) {}

DNSResolver::DNSResolver()
{
    init_ctx();
    open_ctx();
}

DNSResolver::~DNSResolver() {
    free_ctx();
}

DNSResolver::A4RecPtr DNSResolver::resolveA4(const std::string & name)
{
    std::shared_ptr<dns_rr_a4> res(dns_resolve_a4(ctx, name.c_str(), 0), free);
    check_status();

    return res;
}

DNSResolver::SrvRecPtr DNSResolver::resolveSrv(const std::string & full_name)
{
    std::shared_ptr<dns_rr_srv> res(dns_resolve_srv(ctx, full_name.c_str(), nullptr, nullptr, 0), free);
    check_status();

    return res;
}

DNSResolver::SrvRecPtr DNSResolver::resolveSrv(const std::string & name, const std::string & service, const std::string & protocol)
{
    std::shared_ptr<dns_rr_srv> res(dns_resolve_srv(ctx, name.c_str(), service.c_str(), protocol.c_str(), 0), free);
    check_status();

    return res;
}

DNSResolver::PtrRecPtr DNSResolver::resolvePtr(const std::string & host)
{
    struct in_addr paddr{inStrToAddr(host)};
    std::shared_ptr<dns_rr_ptr> res(dns_resolve_a4ptr(ctx, &paddr), free);
    check_status();

    return res;
}

void DNSResolver::check_status()
{
    int err_code = dns_status(ctx);
    if (err_code < 0) {
        throw DNSResolveException(err_code, dns_strerror(err_code));
    }
}

void DNSResolver::init_ctx()
{
    // reset defctx, this is executed only once
    std::call_once(init_defctx, []() { dns_reset(nullptr); });

    // create new ctx by copying default ctx
    if (!(ctx = dns_new(nullptr))) {
        throw DNSResolveException(DNS_E_TEMPFAIL, "Failed to create dns ctx");
    }

    if (dns_init(ctx, 0) < 0) {
        throw DNSResolveException(DNS_E_TEMPFAIL, "Failed to init dns ctx");
    }
}

void DNSResolver::open_ctx()
{
    int fd_udns;
    if ((fd_udns = dns_open(ctx)) < 0) {
        throw DNSResolveException(DNS_E_TEMPFAIL, "Failed to open dns ctx");
    }
}

void DNSResolver::free_ctx()
{
    if (ctx) {
        dns_free(ctx);
        ctx = nullptr;
    }
}


std::vector<std::string> DNSResolver::resolveA4ByTCP(const std::string & name) {
    std::vector<std::string> result;

    addrinfo hints;
    std::memset(&hints, 0, sizeof(hints));
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_family = AF_INET;
    hints.ai_flags = AI_PASSIVE;
    hints.ai_protocol = 0;

    addrinfo * res;
    int err = getaddrinfo(name.c_str(), nullptr, &hints, &res);
    if (err)
    {
        if (err == EAI_NONAME)
            return result;
        throw DNSResolveException(DNS_E_GETADDRINFOFAIL, "dns lookup error" + std::string(gai_strerror(err)));
    }

    for (auto * ptr = res; ptr != nullptr; ptr = ptr->ai_next)
    {
        // ipv4 support
        if (ptr->ai_family == AF_INET)
        {
            char ipbuf[16];
            sockaddr_in * addr = reinterpret_cast<sockaddr_in *>( ptr->ai_addr);
            result.emplace_back(inet_ntop(AF_INET, &addr->sin_addr, ipbuf, 16));
        }
        // ipv6 support
        else if (ptr->ai_family == AF_INET6)
        {
            char ip6buf[64];
            sockaddr_in6 * addr6 =  reinterpret_cast<sockaddr_in6 *>( ptr->ai_addr);
            result.emplace_back(inet_ntop(AF_INET6, &addr6->sin6_addr, ip6buf, 64));
        }
        else
            throw DNSResolveException(DNS_E_GETADDRINFOFAIL, "unsupported AI Family");
    }
    freeaddrinfo(res);
    return result;
}

std::string DNSResolver::inAddrToStr(const in_addr_t & ip)
{
    char ip_str[INET_ADDRSTRLEN];
    inet_ntop(AF_INET, &ip, ip_str, INET_ADDRSTRLEN);
    return std::string(ip_str);
}

in_addr_t DNSResolver::inStrToAddr(const std::string & host)
{
    return inet_addr(host.c_str());
}

}
