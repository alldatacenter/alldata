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

import { ConfigLayout, Copy } from 'common';
import i18n from 'i18n';
import React from 'react';
import { getInfoBlock } from '../artifacts/artifacts-info';
import { Button } from 'antd';
import errorReportStore from 'publisher/stores/error-report';
import routeInfoStore from 'core/stores/route';
import { formatTime } from 'common/utils';
import { useUnmount } from 'react-use';

const ErrorReportDetail = () => {
  const [publisherItemId, query] = routeInfoStore.useStore((s) => [s.params.publisherItemId, s.query]);
  const detail = errorReportStore.useStore((s) => s.errorDetail);
  let tags: Obj = {};
  let log = i18n.t('publisher:no data');
  if (detail) {
    tags = detail.tags;
    tags.timestamp = detail['@timestamp'];
    log = decodeURIComponent(tags.stack_trace);
  }

  useUnmount(() => {
    errorReportStore.reducers.clearErrorDetail();
  });

  React.useEffect(() => {
    if (query.filter) {
      errorReportStore.effects.getErrorDetail({
        artifactsId: publisherItemId,
        filter_error: query.filter,
        start: query.start,
        end: query.end,
        limit: 1,
        ak: query.ak,
        ai: query.ai,
      });
    }
  }, [publisherItemId, query.ai, query.ak, query.end, query.filter, query.start]);

  const fields = [
    [i18n.t('publisher:device model'), 'md'],
    [i18n.t('system version'), 'osv'],
    [i18n.t('publisher:app package name'), 'dh'],
    [i18n.t('publisher:app version'), 'av'],
    [i18n.t('publisher:cpu architecture'), 'cpu'],
    [i18n.t('publisher:mem'), 'mem'],
    [i18n.t('publisher:storage space'), 'rom'],
    [i18n.t('publisher:client ID'), 'cid'],
    [i18n.t('publisher:whether to jailbreak'), 'jb'],
    [i18n.t('publisher:gps'), 'gps'],
    [i18n.t('user ID'), 'uid'],
    [i18n.t('publisher:ip address'), 'ip'],
    [i18n.t('publisher:client occurrence time'), 'timestamp', 'time'],
  ];
  const basicFieldsList = fields.map((item) => {
    const [label, name, type] = item;
    const typeMap = {
      time: (val: string) => formatTime(val, 'YYYY-MM-DD HH:mm:ss'),
    };
    if (type) {
      return { label, name, render: typeMap[type] };
    }
    return { label, name };
  });

  const sectionList: any[] = [
    {
      title: i18n.t('basic information'),
      children: getInfoBlock(basicFieldsList, tags),
    },
    {
      title: i18n.t('publisher:detailed log'),
      titleExtra: (
        <>
          <Copy selector=".cursor-copy" />
          <Button className="cursor-copy" data-clipboard-text={log} type="primary" ghost>
            {i18n.t('copy')}
          </Button>
        </>
      ),
      children: (
        <pre className="code-block" style={{ whiteSpace: 'pre-wrap' }}>
          {log}
        </pre>
      ),
    },
  ];

  return (
    <>
      <ConfigLayout sectionList={sectionList} />
    </>
  );
};

export default ErrorReportDetail;
