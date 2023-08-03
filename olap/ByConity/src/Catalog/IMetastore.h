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

#include <memory>
#include <string_view>
#include <common/StringRef.h>
#include <Catalog/MetastoreCommon.h>

namespace DB
{

namespace Catalog
{

/***
* Interface for metastore implementation.
*/
class IMetaStore
{
public:
    /***
     * Iterator to go over a set of record in metastore.
     */
    struct Iterator{
        virtual ~Iterator() {}
        virtual bool next() = 0;
        virtual String key() = 0;
        virtual String value() = 0;
    };

    using IteratorPtr = std::shared_ptr<Iterator>;
    IMetaStore(){}
    virtual ~IMetaStore() {}

    /***
     * Save a record into metastore;
     */
    virtual void put(const String & key, const String & value, bool if_not_exists = false) = 0;

    /***
     * Put with CAS. Return true if CAS succeed, otherwise return false with current value.
     */
    virtual std::pair<bool, String> putCAS(const String & key, const String & value, const String & expected, bool with_old_value = false) = 0;
    /***
     * Get a record by name from metastore;
     */
    virtual uint64_t get(const String & key, String & value) = 0;

    /***
     * Get a set of records by their names from metastore;
     */
    virtual std::vector<std::pair<String, UInt64>> multiGet(const std::vector<String> & keys) = 0;

    /***
     * Commit record in batch. For both write and delete.
     */
    virtual bool batchWrite(const BatchCommitRequest & req, BatchCommitResponse & response) = 0;

    /***
     * Delete a specific record from metastore;
     */
    virtual void drop(const String & key, const String & expected = {}) = 0;

    /***
     * Get all records from metastore;
     */
    virtual IteratorPtr getAll() = 0;

    /***
     * Range scan by specific prefix; limit the number of result
     */
    virtual IteratorPtr getByPrefix(const String & key_prefix, const size_t & limit = 0, uint32_t scan_batch_size = DEFAULT_SCAN_BATCH_COUNT) = 0;

    /***
     * Scan a range of records by start and end key;
     */
    virtual IteratorPtr getByRange(const String & start_key, const String & end_key, const bool include_start, const bool include_end) = 0;

    /***
     * Claer all metainfo in the metastore start with specification prefix;
     */
    virtual void clean(const String & prefix) = 0;

    /***
     * Close metastore;
     */
    virtual void close() = 0;
};

}

}
