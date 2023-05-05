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
import { message, Tooltip, Radio, Button } from 'antd';
import { FormModal, Icon as CustomIcon, IF, DeleteConfirm, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo, updateSearch } from 'common/utils';
import RepoFile from './repo-file';
import RepoEditor from './repo-editor';
import { isEmpty, get, find } from 'lodash';
import { useUpdateEffect, useMount } from 'react-use';
import { RepoBlame } from './repo-blame';
import DiceYamlEditor from './yml-editor';
import {
  needRenderWorkFlowView,
  isPipelineWorkflowYml,
  isPipelineYml,
  isInDiceDirectory,
  isYml,
} from 'application/common/yml-flow-util';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';

import appStore from 'application/stores/application';
import { getInfoFromRefName } from '../util';
import { useLoading } from 'core/stores/loading';
import { AppPermType } from 'app/user/stores/_perm-state';
import { usePerm } from 'user/common';
import PipelineYml from 'application/common/yml-editor/pipeline-editor';
import i18n from 'i18n';

import './repo-file.scss';

const { Group } = Radio;
const { Group: ButtonGroup } = Button;

interface IProps {
  name: string;
  autoHeight: boolean;
  noEdit: boolean; // tree下的ReadMe不能直接编辑
  branchInfo: APPLICATION.IBranchInfo[];
  permMap: AppPermType;
  maxLines?: number;
}

