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

#include <Common/PODArray.h>
#include <Common/Exception.h>
#include <Compression/CompressedReadBuffer.h>
#include <Compression/CompressedReadBufferFromFile.h>
#include <Compression/CompressedWriteBuffer.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnVector.h>
#include <DataTypes/DataTypeArray.h>
#include <IO/ReadBufferFromFile.h>
#include <IO/WriteBufferFromFile.h>
#include <IO/HashingWriteBuffer.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <Storages/MergeTree/MergeTreeData.h>

#include <sstream>
#include <roaring.hh>
#include <common/logger_useful.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Common/ThreadPool.h>

namespace DB
{
/** Macro that guarantees a Boolean value results in a 1 for true and 0 for false
 *
 * Useful shorthand to indicate a Boolean result like (a>b) results in 1 or 0 without using a
 * forbidden questionmark operator which has very bad optimization results.
 * Usually Boolean is defined as being 1 or 0 so no alteration is needed; however,
 * this macro allows clarity on code which makes this assumption and a way to add active
 * logic for processors/compilers where this is not the case.
 */
#define BOOL_TO_BIT(exp) (exp)

class BitMap : public Roaring
{
public:
    // TODO: add BitmapIndex specific methods
    void deserialize(ReadBuffer& istr)
    {
        size_t size_in_bytes = 0;
        readIntBinary(size_in_bytes, istr);

        PODArray<char> buffer(size_in_bytes);

        istr.read(buffer.data(), size_in_bytes);

        std::unique_ptr<roaring_bitmap_t>  r(roaring::api::roaring_bitmap_portable_deserialize_safe(buffer.data(), size_in_bytes));
        if (!r)
        {
            throw Exception("failed alloc while roaring bitmap reading", ErrorCodes::CANNOT_ALLOCATE_MEMORY);
        }

        roaring = std::move(*r);
    }

    //TBD: write length or capacity
    void serialize(WriteBuffer& ostr) const
    {
        size_t expected_size_in_bytes = getSizeInBytes();
        PODArray<char> buffer(expected_size_in_bytes);
        // may save space by setting portable flag to false
        size_t size_in_bytes = this->write(buffer.data());
        //TODO: avoid allocating buffer above, clone logic from roaring_bitmap_serialize
        writeIntBinary(size_in_bytes, ostr);
        ostr.write(buffer.data(), size_in_bytes);
    }

    inline void set(size_t x)
    {
        add(x);
    }
};

/**
 * map AB vid to MSN (mark + offset_in_mark),
 * Using BitMap by default (phase 1)
 */

class IListIndex
{
    size_t total_rows = 0;
    BitMap mIndexData;
public:
    virtual ~IListIndex() = default;
    virtual void addMSN(size_t msn) { mIndexData.set(msn); }
    virtual size_t size() { return mIndexData.cardinality(); }
    virtual const BitMap& getIndex() const {return mIndexData;}
    virtual BitMap& getIndex() {return mIndexData;}
    virtual void setIndex(const BitMap & bitmap) { mIndexData = bitmap; }
    virtual size_t getOriginalRows() const { return total_rows; }
    virtual void setOriginalRows(const size_t & rows) { total_rows = rows; }
    virtual void addRows(const size_t & rows) { total_rows += rows; }
};

template <typename VIDTYPE = Int32>
class ListIndex : public IListIndex
{
    VIDTYPE vid;
public:
    ListIndex() {}
    ListIndex(VIDTYPE vid_) : vid(vid_) {}

    VIDTYPE getVid() const {return vid;}
    void setVid(const VIDTYPE & vid_) { vid = vid_; }
};

using BitmapIndexPtr = std::shared_ptr<IListIndex>;
using WriteBufferFromFilePtr = std::unique_ptr<WriteBufferFromFile>;
using ReadBufferFromFilePtr = std::unique_ptr<ReadBufferFromFile>;
using CompressedWriteBufferPtr = std::unique_ptr<CompressedWriteBuffer>;
using HashingWriteBufferPtr = std::unique_ptr<HashingWriteBuffer>;
/**
 *  Writer that spill ListIndex for VIDs, the layout is:
 *  - abindex.irk
 *  - abindex.idx
 *  The abindex.irk will track per vid's index in idx file,
 *  abindex.idx is the spilled list index (appeded by vids).
 *
 *  Spilled format looks as below:
 *  [vid, format(all, sparse, bits), raw_index_data]
 */

enum BitmapIndexMode {
    ROW = 0,
    MARK
};

class BitmapIndexWriter
{
private:
    WriteBufferFromFilePtr idx;
    HashingWriteBufferPtr hash_idx;
    CompressedWriteBufferPtr compressed_idx;
    HashingWriteBufferPtr hash_compressed;
    WriteBufferFromFilePtr irk;
    HashingWriteBufferPtr hash_irk;
    size_t total_rows;
    bool enable_run_optimization;

