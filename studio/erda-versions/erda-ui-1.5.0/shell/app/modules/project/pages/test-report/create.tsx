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
import { Form, Input, Button } from 'antd';
import DiceConfigPage from 'config-page';
import IterationSelect from 'project/common/components/issue/iteration-select';
import { useUpdate } from 'common/use-hooks';
import { encode } from 'js-base64';
import i18n from 'i18n';
import { set } from 'lodash';
import { produce } from 'immer';
import { goTo } from 'common/utils';
import { MarkdownEditor, Title } from 'common';
import { saveTestReport } from 'project/services/project';

interface IState {
  testDashboard: Obj | null;
  issueDashboard: Obj | null;
  chosenIterationID: string;
}

const CreateTestReport = () => {
  const [{ projectId }] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [form] = Form.useForm();
  const saving = saveTestReport.useLoading();
  const [{ testDashboard, issueDashboard, chosenIterationID }, updater] = useUpdate<IState>({
    testDashboard: null,
    issueDashboard: null,
    chosenIterationID: '',
  });

  const onClick = () => {
    form.validateFields().then((res) => {
      saveTestReport
        .fetch({
          projectId,
          ...res,
          iterationID: +res.iterationID,
          reportData: {
            'issue-dashboard': issueDashboard,
            'test-dashboard': testDashboard,
          },
        })
        .then(() => {
          goTo('../');
        });
    });
  };

  const changeIteration = (v: string) => {
    updater.chosenIterationID(v);
  };

  const inParams = { projectId };
  // TODO: better add a iterationID inparams; need backend support
  if (chosenIterationID) {
    inParams.filter__urlQuery = encode(`{"iteration":[${chosenIterationID}]}`);
  }

  const handleDashboardFilter = (data: Obj) => {
    return produce(data, (draft) => {
      const conditions = draft.protocol?.components?.filter?.state?.conditions || [];
      const reConditions = conditions.map((cond: Obj) => {
        if (cond.key === 'iteration') {
          return { ...cond, disabled: true };
        }
        return cond;
      });
      set(draft, 'protocol.components.filter.state.conditions', reConditions);
    });
  };

  return (
    <div>
      <div className="top-button-group">
        <Button type="primary" onClick={onClick} loading={saving}>
          {i18n.t('dop:create test report')}
        </Button>
      </div>
      <div className="bg-white rounded p-2">
        <Form className="w-3/5" layout="vertical" form={form}>
          <Form.Item label={i18n.t('cmp:report name')} name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label={i18n.t('dop:iteration')} name="iterationID" rules={[{ required: true }]}>
            <IterationSelect onChange={changeIteration} autoSelectFirst disabledBacklog />
          </Form.Item>
          <Form.Item label={i18n.t('dop:test summary')} name="summary" rules={[{ required: true }]}>
            <MarkdownEditor />
          </Form.Item>
        </Form>
      </div>
      {chosenIterationID ? (
        <div key={chosenIterationID}>
          <Title title={i18n.t('dop:test statistics')} />
          <DiceConfigPage
            scenarioType={'test-dashboard'}
            scenarioKey={'test-dashboard'}
            fullHeight={false}
            updateConfig={(v) => {
              updater.testDashboard(handleDashboardFilter(v));
            }}
            debugConfig={testDashboard}
            inParams={inParams}
          />

          <Title title={i18n.t('dop:test statistics')} />
          <DiceConfigPage
            scenarioType={'issue-dashboard'}
            scenarioKey={'issue-dashboard'}
            inParams={inParams}
            fullHeight={false}
            debugConfig={issueDashboard}
            updateConfig={(v) => {
              updater.issueDashboard(handleDashboardFilter(v));
            }}
          />
        </div>
      ) : null}
    </div>
  );
};

export default CreateTestReport;