const RepoFileContainerComp = (props: IProps) => {
  const [state, updater] = useUpdate({
    modalVisible: false,
    viewType: 'graphic',
  });
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { name: fileName } = props;
  const [info, blob, mode, pipelineYmlStructure, tree] = repoStore.useStore((s) => [
    s.info,
    s.blob,
    s.mode,
    s.pipelineYmlStructure,
    s.tree,
  ]);
  const [parsePipelineYmlStructureLoading, getRepoBlobLoading] = useLoading(repoStore, [
    'parsePipelineYmlStructure',
    'getRepoBlob',
  ]);

  const { commit, getRepoBlob } = repoStore.effects;
  const { changeMode } = repoStore.reducers;
  const toggleModal = (modalVisible: boolean) => {
    updater.modalVisible(modalVisible);
  };

  useMount(() => {
    if (query?.editPipeline) {
      // clear editPipeline in query, replace=true to forbidden Previous useless url
      updateSearch({ editPipeline: undefined }, { gotoOption: { replace: true } });
      // url change produce a mode reset in repo store, use setTimeout delay changeMode after reset.
      setTimeout(() => {
        changeMode({ editFile: true });
      });
    }
  });

  // 是否为dice.yml 或 pipeline.yml
  const isDiceOrPipelineFile = React.useMemo(() => {
    return needRenderWorkFlowView(fileName);
  }, [fileName]);

  useUpdateEffect(() => {
    if (
      isDiceOrPipelineFile &&
      !parsePipelineYmlStructureLoading &&
      !getRepoBlobLoading &&
      isEmpty(pipelineYmlStructure) &&
      blob.path.includes('pipeline.yml')
    ) {
      updater.viewType('code');
    }
  }, [isDiceOrPipelineFile, getRepoBlobLoading, parsePipelineYmlStructureLoading, pipelineYmlStructure, updater]);

  const handleDelete = (values: Pick<REPOSITORY.Commit, 'message' | 'branch'>) => {
    commit({
      ...values,
      actions: [
        {
          action: 'delete',
          path: blob.path,
          pathType: 'blob',
        },
      ],
      isDelete: true,
    }).then((res) => {
      toggleModal(false);
      if (res.success) {
        message.success(i18n.t('dop:file deleted successfully'));
        // back to parent path
        const parentPath = res?.data?.commit?.parentDirPath;
        const { branch, tag } = getInfoFromRefName(info.refName);
        const curBranch = branch || tag || info.defaultBranch;
        const newPath = `${goTo.resolve.repo()}/tree/${curBranch}/${parentPath}`;
        goTo(newPath);
      }
    });
  };
  const changeRadioValue = (e: any) => {
    updater.viewType(e.target.value);
  };
  const renderRadioGroup = () => {
    const { viewType } = state;
    const { editFile } = mode;
    const disabled = !!editFile;
    return (
      <Group
        className="radio-btn-group flex justify-between items-center"
        value={viewType}
        size="small"
        onChange={changeRadioValue}
        disabled={disabled}
      >
        <Radio.Button value="graphic">
          <CustomIcon className="yml-editor-code-btn" type="lc" />
        </Radio.Button>
        <Radio.Button value="code">
          <CustomIcon className="yml-editor-code-btn" type="html1" />
        </Radio.Button>
      </Group>
    );
  };
  const renderEditBtns = (curBranch: string) => {
    const { noEdit, permMap, branchInfo } = props;
    const { editFile, fileBlame } = mode;
    const branchAuthObj = permMap.repo.branch;
    if (noEdit) {
      return null;
    }

    if (editFile) {
      return (
        <Tooltip title={i18n.t('cancel')}>
          <ErdaIcon
            className="cursor-pointer"
            width="20"
            height="21"
            fill="black-400"
            type="qxbj"
            onClick={() => changeMode({ editFile: false, addFile: false, fileBlame: false })}
          />
        </Tooltip>
      );
    } else {
      const { projectId, appId } = params;
      const { path, binary } = blob;
      const { branch } = getInfoFromRefName(info.refName);
      const isBranchTree = !!curBranch;
      const isProtectBranch = get(find(branchInfo, { name: branch }), 'isProtect');
      const branchAuth = isProtectBranch ? branchAuthObj.writeProtected.pass : branchAuthObj.writeNormal.pass;

      const disabledTips = branchAuth
        ? [i18n.t('dop:can only edit files under the branch'), i18n.t('dop:can only delete files under the branch')]
        : [
            i18n.t('dop:branch is protected, you have no permission yet'),
            i18n.t('dop:branch is protected, you have no permission yet'),
          ];
      return (
        <IF check={isBranchTree && branchAuth}>
          <ButtonGroup className="mr-5">
            <Button size="small" onClick={() => changeMode({ fileBlame: !fileBlame })}>
              {fileBlame ? i18n.t('dop:normal view') : i18n.t('dop:view by line')}
            </Button>
            <Button
              size="small"
              onClick={() => {
                goTo(goTo.pages.commits, { projectId, appId, branch, path });
              }}
            >
              {i18n.t('dop:submission history')}
            </Button>
          </ButtonGroup>
          <IF check={!binary}>
            <Tooltip title={info.isLocked ? i18n.t('dop:lock-operation-tip') : i18n.t('edit')}>
              <div className="mt-1 mr-3">
                <ErdaIcon
                  fill="black-400"
                  size="20"
                  className={`${info.isLocked ? 'disabled' : ''} cursor-pointer`}
                  type="bj"
                  onClick={() => !info.isLocked && changeMode({ editFile: true })}
                />
              </div>
            </Tooltip>
          </IF>
          <Tooltip title={info.isLocked ? i18n.t('dop:lock-operation-tip') : i18n.t('delete')}>
            <ErdaIcon
              fill="black-400"
              width="20"
              height="21"
              className={`${info.isLocked ? 'disabled' : ''} cursor-pointer`}
              type="sc1"
              onClick={() => !info.isLocked && toggleModal(true)}
            />
          </Tooltip>

          <IF.ELSE />

          <Tooltip title={disabledTips[0]}>
            <CustomIcon type="bj" className="not-allowed" />
          </Tooltip>
          <Tooltip title={disabledTips[1]}>
            <CustomIcon type="sc1" className="not-allowed" />
          </Tooltip>
        </IF>
      );
    }
  };
  const onYmlUpgrade = () => {
    const { ymlContent } = pipelineYmlStructure;
    const { path } = tree;
    const { branch } = getInfoFromRefName(tree.refName);

    commit({
      branch,
      message: `Update ${path}`,
      actions: [
        {
          action: 'update',
          content: ymlContent,
          path,
          pathType: 'blob',
        },
      ],
    }).then((res) => {
      if (res.success) {
        changeMode({ editFile: false, addFile: false });
        message.success(i18n.t('dop:file modified successfully'));
        getRepoBlob();
      }
    });
  };
  const getFileAlertIcon = () => {
    const { path } = tree;
    if (pipelineYmlStructure && isPipelineWorkflowYml(path)) {
      return pipelineYmlStructure.needUpgrade ? (
        <>
          <DeleteConfirm
            title={`${path} ${i18n.t('dop:New version available. Upgrade now?')}?`}
            secondTitle=""
            onConfirm={onYmlUpgrade}
          >
            <div className="file-alert cursor-pointer">
              <CustomIcon className="mr-1" type="jg" />
              <span className="alert-text">
                {i18n.t('dop:current')} {path} {i18n.t('dop:can be upgraded with one click')}！
              </span>
            </div>
          </DeleteConfirm>
        </>
      ) : undefined;
    }
    return undefined;
  };
  const renderContent = () => {
    const { name, autoHeight, maxLines } = props;
    const { viewType } = state;
    const { branch } = getInfoFromRefName(tree.refName);
    const { editFile, fileBlame } = mode;
    let ops = renderEditBtns(branch || '');
    if ((isDiceOrPipelineFile || (isInDiceDirectory(tree.path) && isYml(fileName))) && !fileBlame) {
      const fileAlertIcon = getFileAlertIcon();
      ops = (
        <React.Fragment>
          {fileAlertIcon}
          <span className="viewer-ops">{renderRadioGroup()}</span>
          <span className="editor-ops">{renderEditBtns(branch || '')}</span>
        </React.Fragment>
      );
    }

    if (fileBlame) {
      return <RepoBlame {...(props as any)} ops={ops} />;
    } else if (isPipelineYml(fileName) || (isInDiceDirectory(tree.path) && isYml(fileName))) {
      // 3.19：新pipelineYml接管pipeline文件的查看、编辑、状态切换，编辑状态行为同流水线模板（addPipelineYml）
      return (
        <PipelineYml
          viewType={viewType}
          fileName={name}
          ops={ops}
          editing={editFile}
          content={blob.content}
          onUpdateViewType={(val: string) => updater.viewType(val)}
        />
      );
    } else if (viewType === 'graphic' && isDiceOrPipelineFile) {
      return (
        <React.Fragment>
          <DiceYamlEditor ops={ops} editing={editFile} fileName={name} content={blob.content} />
        </React.Fragment>
      );
    } else if (editFile) {
      return (
        <RepoEditor
          isDiceOrPipelineFile={isDiceOrPipelineFile}
          name={name}
          blob={mode.editFile ? blob : ({} as REPOSITORY.IBlob)}
          autoHeight={autoHeight}
          maxLines={maxLines}
          ops={ops}
        />
      );
    } else {
      return <RepoFile {...(props as any)} ops={ops} />;
    }
  };
  const getFieldsList = () => {
    const { name } = props;
    const { branch } = getInfoFromRefName(info.refName);
    const fieldsList = [
      {
        label: i18n.t('dop:submit information'),
        name: 'message',
        type: 'textArea',
        itemProps: {
          maxLength: 200,
          autoSize: { minRows: 3, maxRows: 7 },
        },
        initialValue: `Delete ${name}`,
      },
      {
        label: i18n.t('dop:submit branch'),
        name: 'branch',
        type: 'select',
        initialValue: branch,
        options: (info.branches || []).map((a) => ({ name: a, value: a })),
        itemProps: {
          disabled: true,
        },
      },
    ];
    return fieldsList;
  };
  return (
    <React.Fragment>
      {renderContent()}
      <FormModal
        width={620}
        title={`${i18n.t('delete')}${props.name}`}
        fieldsList={getFieldsList()}
        visible={state.modalVisible}
        onOk={handleDelete}
        onCancel={() => toggleModal(false)}
      />
    </React.Fragment>
  );
};

const PureRepoFileContainer = React.memo(RepoFileContainerComp);

interface IContainerProps extends Partial<IProps> {
  [prop: string]: any;
  path: string;
}

const RepoFileContainer = (props: IContainerProps) => {
  const { getRepoBlob } = repoStore.effects;
  const { clearRepoBlob } = repoStore.reducers;
  const [detail, branchInfo] = appStore.useStore((s) => [s.detail, s.branchInfo]);
  const { gitRepoAbbrev } = detail;
  const permMap = usePerm((s) => s.app);
  const { path } = props;
  React.useEffect(() => {
    if (gitRepoAbbrev) {
      getRepoBlob({ path, repoPrefix: gitRepoAbbrev });
    }
    return () => {
      clearRepoBlob();
    };
  }, [clearRepoBlob, getRepoBlob, gitRepoAbbrev, path]);
  return <PureRepoFileContainer {...(props as IProps)} branchInfo={branchInfo} permMap={permMap} />;
};

export default RepoFileContainer;
