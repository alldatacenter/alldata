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
#include <DataStreams/IBlockInputStream.h>
#include <Processors/Formats/IInputFormat.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

class IInputFormat;
using InputFormatPtr = std::shared_ptr<IInputFormat>;

class ParquetBlockInputStream : public IBlockInputStream
{
public:
    explicit ParquetBlockInputStream(InputFormatPtr input_format_)
        : input_format(std::move(input_format_)), port(input_format->getPort().getHeader(), input_format.get())
    {
        connect(input_format->getPort(), port);
        port.setNeeded();
    }

    String getName() const override { return input_format->getName(); }
    Block getHeader() const override { return input_format->getPort().getHeader(); }

    InputFormatPtr getInputFormatPtr() const { return input_format; }

    void cancel(bool kill) override
    {
        input_format->cancel();
        IBlockInputStream::cancel(kill);
    }

    const BlockMissingValues & getMissingValues() const override { return input_format->getMissingValues(); }

protected:
    Block readImpl() override
    {
        while (true)
        {
            auto status = input_format->prepare();

            switch (status)
            {
                case IProcessor::Status::Ready:
                    input_format->work();
                    break;

                case IProcessor::Status::Finished:
                    return {};

                case IProcessor::Status::PortFull:
                    return input_format->getPort().getHeader().cloneWithColumns(port.pull().detachColumns());

                case IProcessor::Status::NeedData:
                case IProcessor::Status::Async:
                case IProcessor::Status::ExpandPipeline:
                    throw Exception("Source processor returned status " + IProcessor::statusToName(status), ErrorCodes::LOGICAL_ERROR);
            }
        }
    }

private:
    InputFormatPtr input_format;
    InputPort port;
};

}