    BitmapIndexMode bitmap_index_mode;
    std::vector<String> adx_suffix_vec = {AB_IDX_EXTENSION, MARK_BITMAP_IDX_EXTENSION};
    std::vector<String> ark_suffix_vec = {AB_IRK_EXTENSION, MARK_BITMAP_IRK_EXTENSION};

public:
    // Initialize writers
    BitmapIndexWriter(String path, String name, size_t rows, BitmapIndexMode bitmap_index_mode_, const bool & enable_run_optimization_ = false);
    BitmapIndexWriter(String path, size_t rows, BitmapIndexMode bitmap_index_mode_, const bool & enable_run_optimization_ = false);

    void writeRows() { writeIntBinary(total_rows, *hash_irk); }
    template <typename VIDTYPE> void serialize(IListIndex & li);
    void addToChecksums(MergeTreeData::DataPart::Checksums & checksums, const String & column_name);
};


class BitmapIndexReader
{
private:
    String path;
    String column_name;
    BitmapIndexMode bitmap_index_mode ;
    std::unique_ptr<CompressedReadBufferFromFile> compressed_idx;
    ReadBufferFromFilePtr irk;

    std::vector<String> adx_suffix_vec = {AB_IDX_EXTENSION, MARK_BITMAP_IDX_EXTENSION};
    std::vector<String> ark_suffix_vec = {AB_IRK_EXTENSION, MARK_BITMAP_IRK_EXTENSION};
public:
    BitmapIndexReader(String path, String name, BitmapIndexMode bitmap_index_mode_);
    ~BitmapIndexReader() = default;
    // seek based on irk and read idx
    template <typename VIDTYPE> bool deserialize(VIDTYPE vid, IListIndex& li);
    template <typename VIDTYPE, typename Method> bool deserializeVids(Method & vids, std::vector<BitmapIndexPtr> & indexes, size_t total_vid_cnt);
    void init();
    bool valid() { return (compressed_idx && irk); }
};

class IBitmapColumnListIndexes
{
protected:
    String path;
    String colname; // optional
    //size_t offset; // record previos domain if multiple blocks are built(merge case)
    //size_t total_rows;
    bool enable_run_optimization;
    std::unique_ptr<BitmapIndexWriter> bitmap_index_writer;
public:
    IBitmapColumnListIndexes(const String & path_, const String & colname_, const bool & enable_run_optimization_ = false)
        : path(path_), colname(colname_), /*offset(0), total_rows(0),*/ enable_run_optimization(enable_run_optimization_){}

    virtual void asyncAppendColumnData(ColumnPtr col) = 0;

    virtual void serialize(BitmapIndexWriter & ) = 0;

    virtual void deserialize(BitmapIndexReader &) = 0;

    virtual void finalize() = 0;

    virtual void addToChecksums(MergeTreeData::DataPart::Checksums & checksums) = 0;

    virtual const String getPath() const { return path; }

    Poco::Logger * log = &Poco::Logger::get("BitmapColumnListIndexes");

    virtual ~IBitmapColumnListIndexes() {}
};

struct BitmapBuildTask
{
    BitmapBuildTask() {}
    size_t start_offset = 0;
};


template <typename VIDTYPE = Int32>
struct BitmapBuildTaskHolder
{
    using ColumnIndexes = std::map<VIDTYPE, ListIndex<VIDTYPE>>;

    std::condition_variable cond;
    std::mutex mtx;
    ColumnIndexes final_indexes;

    // global_offset is used for recording the current offset during insert blocks.
    // Since we will use multiple threads for building bitmap, the current offset of each thread should
    // assigned uniformly.
    size_t global_offset = 0;
    size_t total_rows = 0;

