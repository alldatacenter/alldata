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
import { get } from 'lodash';
import i18n from 'i18n';

import { Drawer } from 'antd';
import { BuildLog } from 'application/pages/build-detail/build-log';
import InfoPreview from 'config-page/components/info-preview/info-preview';
import { getPreviewData } from 'project/pages/auto-test/scenes';
import { useUpdate } from 'common/use-hooks';

const AutoTestPlanDetail = () => {
  const { projectId, testPlanId } = routeInfoStore.useStore((s) => s.params);
  const [{ logVisible, logProps, resultVis, previewData }, updater, update] = useUpdate({
    logVisible: false,
    logProps: {},
    resultVis: false,
    previewData: {} as CP_INFO_PREVIEW.Props,
  });
  const inParams = {
    projectId: +projectId,
    testPlanId: +testPlanId,
  };

  const hideLog = () => {
    update({
      logVisible: false,
      logProps: {},
    });
  };

  const closeResult = () => {
    update({
      resultVis: false,
      previewData: {} as CP_INFO_PREVIEW.Props,
    });
  };

  return (
    <>
      <DiceConfigPage
        scenarioType="auto-test-plan-detail"
        scenarioKey="auto-test-plan-detail"
        inParams={inParams}
        customProps={{
          executeTaskTable: {
            op: {
              operations: {
                checkLog: (d: any) => {
                  const { logId, pipelineId: pId, nodeId: nId } = get(d, 'meta') || {};
                  if (logId) {
                    update({
                      logVisible: true,
                      logProps: {
                        logId,
                        title: i18n.t('msp:log details'),
                        customFetchAPIPrefix: `/api/apitests/pipeline/${pId}/task/${nId}/logs`,
                        pipelineID: pId,
                        taskID: nId,
                        downloadAPI: '/api/apitests/logs/actions/download',
                      },
                    });
                  }
                },
                checkDetail: (d: any) => {
                  if (d) {
                    update({
                      resultVis: true,
                      previewData: getPreviewData(d),
                    });
                  }
                },
              },
            },
          },
        }}
      />
      <BuildLog visible={logVisible} hideLog={hideLog} {...logProps} />
      <Drawer width={1000} visible={resultVis} onClose={closeResult}>
        <InfoPreview {...previewData} />
      </Drawer>
    </>
  );
};
export default AutoTestPlanDetail;
