/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

INSERT INTO `user` (`id`, `name`, `password`, `account_type`, `due_date`, `creator`, `modifier`)
VALUES (1, 'admin', '1976e096b31cfda81269d0df2775466aac6dd809e3ada1d5ba7831d85e80f109',
        0, '2099-12-31 23:59:59', 'inlong_init', 'inlong_init');

INSERT INTO `user` (`id`, `name`, `password`, `account_type`, `due_date`, `creator`, `modifier`)
VALUES (2, 'operator', '1976e096b31cfda81269d0df2775466aac6dd809e3ada1d5ba7831d85e80f109',
        1, '2099-12-31 23:59:59', 'inlong_init', 'inlong_init');
