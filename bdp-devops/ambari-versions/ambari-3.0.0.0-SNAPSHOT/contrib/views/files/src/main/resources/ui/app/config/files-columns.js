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

/*
  Static configuration for the columns to be shown.
*/
var columnsConfig = [
  {
    title: 'Name',
    key: 'name',
    isVisible: true,
    sortable: true,
    sortOrder: 0,
    columnClass: 'col-md-3 col-xs-3'
  },
  {
    title: 'Size',
    key: 'size',
    isVisible: true,
    sortable: true,
    sortOrder: 0,
    columnClass: 'col-md-1 col-xs-1'
  },
  {
    title: 'Last Modified',
    key: 'date',
    isVisible: true,
    sortable: true,
    sortOrder: 0,
    columnClass: 'col-md-2 col-xs-2'
  },
  {
    title: 'Owner',
    key: 'owner',
    isVisible: true,
    sortable: true,
    sortOrder: 0,
    columnClass: 'col-md-1 col-xs-1'
  },
  {
    title: 'Group',
    key: 'group',
    isVisible: true,
    sortable: true,
    sortOrder: 0,
    columnClass: 'col-md-1 col-xs-1'
  },
  {
    title: 'Permission',
    key: 'permission',
    isVisible: true,
    sortable: false,
    sortOrder: 0,
    columnClass: 'col-md-1 col-xs-1'
  },
  {
    title: 'Erasure Coding',
    key: 'erasureCodingPolicyName',
    isVisible: true,
    sortable: false,
    sortOrder: 0,
    columnClass: 'col-md-2 col-xs-2'
  },
  {
    title: 'Encrypted',
    key: 'isEncrypted',
    isVisible: true,
    sortable: false,
    sortOrder: 0,
    columnClass: 'col-md-1 col-xs-1'
  }
];

export default columnsConfig;
