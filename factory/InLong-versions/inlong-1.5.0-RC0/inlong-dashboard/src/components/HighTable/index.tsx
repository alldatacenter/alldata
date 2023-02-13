/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import * as React from 'react';
import { Table } from 'antd';
import paginationConfig from '@/configs/pagination';
import FormGenerator, { useForm, FormGeneratorProps } from '../FormGenerator';
import styles from './style.module.less';

export interface HighTableProps {
  // antd table props
  table: {
    [key: string]: any;
  };
  filterForm?: FormGeneratorProps;
  // Prefix, inserted before the filter form
  prefix?: React.ReactNode;
  // Suffix, inserted after the filter form
  suffix?: React.ReactNode;
  // Table prefix, inserted before the table
  tablePrefix?: React.ReactNode;
  // Table suffix, inserted after the table
  tableSuffix?: React.ReactNode;
  // Head Node layout rules
  // start: prefix and form are combined into one, suffix
  // end: prefix, form and suffix are combined into one
  // normal: prefix, form, suffix
  layout?: 'start' | 'end' | 'normal';
  // Head custom style
  headerStyle?: React.CSSProperties;
  style?: React.CSSProperties;
}

const HighTable: React.FC<HighTableProps> = ({
  table,
  filterForm,
  prefix: _prefix = null,
  suffix: _suffix = null,
  tablePrefix = null,
  tableSuffix = null,
  layout = 'start',
  headerStyle = {},
  style = {},
}) => {
  const pagination = {
    ...paginationConfig,
    ...table.pagination,
  };

  if (table.footer) {
    pagination.style = {
      position: 'absolute',
      right: 0,
      bottom: 0,
    };
  }

  const prefix = _prefix && <div className={styles.prefix}>{_prefix}</div>;
  const suffix = _suffix && <div className={styles.suffix}>{_suffix}</div>;

  const content =
    layout !== 'normal' ? (
      <>
        {layout === 'end' && prefix}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {layout === 'start' && prefix}
          <FormGenerator layout="inline" {...filterForm} />
          {layout === 'end' && suffix}
        </div>
        {layout === 'start' && suffix}
      </>
    ) : (
      <>
        {prefix}
        <FormGenerator layout="inline" {...filterForm} />
        {suffix}
      </>
    );

  const hasTableExtra = tablePrefix || tableSuffix;

  return (
    <div style={style}>
      {filterForm ? (
        <div
          className={styles.filterForm}
          style={{
            justifyContent: layout === 'normal' ? 'flex-start' : '',
            ...headerStyle,
          }}
        >
          {content}
        </div>
      ) : null}

      <div style={hasTableExtra ? { display: 'flex' } : {}}>
        {tablePrefix}
        <Table
          {...table}
          pagination={pagination}
          scroll={{
            ...table.scroll,
            // y: 550,
          }}
          style={hasTableExtra ? { ...table.style, flex: '1 1 auto' } : { ...table.style }}
        />
        {tableSuffix}
      </div>
    </div>
  );
};

export { useForm };

export default HighTable;
