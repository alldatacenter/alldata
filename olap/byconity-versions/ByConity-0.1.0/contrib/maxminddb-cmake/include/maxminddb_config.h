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

#ifndef MAXMINDDB_CONFIG_H
#define MAXMINDDB_CONFIG_H

#ifndef MMDB_UINT128_USING_MODE
/* Define as 1 if we use unsigned int __atribute__ ((__mode__(TI))) for uint128 values */
/* #undef MMDB_UINT128_USING_MODE */
#endif

#ifndef MMDB_UINT128_IS_BYTE_ARRAY
/* Define as 1 if we don't have an unsigned __int128 type */
/* #undef MMDB_UINT128_IS_BYTE_ARRAY */
#endif

#endif                          /* MAXMINDDB_CONFIG_H */
