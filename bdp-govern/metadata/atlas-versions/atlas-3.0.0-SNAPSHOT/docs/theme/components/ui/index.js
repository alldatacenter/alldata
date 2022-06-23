/**
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

import { Blockquote } from "./Blockquote";
import { InlineCode } from "./InlineCode";
import { Link } from "./Link";
import { Loading } from "./Loading";
import { NotFound } from "./NotFound";
import { OrderedList } from "./OrderedList";
import { Page } from "./Page";
import { Paragraph } from "./Paragraph";
import { Pre } from "./Pre";
import { Props } from "./Props";
import { Table } from "./Table";
import { UnorderedList } from "./UnorderedList";

export const components = {
  a: Link,
  blockquote: Blockquote,
  inlineCode: InlineCode,
  loading: Loading,
  notFound: NotFound,
  ol: OrderedList,
  p: Paragraph,
  page: Page,
  pre: Pre,
  props: Props,
  table: Table,
  ul: UnorderedList
};