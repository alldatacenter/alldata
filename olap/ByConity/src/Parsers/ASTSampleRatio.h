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

#include <Parsers/IAST.h>


namespace DB
{

/** Sampling factor in the form 0.1 or 1/10.
  * It's important to save it as a rational number without converting it to IEEE-754.
  */
class ASTSampleRatio : public IAST
{
public:
    using BigNum = __uint128_t;    /// Must contain the result of multiplying two UInt64.

    struct Rational
    {
        BigNum numerator = 0;
        BigNum denominator = 1;
    };

    Rational ratio;

    explicit ASTSampleRatio(const Rational & ratio_) : ratio(ratio_) {}

    String getID(char delim) const override { return "SampleRatio" + (delim + toString(ratio)); }

    ASTType getType() const override { return ASTType::ASTSampleRatio; }

    ASTPtr clone() const override { return std::make_shared<ASTSampleRatio>(*this); }

    static String toString(BigNum num);
    static String toString(Rational ratio);

    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

    void serialize(WriteBuffer & buf) const override;
    static ASTPtr deserialize(ReadBuffer & buf);
};

}
