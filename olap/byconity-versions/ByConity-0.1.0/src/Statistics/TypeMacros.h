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

#define INTEGER_TYPE_ITERATE(X_) \
    X_(Int8) \
    X_(Int16) \
    X_(Int32) \
    X_(Int64) \
    X_(UInt8) \
    X_(UInt16) \
    X_(UInt32) \
    X_(UInt64) \
    X_(UInt128) \
    X_(UInt256) \
    X_(Int128) \
    X_(Int256)

#define FLOATING_TYPE_ITERATE(X_) \
    X_(Float64) \
    X_(Float32)

#define SPECIAL_TYPE_ITERATE(X_) \
    /* other types */ \
    X_(String)

#define FIXED_TYPE_ITERATE(X_) \
    INTEGER_TYPE_ITERATE(X_) \
    FLOATING_TYPE_ITERATE(X_)

#define ALL_TYPE_ITERATE(X_) \
    INTEGER_TYPE_ITERATE(X_) \
    FLOATING_TYPE_ITERATE(X_) \
    SPECIAL_TYPE_ITERATE(X_)

#define TYPED_STATS_ITERATE(X_) \
    X_(KllSketch) \
    X_(NdvBuckets) \
    X_(NdvBucketsExtend) \
    X_(NdvBucketsResult)

#define UNTYPED_STATS_ITERATE(X_) \
    X_(DummyAlpha) \
    X_(DummyBeta) \
    X_(TableBasic) \
    X_(ColumnBasic) \
    X_(CpcSketch)
