/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import { App } from 'vue'
import {
  ConfigProvider,
  Select,
  Menu,
  Button,
  Table,
  Tooltip,
  Input,
  Modal,
  Form,
  InputNumber,
  Pagination,
  Spin,
  Divider,
  Tabs,
  List,
  Breadcrumb,
  Checkbox,
  AutoComplete,
  Empty,
  Upload,
  Radio,
  Collapse
} from 'ant-design-vue'

const compontens = [
  ConfigProvider,
  Spin,
  Divider,
  Tabs,
  InputNumber,
  Select,
  Menu,
  Button,
  Tooltip,
  Table,
  Input,
  Modal,
  Form,
  Pagination,
  List,
  Breadcrumb,
  Checkbox,
  AutoComplete,
  Empty,
  Upload,
  Radio,
  Collapse
]
export default function (app: App): void {
  compontens.forEach(app.use)
}
