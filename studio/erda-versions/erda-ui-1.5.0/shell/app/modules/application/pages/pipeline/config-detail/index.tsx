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
import fileTreeStore from 'common/stores/file-tree';
import CasePipelineEditor from './pipeline-editor';
// import RecordList from './record-list';
import { useUpdate } from 'common/use-hooks';
import repoStore from 'application/stores/repo';
import { getBranchPath } from 'application/pages/pipeline/config';
import { get } from 'lodash';
import i18n from 'i18n';

interface IProps {
  nodeId: string;
  addDrawerProps: Obj;
  scope: string;
  onCaseChange: (bool: boolean) => void;
  scopeParams: { scope: string; scopeID: string };
  editAuth: { hasAuth: boolean; authTip?: string };
}

const PipelineConfigDetail = (props: IProps) => {
  const { nodeId, addDrawerProps = {}, scope, onCaseChange, scopeParams, editAuth } = props;
  const [caseDetail] = fileTreeStore.useStore((s) => [s.curNodeDetail]);
  const { getTreeNodeDetailNew } = fileTreeStore;
  const { commit } = repoStore.effects;
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
    getTreeNodeDetailNew({ id: nodeId, ...scopeParams });
  }, [nodeId, getTreeNodeDetailNew, scopeParams]);
  const onUpdateYml = (ymlStr: string) => {
    const fileName = caseDetail.name;
    const { branch, path } = getBranchPath(caseDetail);

    return commit({
      branch,
      message: `Update ${fileName}`,
      actions: [
        {
          content: ymlStr,
          path,
          action: 'update',
          pathType: 'blob',
        },
      ],
    }).then(() => {
      getTreeNodeDetailNew({ id: nodeId, ...scopeParams });
      if (recordRef.current && recordRef.current.reload) {
        recordRef.current.reload();
      }
    });
  };

  // const onSelectPipeline = (c: any) => {
  //   updater.useCaseDetail(c || caseDetail);
  // };

  return (
    <div>
      <div className="flex justify-between items-center mb-2">
        <span className="font-medium title">{i18n.t('detail')}</span>
        {/* <RecordList ref={recordRef} curPipelineDetail={useCaseDetail} onSelectPipeline={onSelectPipeline} nodeId={nodeId} /> */}
      </div>
      <CaseInfo caseDetail={useCaseDetail} />
      <CasePipelineEditor
        addDrawerProps={{ ...addDrawerProps, curCaseId: nodeId, scope }}
        caseDetail={useCaseDetail}
        editable={editAuth.hasAuth && isLastRecord}
        onUpdateYml={onUpdateYml}
      />
    </div>
  );
};

export default PipelineConfigDetail;
