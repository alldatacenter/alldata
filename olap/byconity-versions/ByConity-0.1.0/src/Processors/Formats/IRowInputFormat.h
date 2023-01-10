/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <string>
#include <Columns/IColumn.h>
#include <Processors/Formats/IInputFormat.h>
#include <DataStreams/SizeLimits.h>
#include <Poco/Timespan.h>
#include <Common/Stopwatch.h>
#include <Formats/FormatFactory.h>


namespace DB
{

/// Contains extra information about read data.
struct RowReadExtension
{
    /// IRowInputFormat::read output. It contains non zero for columns that actually read from the source and zero otherwise.
    /// It's used to attach defaults for partially filled rows.
    std::vector<UInt8> read_columns;
};

/// Common parameters for generating blocks.
struct RowInputFormatParams
{
    size_t max_block_size = 0;

    UInt64 allow_errors_num = 0;
    Float64 allow_errors_ratio = 0;

    Poco::Timespan max_execution_time = 0;
    OverflowMode timeout_overflow_mode = OverflowMode::THROW;
};

bool isParseError(int code);
bool checkTimeLimit(const RowInputFormatParams & params, const Stopwatch & stopwatch);

/// Row oriented input format: reads data row by row.
class IRowInputFormat : public IInputFormat
{
public:
    using Params = RowInputFormatParams;

    IRowInputFormat(Block header, ReadBuffer & in_, Params params_);

    Chunk generate() override;

    void resetParser() override;

    size_t getNumErrors() const { return num_errors; }
    size_t getErrorBytes() const { return error_bytes; }
    Exception getAndParseException()
    {
        auto res = parse_exception.value();
        parse_exception.reset();
        return res;
    }
    void setReadCallBack(const FormatFactory::ReadCallback call_back) { read_virtual_columns_callback = call_back; }
    void setCallbackOnError(const std::function<void(Exception &)> & on_error_) { on_error = on_error_; }


protected:
    /** Read next row and append it to the columns.
      * If no more rows - return false.
      */
    virtual bool readRow(MutableColumns & columns, RowReadExtension & extra) = 0;

    virtual void readPrefix() {}                /// delimiter before begin of result
    virtual void readSuffix() {}                /// delimiter after end of result

    /// Skip data until next row.
    /// This is intended for text streams, that allow skipping of errors.
    /// By default - throws not implemented exception.
    virtual bool allowSyncAfterError() const { return false; }
    virtual void syncAfterError();

    /// In case of parse error, try to roll back and parse last one or two rows very carefully
    ///  and collect as much as possible diagnostic information about error.
    /// If not implemented, returns empty string.
    virtual std::string getDiagnosticInfo() { return {}; }

    const BlockMissingValues & getMissingValues() const override { return block_missing_values; }

    size_t getTotalRows() const { return total_rows; }

    Serializations serializations;

private:
    Params params;

    size_t total_rows = 0;
    size_t num_errors = 0;
    size_t error_bytes = 0;
    std::optional<Exception> parse_exception;

    /// Callback used to setup virtual columns after reading each row.
    /// Only used by HaKafkaBlockInputStream, which is different from implementation logic of community version
    FormatFactory::ReadCallback read_virtual_columns_callback;
    std::function<void(Exception &)> on_error;

    BlockMissingValues block_missing_values;
};

}
