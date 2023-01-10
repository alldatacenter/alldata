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


#include <Catalog/Catalog.h>
// #include <MergeTreeCommon/CnchServerClient.h>
#include <Common/Exception.h>
#include <Common/HostWithPorts.h>
#include <Interpreters/Context.h>
#include <Transaction/TxnTimestamp.h>

#include <Transaction/ICnchTransaction.h>


namespace DB
{
/// ProxyTransaction - A proxy to work with transaction on other server.
/// - Alway `write` transaction (RO transaction can stand alone on any server w/o proxy)
/// - Not all method are supported. See comments in each the method detail information

class CnchProxyTransaction : public ICnchTransaction
{
    using Base = ICnchTransaction;

private:
    CnchServerClientPtr remote_client;

public:
    explicit CnchProxyTransaction(const ContextPtr & context_) : Base(context_) {}
    explicit CnchProxyTransaction(const ContextPtr & context_, CnchServerClientPtr client, const TxnTimestamp & primary_txn_id);
    ~CnchProxyTransaction() override = default;
    String getTxnType() const override { return "CnchProxyTransaction"; }
    void precommit() override;
    TxnTimestamp commit() override;
    TxnTimestamp commitV2() override;
    TxnTimestamp rollback() override;
    TxnTimestamp abort() override;
    void clean(TxnCleanTask & task) override;
    void removeIntermediateData() override;
    void syncTransactionStatus(bool throw_on_missmatch = false);
    void setTransactionStatus(CnchTransactionStatus status);
};

using ProxyTransactionPtr = std::shared_ptr<CnchProxyTransaction>;

}
