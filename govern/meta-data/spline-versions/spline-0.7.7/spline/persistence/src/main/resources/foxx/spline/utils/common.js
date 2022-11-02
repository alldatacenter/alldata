/*
 * Copyright 2020 ABSA Group Limited
 *
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

"use strict";

/**
 * Returns a memoized function that is based on two provided ones - the key and value functions respectively.
 * The signature of the provided functions must be identical, and the resulting memoized function will have the same signature.
 * When the resulting function is invoked the key function is first called to get the caching key. The value function is only
 * called when there's no previously cached value for the key. Otherwise the cached value is returned.
 * @param keyFn a key function
 * @param valFn a value function
 * @returns memoized function with the same signature as _keyFn_ and _valueFn_
 */
function memoize(keyFn, valFn) {
    const cache = new Map();
    return function () {
        const key = keyFn.apply(this, arguments);
        if (cache.has(key)) {
            return cache.get(key);
        } else {
            const value = valFn.apply(this, arguments);
            cache.set(key, value);
            return value;
        }
    }
}

module.exports = {
    memoize,
}
