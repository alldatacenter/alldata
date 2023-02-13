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

import React from 'react';
import routeInfoStore from 'core/stores/route';
import DiceConfigPage from 'app/config-page';
import { getUrlQuery } from 'config-page/utils';
import { updateSearch } from 'common/utils';
import { Spin } from 'antd';
import Download from './download';
import i18n from 'i18n';

interface IMeta {
  meta: { reportId: string };
}
const TestReport = () => {
  const [{ projectId }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [urlQuery, setUrlQuery] = React.useState(query);
  const [downloadId, setDownloadId] = React.useState('');
  const [downloading, setDownloading] = React.useState(false);

  React.useEffect(() => {
    updateSearch({ ...urlQuery });
  }, [urlQuery]);

  const inParams = {
    projectId: +projectId,
    ...urlQuery,
  };

  const urlQueryChange = (val: Obj) => setUrlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));

  const download = (opMeta: { reportId: string }) => {
    setDownloadId(opMeta.reportId);
    setDownloading(true);
  };

  const onFinish = () => {
    setDownloading(false);
    setDownloadId('');
  };

  return (
    <Spin spinning={downloading} tip={i18n.t('downloading')}>
      <DiceConfigPage
        scenarioType="test-report"
        scenarioKey="test-report"
        inParams={inParams}
        customProps={{
          filter: {
            op: {
              onFilterChange: urlQueryChange,
            },
          },
          table: {
            op: {
              operations: {
                download: (op: IMeta) => {
                  download(op?.meta);
                },
              },
            },
          },
        }}
      />
      {downloadId ? (
        <div className="absolute overflow-hidden" style={{ top: '100vw', height: 0 }}>
          <Download downloadId={downloadId} projectId={projectId} onFinish={onFinish} />
        </div>
      ) : null}
    </Spin>
  );
};

export default TestReport;