    std::vector<std::shared_ptr<BitmapBuildTask>> build_tasks;
    std::queue<std::shared_ptr<BitmapBuildTask>> free_tasks;
    size_t max_size = 0;
    BitmapBuildTaskHolder(const size_t max_size_)
        : max_size(max_size_)
    {
        //build_tasks = std::vector<std::shared_ptr<BuildTask>>(max_size, std::make_shared<BuildTask>());
        for (size_t i = 0; i < max_size; ++i)
            build_tasks.push_back(std::make_shared<BitmapBuildTask>());

        for (auto task : build_tasks)
        {
            free_tasks.push(task);
        }

        if (max_size == 0)
            throw Exception("Initialize BuildTaskHolder with max_size = 0", ErrorCodes::LOGICAL_ERROR);
    }

    // consume a task from free_tasks
    std::shared_ptr<BitmapBuildTask> getTask(ColumnPtr col)
    {
        std::unique_lock<std::mutex> lock(mtx);
        cond.wait(lock, [this](){
            return !free_tasks.empty();
        });

        std::shared_ptr<BitmapBuildTask> ret_task = free_tasks.front();
        ret_task->start_offset = global_offset;
        free_tasks.pop();

        global_offset += col->size();
        total_rows += col->size();

        return ret_task;
    }

    void addTask(std::shared_ptr<BitmapBuildTask> task)
    {
        std::unique_lock<std::mutex> lock(mtx);
        cond.wait(lock, [this](){
            return free_tasks.size() < max_size;
        });

        free_tasks.push(task);
        cond.notify_all();
    }

    void commitBitmap(ColumnIndexes & column_indexes)
    {
        std::unique_lock<std::mutex> lock(mtx);
        for (auto it = column_indexes.begin(); it != column_indexes.end(); ++it)
        {
            const VIDTYPE & vid = it->first;
            if (final_indexes.find(vid) == final_indexes.end())
                final_indexes.insert({vid, ListIndex<VIDTYPE>(vid)});
            auto & bit_map = final_indexes[vid].getIndex();
            bit_map |= it->second.getIndex();
        }
    }
};


template <typename VIDTYPE = Int32>
class BitmapColumnListIndexes : public IBitmapColumnListIndexes
{
    using ColumnIndexes = std::map<VIDTYPE, ListIndex<VIDTYPE>>;

    std::shared_ptr<BitmapBuildTaskHolder<VIDTYPE>> build_tasks_holder;
    std::unique_ptr<ThreadPool> thread_pool;
    ThreadGroupStatusPtr thread_group;
    size_t index_granularity;
    BitmapIndexMode bitmap_index_mode;

public:
    BitmapColumnListIndexes(const String& path_, const String & colname_, const bool enable_run_optimization_ = false, const size_t max_parallel_threads = 1,
                            size_t index_granularity_ = 8192, BitmapIndexMode bitmap_index_mode_ = BitmapIndexMode::ROW) :
        IBitmapColumnListIndexes(path_, colname_, enable_run_optimization_), index_granularity(index_granularity_), bitmap_index_mode(bitmap_index_mode_)
    {
        build_tasks_holder = std::make_shared<BitmapBuildTaskHolder<VIDTYPE>>(max_parallel_threads);
        thread_pool = std::make_unique<ThreadPool>(build_tasks_holder->max_size);
        //thread_pool = std::make_unique<FreeThreadPool>(build_tasks_holder->max_size);
        thread_group = CurrentThread::getGroup();
        if (!thread_group)
        {
            CurrentThread::initializeQuery();
            thread_group = CurrentThread::getGroup();
        }
    }

    void appendColumnData(ColumnPtr col, std::shared_ptr<BitmapBuildTask> task);

    void asyncAppendColumnData(ColumnPtr col) override;

    String dumpDebugInfo() const;

    void serialize(BitmapIndexWriter & ) override;

    // for debug only
    void deserialize(BitmapIndexReader &) override;

    void addToChecksums(MergeTreeData::DataPart::Checksums & checksums) override;

