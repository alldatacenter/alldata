// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

// @ts-nocheck
// @prefix fil.li
// @description Filter config list
const filterField = [
  {
    type: Input,
    name: '${1:name}',
    customProps: {
      placeholder: i18n.t('filter by {name}', { name: i18n.t('${1:name}') }),
    },
  },
  {
    type: Select,
    name: '${2:state}',
    customProps: {
      placeholder: i18n.t('filter by {name}', { name: i18n.t('${2:state}') }),
      allowClear: true,
      options: (${3:selList} || []).map(item => {
        return (
          <Option key={item.key} value={item.key}>{item.name}</Option>
        );
      }),
    },
  },
  {
    type: MemberSelector,
    name: '${4:creator}',
    customProps: {
      placeholder: i18n.t('filter by {name}', { name: i18n.t('${4:submitter}') }),
      scopeType: 'project',
      size: 'small',
      scopeId: projectId,
      allowClear: true,
    },
  },
];

// ---
// @prefix fil.com
// @description Filter component
<Filter config={filterField} onFilter={onFilter}${1: connectUrlSearch}${2: urlExtra={urlExtra\}} />

// ---
// @prefix fil.on
// @description Filter onFilter function
const onFilter = (query: Obj = {}) => {
  updater.filterData(query);
  getList({ pageNo: 1, ...query });
};

// ---
// @prefix fil.s
// @description Select config in Filter
{
  type: ${1:Select},
  name: ${2:'type'},
  customProps: {
    allowClear: true,
    options: $3,
    placeholder: $4,
  },
}

// ---
// @prefix fil.i
// @description Input config in Filter
{
  type: ${1:Input},
  name: ${2:'name'},
  customProps: {
    allowClear: true,
    placeholder: $3,
  },
}
