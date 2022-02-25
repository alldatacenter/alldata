/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import { dateHelper } from 'oozie-designer/helpers/date-helper';
import { module, test } from 'qunit';

module('Unit | Helper | date helper');

test('it works for invalid date string', function (assert) {
    let result = dateHelper("InvalidDateString");
    assert.equal(result, "");
});

test('it works for valid date string', function (assert) {
    let result = dateHelper(new Date().toString());
    assert.equal(result, "a few seconds ago");
});

test('it works for yesterday', function (assert) {
    let today = new Date();
    let yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
    let result = dateHelper(yesterday);
    assert.equal(result, "a day ago");
});
