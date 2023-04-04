/**
 * Datart
 *
 * Copyright 2021
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

import { Schema } from 'app/pages/MainPage/pages/ViewPage/slice/types';

export interface HttpSourceConfig {
  url: string;
  method: string;
  property: string;
  username: string;
  password: string;
  timeout: string;
  ResponseParser: string;
  headers: string;
  queryParam: string;
  body: string;
  contentType: string;
  tableName: string;
  columns: Schema[];
}
