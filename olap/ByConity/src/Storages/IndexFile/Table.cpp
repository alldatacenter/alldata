/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

#include <Storages/IndexFile/Table.h>

#include <Storages/IndexFile/Block.h>
#include <Storages/IndexFile/Cache.h>
#include <Storages/IndexFile/Comparator.h>
#include <Storages/IndexFile/Env.h>
#include <Storages/IndexFile/FilterBlock.h>
#include <Storages/IndexFile/FilterPolicy.h>
#include <Storages/IndexFile/Format.h>
#include <Storages/IndexFile/Options.h>
#include <Storages/IndexFile/SelectIterator.h>
#include <Storages/IndexFile/TwoLevelIterator.h>
#include <Common/Coding.h>

#include <memory>

namespace DB::IndexFile
{
struct Table::Rep
{
    ~Rep()
    {
        delete filter;
        delete[] filter_data;
        delete index_block;
    }

    Options options;
    Status status;
    std::unique_ptr<RandomAccessFile> file;
    uint64_t cache_id;
    FilterBlockReader * filter;
    const char * filter_data;
    size_t filter_size = 0;     /// total bytes of filter

    BlockHandle metaindex_handle; // Handle to metaindex_block: saved from footer
    Block * index_block;
};

Status Table::Open(const Options & options, std::unique_ptr<RandomAccessFile> && file, uint64_t size, std::unique_ptr<Table> * table)
{
    *table = nullptr;
    if (size < Footer::kEncodedLength)
    {
        return Status::Corruption("file is too short");
    }

    char footer_space[Footer::kEncodedLength];
    Slice footer_input;
    Status s = file->Read(size - Footer::kEncodedLength, Footer::kEncodedLength, &footer_input, footer_space);
    if (!s.ok())
        return s;

    Footer footer;
    s = footer.DecodeFrom(&footer_input);
    if (!s.ok())
        return s;

    // Read the index block
    BlockContents index_block_contents;
    ReadOptions opt;
    if (options.paranoid_checks)
    {
        opt.verify_checksums = true;
    }
    s = ReadBlock(file.get(), opt, footer.index_handle(), &index_block_contents);

    if (s.ok())
    {
        // We've successfully read the footer and the index block: we're
        // ready to serve requests.
        Block * index_block = new Block(index_block_contents);
        Rep * rep = new Table::Rep;
        rep->options = options;
        rep->file = std::move(file);
        rep->metaindex_handle = footer.metaindex_handle();
        rep->index_block = index_block;
        rep->cache_id = (options.block_cache ? options.block_cache->NewId() : 0);
        rep->filter_data = nullptr;
        rep->filter = nullptr;
        rep->filter_size = 0;
        (*table).reset(new Table(rep));
        (*table)->ReadMeta(footer);
    }

    return s;
}

void Table::ReadMeta(const Footer & footer)
{
    if (rep_->options.filter_policy == nullptr)
    {
        return; // Do not need any metadata
    }

    // TODO(sanjay): Skip this if footer.metaindex_handle() size indicates
    // it is an empty block.
    ReadOptions opt;
    if (rep_->options.paranoid_checks)
    {
        opt.verify_checksums = true;
    }
    BlockContents contents;
    if (!ReadBlock(rep_->file.get(), opt, footer.metaindex_handle(), &contents).ok())
    {
        // Do not propagate errors since meta info is not needed for operation
        return;
    }
    Block * meta = new Block(contents);

    Iterator * iter = meta->NewIterator(BytewiseComparator());
    std::string key = "filter.";
    key.append(rep_->options.filter_policy->Name());
    iter->Seek(key);
    if (iter->Valid() && iter->key() == Slice(key))
    {
        ReadFilter(iter->value());
    }
    delete iter;
    delete meta;
}

void Table::ReadFilter(const Slice & filter_handle_value)
{
    Slice v = filter_handle_value;
    BlockHandle filter_handle;
    if (!filter_handle.DecodeFrom(&v).ok())
    {
        return;
    }

    // We might want to unify with ReadBlock() if we start
    // requiring checksum verification in Table::Open.
    ReadOptions opt;
    if (rep_->options.paranoid_checks)
    {
        opt.verify_checksums = true;
    }
    BlockContents block;
    if (!ReadBlock(rep_->file.get(), opt, filter_handle, &block).ok())
    {
        return;
    }
    if (block.heap_allocated)
    {
        rep_->filter_data = block.data.data(); // Will need to delete later
        rep_->filter_size = block.data.size();
    }
    rep_->filter = new FilterBlockReader(rep_->options.filter_policy.get(), block.data);
}

Table::~Table()
{
    delete rep_;
}

static void DeleteBlock(void * arg, [[maybe_unused]] void * ignored)
{
    delete reinterpret_cast<Block *>(arg);
}

static void DeleteCachedBlock([[maybe_unused]] const Slice & key, void * value)
{
    Block * block = reinterpret_cast<Block *>(value);
    delete block;
}

static void ReleaseBlock(void * arg, void * h)
{
    auto * cache = reinterpret_cast<Cache *>(arg);
    auto * handle = reinterpret_cast<Cache::Handle *>(h);
    cache->Release(handle);
}

// Convert an index iterator value (i.e., an encoded BlockHandle)
// into an iterator over the contents of the corresponding block.
Iterator * Table::BlockReader(void * arg, const ReadOptions & options, const Slice & index_value)
{
    Table * table = reinterpret_cast<Table *>(arg);
    auto block_cache = table->rep_->options.block_cache;
    Block * block = nullptr;
    Cache::Handle * cache_handle = nullptr;

    BlockHandle handle;
    Slice input = index_value;
    Status s = handle.DecodeFrom(&input);
    // We intentionally allow extra stuff in index_value so that we
    // can add more features in the future.

    if (s.ok())
    {
        BlockContents contents;
        if (block_cache != nullptr)
        {
            char cache_key_buffer[16];
            EncodeFixed64(cache_key_buffer, table->rep_->cache_id);
            EncodeFixed64(cache_key_buffer + 8, handle.offset());
            Slice key(cache_key_buffer, sizeof(cache_key_buffer));
            cache_handle = block_cache->Lookup(key);
            if (cache_handle != nullptr)
            {
                block = reinterpret_cast<Block *>(block_cache->Value(cache_handle));
            }
            else
            {
                s = ReadBlock(table->rep_->file.get(), options, handle, &contents);
                if (s.ok())
                {
                    block = new Block(contents);
                    if (contents.cachable && options.fill_cache)
                    {
                        cache_handle = block_cache->Insert(key, block, block->size(), &DeleteCachedBlock);
                    }
                }
            }
        }
        else
        {
            s = ReadBlock(table->rep_->file.get(), options, handle, &contents);
            if (s.ok())
            {
                block = new Block(contents);
            }
        }
    }

    Iterator * iter;
    if (block != nullptr)
    {
        iter = block->NewIterator(table->rep_->options.comparator);
        if (options.select_predicate)
        {
            iter = NewSelectIterator(iter, options.select_predicate);
        }

        if (cache_handle == nullptr)
        {
            iter->RegisterCleanup(&DeleteBlock, block, nullptr);
        }
        else
        {
            iter->RegisterCleanup(&ReleaseBlock, block_cache.get(), cache_handle);
        }
    }
    else
    {
        iter = NewErrorIterator(s);
    }
    return iter;
}

Iterator * Table::NewIterator(const ReadOptions & options) const
{
    return NewTwoLevelIterator(
        rep_->options.comparator, rep_->index_block->NewIterator(rep_->options.comparator), &Table::BlockReader, const_cast<Table *>(this), options);
}

Status Table::Get(const ReadOptions & options, const Slice & k, std::string * value)
{
    Status notfound = Status::NotFound(Slice());

    /// first seek key in index block
    std::unique_ptr<Iterator> iiter(rep_->index_block->NewIterator(rep_->options.comparator));
    iiter->Seek(k);
    if (Status s = iiter->status(); !s.ok())
        return s;
    if (!iiter->Valid())
        return notfound;

    /// then check key in bloom filter when we have one
    Slice handle_value = iiter->value();
    FilterBlockReader * filter = rep_->filter;
    BlockHandle handle;
    if (filter != nullptr && handle.DecodeFrom(&handle_value).ok() && !filter->KeyMayMatch(handle.offset(), k))
        return notfound;

    /// finally seek key in data block
    std::unique_ptr<Iterator> block_iter(BlockReader(this, options, iiter->value()));
    block_iter->Seek(k);
    if (Status s = block_iter->status(); !s.ok())
        return s;
    if (!block_iter->Valid())
        return notfound;
    if (rep_->options.comparator->Compare(k, block_iter->key()) == 0)
    {
        Slice value_slice = block_iter->value();
        value->assign(value_slice.data(), value_slice.size());
        return Status::OK();
    }
    return notfound;
}

size_t Table::ResidentMemoryUsage() const
{
    return sizeof(Rep) + rep_->index_block->size() + rep_->filter_size;
}

}