    void finalize() override;
};

template<typename VIDTYPE>
void construct_column_indexes(std::map<VIDTYPE, ListIndex<VIDTYPE>> & column_indexes, size_t offset, [[maybe_unused]]const ColumnVector<VIDTYPE> * col, BitmapIndexMode bitmap_index_mode, size_t index_granularity)
{
    if constexpr (std::is_same_v<VIDTYPE, UInt8>  || std::is_same_v<VIDTYPE, UInt16> || std::is_same_v<VIDTYPE, UInt32> || std::is_same_v<VIDTYPE, UInt64>
                  || std::is_same_v<VIDTYPE, UInt128> || std::is_same_v<VIDTYPE, Int8> || std::is_same_v<VIDTYPE, Int16> || std::is_same_v<VIDTYPE, Int32>
                  || std::is_same_v<VIDTYPE, Int64>  || std::is_same_v<VIDTYPE, Float32> || std::is_same_v<VIDTYPE, Float64>)
    {
        size_t numRows = col->size();
        const auto & dataCol = col->getData();
        for (size_t i = 0; i<numRows; i++)
        {
            const VIDTYPE & vid = dataCol[i];
            auto it = column_indexes.find(vid);
            if (it == column_indexes.end())
                column_indexes.insert({vid, ListIndex<VIDTYPE>(vid)});
            auto & bitmap = column_indexes[vid].getIndex();
            if (bitmap_index_mode == BitmapIndexMode::ROW)
                bitmap.set(offset + i);
            else if (bitmap_index_mode == BitmapIndexMode::MARK)
                bitmap.set((offset + i)/index_granularity);
            else
                throw Exception("bitmap index mode not support: ", ErrorCodes::LOGICAL_ERROR);
        }
    }
}


template<typename VIDTYPE>
void construct_column_indexes(std::map<VIDTYPE, ListIndex<VIDTYPE>> & column_indexes, size_t offset, [[maybe_unused]]const ColumnString * col, BitmapIndexMode bitmap_index_mode, size_t index_granularity)
{
    if constexpr (std::is_same_v<VIDTYPE, String>)
    {
        size_t numRows = col->size();
        for (size_t i = 0; i<numRows; i++)
        {
            const VIDTYPE & vid = col->getDataAt(i).toString();
            auto it = column_indexes.find(vid);
            if (it == column_indexes.end())
                column_indexes.insert({vid, ListIndex<VIDTYPE>(vid)});
            auto & bitmap = column_indexes[vid].getIndex();
            if (bitmap_index_mode == BitmapIndexMode::ROW)
                bitmap.set(offset + i);
            else if (bitmap_index_mode == BitmapIndexMode::MARK)
                bitmap.set((offset + i)/index_granularity);
            else
                throw Exception("bitmap index mode not support: ", ErrorCodes::LOGICAL_ERROR);
        }
    }
}


template<typename VIDTYPE>
void construct_column_indexes(std::map<VIDTYPE, ListIndex<VIDTYPE>> & column_indexes, size_t offset, const ColumnArray * col, BitmapIndexMode bitmap_index_mode, size_t index_granularity)
{
    size_t numRows = col->size();
    const auto & inputOffset = col->getOffsets();
    const auto & dataCol = col->getData();

    if constexpr (std::is_same<VIDTYPE, String>::value)
    {
        const auto * data_string = static_cast<const ColumnString *>(&dataCol);
        if (!data_string)
            return;
        size_t prePos = 0;
        for (size_t i = 0; i<numRows; i++)
        {
            size_t endPos = inputOffset[i];
            for (size_t j = prePos; j < endPos; j++)
            {
                const String & vid = data_string->getDataAt(j).toString();
                auto it = column_indexes.find(vid);
                if (it == column_indexes.end())
                    column_indexes.insert({vid, ListIndex<String>(vid)});
                auto & bitmap = column_indexes[vid].getIndex();
                if (bitmap_index_mode == BitmapIndexMode::ROW)
                    bitmap.set(offset + i);
                else if (bitmap_index_mode == BitmapIndexMode::MARK)
                    bitmap.set((offset + i)/index_granularity);
                else
                    throw Exception("bitmap index mode not support: ", ErrorCodes::LOGICAL_ERROR);
            }

            prePos = endPos;
        }
    }
    else
    {
        const auto * data_numbers = static_cast<const ColumnVector<VIDTYPE> *>(&dataCol);
        if (!data_numbers)
            return;
        const auto & dataColVec = data_numbers->getData();
        //LOG_DEBUG(&Logger::get("appendColumnData"), "use bitmap index id : "<< std::to_string(task->id));
        size_t prePos = 0;
        for (size_t i = 0; i<numRows; i++)
        {
            // [prePos, offsets[i])
            size_t endPos = inputOffset[i];
            for (size_t j = prePos; j < endPos; j++)
            {
                const VIDTYPE & vid = dataColVec[j];
                auto it = column_indexes.find(vid);
                if (it == column_indexes.end())
                    column_indexes.insert({vid, ListIndex<VIDTYPE>(vid)});
                auto & bitmap = column_indexes[vid].getIndex();
                if (bitmap_index_mode == BitmapIndexMode::ROW)
                    bitmap.set(offset + i);
                else if (bitmap_index_mode == BitmapIndexMode::MARK)
                    bitmap.set((offset + i)/index_granularity);
                else
                    throw Exception("bitmap index mode not support: ", ErrorCodes::LOGICAL_ERROR);
            }

            prePos = endPos;
        }
    }
}

/**
 * Build Bitmap column's bitmap index based on input block, resize the bitmap in
 * case multiple blocks are used to form a part(merge scenario)
 */
template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::appendColumnData(ColumnPtr col, std::shared_ptr<BitmapBuildTask> task)
{
    // read data row by row
    ColumnIndexes column_indexes;
    size_t offset = task->start_offset;
    if (dynamic_cast<const ColumnArray *>(col.get()))
        construct_column_indexes(column_indexes, offset, dynamic_cast<const ColumnArray *>(col.get()), bitmap_index_mode, index_granularity);
    else if (dynamic_cast<const ColumnString *>(col.get()))
        construct_column_indexes(column_indexes, offset, dynamic_cast<const ColumnString *>(col.get()), bitmap_index_mode, index_granularity);
    else if (dynamic_cast<const ColumnVector<VIDTYPE> *>(col.get()))
        construct_column_indexes(column_indexes, offset, dynamic_cast<const ColumnVector<VIDTYPE> *>(col.get()), bitmap_index_mode, index_granularity);
    else
        throw Exception("Bitmap column " + colname + " type is wrong",  ErrorCodes::LOGICAL_ERROR);

    // add local bitmap to global bitmap
    build_tasks_holder->commitBitmap(column_indexes);
    // add finished task to free_tasks
    build_tasks_holder->addTask(task);
}

template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::asyncAppendColumnData(ColumnPtr col)
{
    auto task = build_tasks_holder->getTask(col);

    auto run_job = [=, this](){
        //DB::ThreadStatus thread_status;
        CurrentThread::attachToIfDetached(thread_group);
        appendColumnData(col, task);
    };

    thread_pool->scheduleOrThrowOnError(run_job);
}

template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::serialize(BitmapIndexWriter & bitmapWriter)
{
    // Write total_rows of a part into irk
    bitmapWriter.writeRows();

    for (auto& vid_index : build_tasks_holder->final_indexes)
    {
        bitmapWriter.serialize<VIDTYPE>(vid_index.second);
    }
}

template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::deserialize(BitmapIndexReader & )
{
    throw Exception("not implemented!", ErrorCodes::LOGICAL_ERROR);
}

template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::finalize()
{
    thread_pool->wait();
    bitmap_index_writer = std::make_unique<BitmapIndexWriter>(getPath(), build_tasks_holder->total_rows, bitmap_index_mode, enable_run_optimization);
    serialize(*bitmap_index_writer);
}

template <typename VIDTYPE>
void BitmapColumnListIndexes<VIDTYPE>::addToChecksums(MergeTreeData::DataPart::Checksums & checksums)
{
    if (bitmap_index_writer)
        bitmap_index_writer->addToChecksums(checksums, colname);
}

template <typename VIDTYPE>
void BitmapIndexWriter::serialize(IListIndex & li)
{
    ListIndex<VIDTYPE> * list_index = static_cast<ListIndex<VIDTYPE>*>(&li);
    if (!list_index)
        return;

    // 1. write mark in irk
    auto & bitmap = li.getIndex();
    // // TODO optimize it without syncing
    hash_compressed->next();
    size_t compressed_offset = hash_idx->count();
    size_t uncompressed_offset = hash_compressed->offset();

    if constexpr (std::is_same<VIDTYPE, String>::value)
        writeStringBinary(list_index->getVid(), *hash_irk);
        // backward compatible
        // Since the first version of bitmap uses Int64 as vids type when it write bitmap of int index
        // We try to cast int to Int64 to be compatible with old data
    else if constexpr (std::is_same<VIDTYPE, Int32>::value)
        writePODBinary(static_cast<Int64>(list_index->getVid()), *hash_irk);
    else
        writePODBinary(list_index->getVid(), *hash_irk);
    writeIntBinary(compressed_offset, *hash_irk);
    writeIntBinary(uncompressed_offset, *hash_irk);

    if (enable_run_optimization)
    {
        bitmap.shrinkToFit();
        bitmap.runOptimize();
    }
    bitmap.serialize(*hash_compressed);
}

// Support one BitmapIndexRead for one vid, otherwise need to seek backward in
// iteration
template <typename VIDTYPE>
bool BitmapIndexReader::deserialize(VIDTYPE vid, IListIndex& li)
{
    // files are compaction of all vids in this part
    // STEP 1: locate the range belong to vid based on irk(index mark)
    [[maybe_unused]] off_t compressed_offset = 0, uncompressed_offset = 0;
    VIDTYPE tmpVid;
    size_t total_rows = 0;
    bool vidFound = false;

    if (!compressed_idx || !irk)
        throw Exception("Cannot deserialize bitmap index since there is no inputstream", ErrorCodes::LOGICAL_ERROR);

    compressed_idx->seek(0,0);
    irk->seek(0, SEEK_SET);

    if (!irk->eof())
        readIntBinary(total_rows, *irk);

    while(!irk->eof())
    {
        if constexpr (std::is_same<VIDTYPE, String>::value)
            readStringBinary(tmpVid, *irk);
            // backward compatible
            // try to read vid of type Int64 instead of template types
            // since the old version has written vids in type `Int64`
            // We only deal with `int` type because only `int` type was used
        else if constexpr (std::is_same<VIDTYPE, Int32>::value)
        {
            Int64 backward_compatible_vid;
            readPODBinary(backward_compatible_vid, *irk);
            tmpVid = backward_compatible_vid;
        }
        else
            readPODBinary(tmpVid, *irk);
        readIntBinary(compressed_offset, *irk);
        readIntBinary(uncompressed_offset, *irk);
        //std::cout<<"vid: "<<vid<<" tmpVid: "<<tmpVid<<" ===>total_rows: "<<total_rows<<std::endl;
        if (tmpVid != vid)
        {
            continue;
        }
        else
        {
            vidFound = true;
            // go this vid end pos
            break;
        }
    }

    li.setOriginalRows(total_rows);
    // what happens if vid not found in this part
    if (!vidFound) return false;
    // Range [vidoffset, offset] in idx are data for this vid, if this vid is
    // the first one, vidoffset is initialized as 0, and [0, offset] is expected

    // STEP 2: get bitmap based on range got in STEP 1 from idx(index data)
    compressed_idx->seek(compressed_offset, uncompressed_offset);
    // TODO: add assertion here that idx file is not corrupted
    li.getIndex().deserialize(*compressed_idx);


    return vidFound;
}

template <typename VIDTYPE, typename Method>
bool BitmapIndexReader::deserializeVids(Method & vids, std::vector<BitmapIndexPtr> & indexes, size_t total_vid_cnt)
{
    [[maybe_unused]] off_t compressed_offset = 0, uncompressed_offset = 0;
    VIDTYPE tmpVid;
    size_t total_rows = 0;
    bool vidFound = false;

    if (!compressed_idx || !irk)
        throw Exception("Cannot deserialize bitmap index since there is no inputstream", ErrorCodes::LOGICAL_ERROR);

    compressed_idx->seek(0,0);
    irk->seek(0, SEEK_SET);

    if (!irk->eof())
        readIntBinary(total_rows, *irk);

    size_t vid_cnt = 0;

    while(!irk->eof())
    {
        if constexpr (std::is_same<VIDTYPE, String>::value)
            readStringBinary(tmpVid, *irk);
            // backward compatible
            // try to read vid of type Int64 instead of template types
            // since the old version has written vids in type `Int64`
            // We only deal with `int` type because only `int` type was used
        else if constexpr (std::is_same<VIDTYPE, Int32>::value)
        {
            Int64 backward_compatible_vid;
            readPODBinary(backward_compatible_vid, *irk);
            tmpVid = backward_compatible_vid;
        }
        else
            readPODBinary(tmpVid, *irk);
        readIntBinary(compressed_offset, *irk);
        readIntBinary(uncompressed_offset, *irk);
        //std::cout<<"vid: "<<vid<<" tmpVid: "<<tmpVid<<" ===>total_rows: "<<total_rows<<std::endl;
        if (!vids.data.has(tmpVid))
        {
            continue;
        }
        else
        {
            vidFound = true;
            indexes.emplace_back(std::make_shared<IListIndex>());
            auto & temp_index = indexes.back();
            temp_index->setOriginalRows(total_rows);
            compressed_idx->seek(compressed_offset, uncompressed_offset);
            temp_index->getIndex().deserialize(*compressed_idx);

            if (++vid_cnt == total_vid_cnt)
                break;
        }
    }
    return vidFound;
}


}
