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

import { DataWithBackend } from '@/metas/DataWithBackend';
import { RenderRow } from '@/metas/RenderRow';
import { RenderList } from '@/metas/RenderList';
import i18n from '@/i18n';
import UserSelect from '@/components/UserSelect';
import { timestampFormat } from '@/utils';
import {
  statusList,
  lastConsumerStatusList,
  genStatusTag,
  genLastConsumerStatusTag,
} from './status';
import { consumes } from '..';

const { I18nMap, I18n } = DataWithBackend;
const { FieldList, FieldDecorator } = RenderRow;
const { ColumnList, ColumnDecorator } = RenderList;

export class ConsumeDefaultInfo implements DataWithBackend, RenderRow, RenderList {
  static I18nMap = I18nMap;
  static FieldList = FieldList;
  static ColumnList = ColumnList;

  readonly id: number;

  @FieldDecorator({
    type: 'input',
    extra: i18n.t('meta.Consume.ConsumerGroupNameRules'),
    rules: [
      { required: true },
      {
        pattern: /^[0-9a-z_-]+$/,
        message: i18n.t('meta.Consume.ConsumerGroupNameRules'),
      },
    ],
  })
  @ColumnDecorator()
  @I18n('meta.Consume.ConsumerGroupName')
  consumerGroup: string;

  @FieldDecorator({
    type: UserSelect,
    extra: i18n.t('meta.Consume.OwnersExtra'),
    rules: [{ required: true }],
    props: {
      mode: 'multiple',
      currentUserClosable: false,
    },
  })
  @ColumnDecorator()
  @I18n('meta.Consume.Owner')
  inCharges: string;

  @FieldDecorator({
    type: 'select',
    extraNames: ['mqType'],
    rules: [{ required: true }],
    props: {
      showSearch: true,
      filterOption: false,
      options: {
        requestTrigger: ['onOpen', 'onSearch'],
        requestService: keyword => ({
          url: '/group/list',
          method: 'POST',
          data: {
            keyword,
            pageNum: 1,
            pageSize: 20,
            status: 130,
          },
        }),
        requestParams: {
          formatResult: result =>
            result?.list?.map(item => ({
              ...item,
              label: `${item.inlongGroupId} (${item.mqType})`,
              value: item.inlongGroupId,
            })),
        },
      },
      onChange: (value, option) => ({
        topic: undefined,
        mqType: option.mqType,
      }),
    },
  })
  @ColumnDecorator()
  @I18n('meta.Consume.TargetInlongGroupID')
  inlongGroupId: string;

  @FieldDecorator({
    type: 'select',
    rules: [{ required: true }],
    props: values => ({
      mode: values.mqType === 'PULSAR' ? 'multiple' : '',
      options: {
        requestService: `/group/getTopic/${values.inlongGroupId}`,
        requestParams: {
          formatResult: result =>
            result.mqType === 'TUBEMQ'
              ? [
                  {
                    label: result,
                    value: result,
                  },
                ]
              : result.topics?.map(item => ({
                  ...item,
                  label: item,
                  value: item,
                })) || [],
        },
      },
      onChange: (value, option) => {
        if (typeof value !== 'string') {
          return {
            inlongStreamId: option.map(item => item.streamTopics).join(','),
          };
        }
      },
    }),
    visible: values => Boolean(values.inlongGroupId),
  })
  @ColumnDecorator()
  @I18n('meta.Consume.TopicName')
  topic: string;

  @FieldDecorator({
    type: 'text',
    visible: values => values.id,
  })
  @ColumnDecorator({
    render: text => consumes.find(c => c.value === text)?.label || text,
  })
  @I18n('meta.Consume.MQType')
  mqType: string;

  @FieldDecorator({
    type: 'select',
    props: {
      allowClear: true,
      options: statusList,
      dropdownMatchSelectWidth: false,
    },
    visible: false,
  })
  @ColumnDecorator({
    render: text => text && genStatusTag(text),
  })
  @I18n('basic.Status')
  readonly status: string;

  @ColumnDecorator({
    render: text => text && timestampFormat(text),
  })
  @I18n('pages.ConsumeDashboard.config.RecentConsumeTime')
  readonly lastConsumeTime: string;

  @FieldDecorator({
    type: 'select',
    props: {
      allowClear: true,
      dropdownMatchSelectWidth: false,
      options: lastConsumerStatusList,
    },
    visible: false,
  })
  @ColumnDecorator({
    render: text => text && genLastConsumerStatusTag(text),
  })
  @I18n('pages.ConsumeDashboard.config.OperatingStatus')
  readonly lastConsumeStatus: string;

  parse(data) {
    return data;
  }

  stringify(data) {
    return data;
  }

  renderRow() {
    const constructor = this.constructor as typeof ConsumeDefaultInfo;
    return constructor.FieldList;
  }

  renderList() {
    const constructor = this.constructor as typeof ConsumeDefaultInfo;
    return constructor.ColumnList;
  }
}
