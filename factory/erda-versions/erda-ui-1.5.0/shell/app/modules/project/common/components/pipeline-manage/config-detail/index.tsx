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
import CaseInfo from './case-info';
import autoTestStore from 'project/stores/auto-test-case';
import CasePipelineEditor from './pipeline-editor';
import RecordList from './record-list';
import { useUpdate } from 'common/use-hooks';
import { get } from 'lodash';
import i18n from 'i18n';

interface IProps {
  caseId: string;
  addDrawerProps: Obj;
  scope: string;
  onCaseChange: (bool: boolean) => void;
}
const PipelineConfigDetail = (props: IProps) => {
  const { caseId, addDrawerProps = {}, scope, onCaseChange } = props;
  const [caseDetail] = autoTestStore.useStore((s) => [s.caseDetail]);
  const { getCaseDetail, updateCasePipeline } = autoTestStore;

  const [{ useCaseDetail, isLastRecord }, updater] = useUpdate({
    useCaseDetail: caseDetail,
    isLastRecord: true,
  });

  React.useEffect(() => {
    updater.useCaseDetail(caseDetail);
  }, [caseDetail, updater]);

  React.useEffect(() => {
    updater.isLastRecord(get(caseDetail, 'meta.historyID') === get(useCaseDetail, 'meta.historyID'));
  }, [caseDetail, updater, useCaseDetail]);

  React.useEffect(() => {
    onCaseChange(isLastRecord);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLastRecord]);

  const recordRef = React.useRef(null as any);

  React.useEffect(() => {
    getCaseDetail({ id: caseId });
  }, [caseId, getCaseDetail]);

  const onUpdateYml = (ymlStr: string) => {
    return updateCasePipeline({ pipelineYml: ymlStr, nodeId: caseId }).then(() => {
      getCaseDetail({ id: caseId });
      if (recordRef.current && recordRef.current.reload) {
        recordRef.current.reload();
      }
    });
  };

  const onSelectPipeline = (c: AUTO_TEST.ICaseDetail | null) => {
    updater.useCaseDetail(c || caseDetail);
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-2">
        <span className="font-medium title">{i18n.t('detail')}</span>
        <RecordList
          ref={recordRef}
          curPipelineDetail={useCaseDetail}
          onSelectPipeline={onSelectPipeline}
          caseId={caseId}
        />
      </div>
      <CaseInfo caseDetail={useCaseDetail} />
      <CasePipelineEditor
        scope={scope}
        addDrawerProps={{ ...addDrawerProps, curCaseId: caseId }}
        caseDetail={useCaseDetail}
        editable={isLastRecord}
        onUpdateYml={onUpdateYml}
      />
    </div>
  );
};

export default PipelineConfigDetail;
