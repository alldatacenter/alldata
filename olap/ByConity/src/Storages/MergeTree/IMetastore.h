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

#include <Core/Types.h>
#include <memory>

namespace DB
{

struct BatchWriteRequest
{
    std::vector<std::string> delete_req;
    std::vector<std::pair<std::string, std::string>> put_req;
};

/***
 * Base class for physical storage of MergeTree metainfo. It defines the a set of interfaces between engine
 * and metastore. Users can choose different database to store metainfo, eg: LevelDB, RocksDB.
 */
class IMetaStore
{
public:
    /***
     * Iterator to go over a set of record in metastore.
     */
    struct Iterator{
        virtual ~Iterator() {}
        virtual bool hasNext() = 0;
        virtual void next() = 0;
        virtual String key() = 0;
        virtual String value() = 0;
    };

    using IteratorPtr = std::shared_ptr<Iterator>;
    IMetaStore(const String & _path): db_path(_path) {}
    virtual ~IMetaStore() {}

    /***
     * Save a record into metastore;
     */
    virtual void put(const String &, const String &) = 0;

    /***
     * Put and delete keys in one batch atomically.
     */
    virtual void multiWrite(const BatchWriteRequest &) = 0;

    /***
     * Get a record by name from metastore;
     */
    virtual void get(const String &, String &) = 0;

    /***
     * Get a set of records by their names from metastore;
     */
    virtual std::vector<String> multiGet(std::vector<String>&) = 0;

    /***
     * Update a specific record in metastore;
     */
    virtual void update(const String&, const String&) = 0;

    /***
     * Delete a specific record from metastore;
     */
    virtual void drop(const String &) = 0;

    /***
     * Scan all records from the specific key. If the key is empty, will go over all records in current DB.
     */
    virtual IteratorPtr scan(const String &) = 0;

    /***
     * Clear all metainfo in the metastore;
     */
    virtual void clean() = 0;

    /***
     * Close metastore;
     */
     virtual void close() = 0;

     /***
      * Make sure the metastore is initialized
      */
     virtual void startUp() = 0;


protected:

    const String & db_path;
};

}
