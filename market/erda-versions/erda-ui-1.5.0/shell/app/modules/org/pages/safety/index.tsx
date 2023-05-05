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

import { useUserMap } from 'core/stores/userMap';
import { CustomFilter, MemberSelector, FileEditor } from 'common';
import { useFilter } from 'common/use-hooks';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';
import moment from 'moment';
import { DatePicker, Table, Button, Popover, Drawer } from 'antd';
import auditStore from 'org/stores/audit';
import auditTpl from 'org/common/audit-render';
import { has, isPlainObject } from 'lodash';
import React from 'react';
import { getTimeRanges, qs, setApiWithOrg } from 'common/utils';
import orgStore from 'app/org-home/stores/org';
import routeInfoStore from 'core/stores/route';

const TestModal = ({ onOk }: { onOk: (d: Obj) => void }) => {
  const [value, setValue] = React.useState('');
  const [err, setErr] = React.useState('');
  const [visible, setVisible] = React.useState(false);
  const test = () => {
    try {
      onOk(JSON.parse(value));
      setErr('');
    } catch (error) {
      setErr(String(error));
    }
  };
  return (
    <>
      <Button onClick={() => setVisible(true)}>Test Template</Button>
      <Drawer
        mask={false}
        height={350}
        title={
          <>
            <Button type="primary" onClick={test}>
              Test
            </Button>
            <span className="ml-1">Paste your template json here, click button to view result</span>
          </>
        }
        placement="bottom"
        bodyStyle={{ padding: 0 }}
        visible={visible}
        onClose={() => setVisible(false)}
      >
        <p>{err}</p>
        <FileEditor fileExtension="json" minLines={13} value={value} onChange={setValue} />
      </Drawer>
    </>
  );
};

const formatStrWithBr = (s: string) => {
  const reContent: Array<string | JSX.Element> = [];
  if (s.includes('\n')) {
    s.split('\n').forEach((a) => {
      reContent.push(a);
      reContent.push(<br key={a} />);
    });
    reContent.pop();
  } else {
    reContent.push(s);
  }
  return reContent;
};

const AuditList = ({ sys }: { sys: boolean }) => {
  const orgId = orgStore.useStore((s) => s.currentOrg.id);
  const query = routeInfoStore.getState((s) => s.query);
  const [extraTpls, setExtraTpls] = React.useState({});
  const userMap = useUserMap();
  const [loading] = useLoading(auditStore, ['getList']);
  const [list, paging] = auditStore.useStore((s) => [s.auditList, s.auditPaging]);

  const columns = [
    {
      title: i18n.t('cmp:operation time'),
      dataIndex: 'startTime',
      width: 180,
      render: (val: string) => moment(val).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('cmp:audit-operator'),
      dataIndex: 'userId',
      width: 150,
      render: (v: string) => {
        return userMap[v] ? userMap[v].nick || userMap[v].name : v;
      },
    },
    {
      title: i18n.t('operation'),
      key: 'op',
      render: (val: string, r: AUDIT.Item) => {
        const content = auditTpl(r, extraTpls);
        const contentRender: Array<string | JSX.Element> = [];
        let contentTipRender: Array<string | JSX.Element> = [];
        if (Array.isArray(content)) {
          content.forEach((c) => {
            if (typeof c === 'string' && c.includes('\n')) {
              contentRender.push(c);
              contentTipRender = contentTipRender.concat(formatStrWithBr(c));
            } else if (isPlainObject(c) && has(c, 'value')) {
              const { value, Comp } = c;
              contentRender.push(<Comp key={contentRender.length} value={value} />);
              contentTipRender.push(<Comp key={contentRender.length} value={formatStrWithBr(value)} />);
            } else {
              contentRender.push(c);
              contentTipRender.push(c);
            }
          });
        } else {
          contentRender.push(content);
        }
        return (
          <div className="flex">
            <Popover
              overlayStyle={{ maxWidth: '80%' }}
              content={contentTipRender.length ? contentTipRender : contentRender}
            >
              <span className="truncate max-w-full inline-block">
                {contentRender}
                {r.result !== 'success' ? ` (${r.errorMsg})` : null}
              </span>
            </Popover>
          </div>
        );
      },
    },
    {
      title: 'IP',
      dataIndex: 'clientIp',
      width: 150,
    },
  ];

  const filterConfig = React.useMemo(
    () => [
      {
        type: MemberSelector,
        name: 'userId',
        customProps: sys
          ? { scopeType: 'uc', mode: 'multiple', size: 'small', allowClear: true }
          : {
              scopeType: 'org',
              mode: 'multiple',
              size: 'small',
              valueChangeTrigger: 'onClose',
              placeholder: i18n.t('search by user name'),
            },
      },
      {
        type: DatePicker.RangePicker,
        name: 'startAt,endAt',
        valueType: 'range',
        customProps: {
          showTime: { format: 'HH:mm' },
          allowClear: false,
          format: 'YYYY-MM-DD HH:mm',
          ranges: getTimeRanges(),
          style: {
            width: 330,
          },
        },
      },
    ],
    [sys],
  );

  const { onSubmit, onTableChange, queryCondition } = useFilter<AUDIT.Item[]>({
    getData: auditStore.effects.getList,
    extraQuery: { sys },
    initQuery: {
      startAt: moment().subtract(1, 'hours').format('YYYY-MM-DD HH:mm:ss'),
      endAt: moment().format('YYYY-MM-DD HH:mm:ss'),
    },
  });

  const onExport = () => {
    const extra = sys ? { sys: true } : { orgId };
    window.open(
      setApiWithOrg(
        `/api/audits/actions/export-excel?${qs.stringify({ ...queryCondition, ...extra }, { arrayFormat: 'repeat' })}`,
      ),
    );
  };

  return (
    <>
      <div className="top-button-group">
        {query.testTpl && <TestModal onOk={setExtraTpls} />}
        <Button type="primary" onClick={onExport}>
          {i18n.t('export')}
        </Button>
      </div>
      <CustomFilter onSubmit={onSubmit} config={filterConfig} isConnectQuery />
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        onChange={onTableChange}
        pagination={{
          current: paging.pageNo,
          pageSize: +paging.pageSize,
          total: paging.total,
        }}
        scroll={{ x: '100%' }}
      />
    </>
  );
};

export default AuditList;
