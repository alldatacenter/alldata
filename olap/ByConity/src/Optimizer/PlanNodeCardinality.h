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
#include <QueryPlan/PlanVisitor.h>

namespace DB
{
class PlanNodeCardinality
{
public:
    struct Range
    {
        Range(size_t lowerBound_, size_t upperBound_) : lowerBound(lowerBound_), upperBound(upperBound_) { }
        size_t lowerBound;
        size_t upperBound;
    };

    static bool isScalar(PlanNodeBase & node) { return isScalar(extractCardinality(node)); }
    static bool isEmpty(PlanNodeBase & node) { return isEmpty(extractCardinality(node)); }
    static bool isAtMost(PlanNodeBase & node, size_t maxCardinality) { return extractCardinality(node).upperBound < maxCardinality; }
    static bool isAtLeast(PlanNodeBase & node, size_t minCardinality) { return extractCardinality(node).lowerBound > minCardinality; }
    static Range extractCardinality(PlanNodeBase & node);

private:
    static inline bool isScalar(const Range & range) { return range.lowerBound == 1 && range.upperBound == 1; }
    static inline bool isEmpty(const Range & range) { return range.lowerBound == 0 && range.upperBound == 0; }
    static Range intersection(const Range & range, const Range & other)
    {
        return Range{std::max(range.lowerBound, other.lowerBound), std::min(range.lowerBound, other.upperBound)};
    }

    class Visitor;
};

}
