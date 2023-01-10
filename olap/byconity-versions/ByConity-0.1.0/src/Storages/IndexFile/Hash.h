// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */


#pragma once

#include <stddef.h>
#include <stdint.h>

namespace DB::IndexFile
{
// Simple hash function used for internal data structures
// FIXME replace with 64-bits xxhash
uint32_t Hash(const char * data, size_t n, uint32_t seed);

}
