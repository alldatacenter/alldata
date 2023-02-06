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
import i18n from 'i18n';
import yaml from 'js-yaml';
import { get, omit, isEmpty } from 'lodash';
import { notify, isPromise } from 'common/utils';
import { Spin, Button, message, Radio, Tooltip, Modal, Popconfirm } from 'antd';
import { RenderForm, FileEditor, Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import repoStore from 'application/stores/repo';
import { getInfoFromRefName } from 'application/pages/repo/util';
import { insertWhen } from 'common/utils';
import FileContainer from 'application/common/components/file-container';
import { produce } from 'immer';
import { useLoading } from 'core/stores/loading';
import { IYmlEditorProps } from './index';
import { PipelineGraphicEditor } from 'yml-chart/common/pipeline-graphic-editor';
import { NodeEleMap, externalKey, NodeType } from 'yml-chart/config';
import { parsePipelineYmlStructure } from 'application/services/repo';
import { ActionType } from 'yml-chart/common/pipeline-node-drawer';
import appStore from 'application/stores/application';

import './pipeline-editor.scss';

enum ViewType {
  graphic = 'graphic',
  code = 'code',
}

const noop = () => {};
const PipelineEditor = (props: IYmlEditorProps) => {
  const { fileName, ops, editing, viewType: propsViewType = 'code', content = '', onUpdateViewType = noop } = props;

  const appDetail = appStore.useStore((s) => s.detail);
  const curScope = appDetail.isProjectLevel ? ActionType.projectLevelAppPipeline : ActionType.appPipeline;

  const [info, tree, editFile, blob] = repoStore.useStore((s) => [s.info, s.tree, s.mode.editFile, s.blob]);
  const { changeMode } = repoStore.reducers;
  const { commit, getRepoBlob } = repoStore.effects;
  const formRef: any = React.useRef(null);
  const form = formRef.current;
  const [loading] = useLoading(repoStore, ['commit']);
  const [
    { ymlObj, viewType, txtValue, errorMsg, pipelineYmlStructure, originPipelineYmlStructure, originYmlValid },
    updater,
    update,
  ] = useUpdate({
    ymlObj: {} as IPipelineYmlStructure,
    viewType: propsViewType,
    txtValue: '',
    originYmlValid: true,
    errorMsg: '',
    pipelineYmlStructure: {} as IPipelineYmlStructure,
    originPipelineYmlStructure: {} as IPipelineYmlStructure,
  });

  React.useEffect(() => {
    if (blob && blob.content) {
      parsePipelineYmlStructure({ pipelineYmlContent: blob.content })
        .then((res: any) => {
          update({
            pipelineYmlStructure: res && res.data,
            originPipelineYmlStructure: res && res.data,
          });
        })
        .catch(() => {
          update({
            txtValue: blob.content,
            viewType: ViewType.code,
            originYmlValid: false,
          });
        });
    } else {
      updater.viewType(ViewType.code);
    }
  }, [blob, update, updater]);

  React.useEffect(() => {
    updater.txtValue(content);
  }, [content, updater]);

  React.useEffect(() => {
    updater.viewType(propsViewType);
  }, [propsViewType, updater]);

  React.useEffect(() => {
    onUpdateViewType(viewType);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewType]);

  React.useEffect(() => {
    updater.ymlObj(pipelineYmlStructure);
  }, [pipelineYmlStructure, updater, editFile]);

  const onDeleteData = (nodeData: any) => {
    const { [externalKey]: externalData } = nodeData;
    const { xIndex, yIndex } = externalData || {};
    const newYmlObj = produce(ymlObj, (draft: IPipelineYmlStructure) => {
      draft.stages[xIndex].splice(yIndex, 1);
      if (draft.stages[xIndex].length === 0) {
        draft.stages.splice(xIndex, 1);
      }
    });
    updater.ymlObj(newYmlObj);
    message.success(i18n.t('dop:please click save to submit the configuration'));
  };

  const commitData = (commitContent: string, values: any) => {
    const { path } = tree;
    const { branch, message: commitMsg } = values;

    commit({
      branch,
      message: commitMsg || `Update ${fileName}`,
      actions: [
        {
          content: commitContent,
          path,
          action: 'update',
          pathType: 'blob',
        },
      ],
    }).then(() => {
      onCancel();
      message.success(i18n.t('dop:file modified successfully'));
      getRepoBlob();
    });
  };

  const handleSubmit = (values: any) => {
    let pass = true as any;
    if (viewType === ViewType.code) {
      // 转图编辑，检测value
      pass = onYmlStringChange(txtValue, false);
    } else if (viewType === ViewType.graphic) {
      // 转文本
      const ymlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade']));
      pass = onYmlStringChange(ymlStr, false);
    }
    if (isPromise(pass)) {
      pass.then((ymlStr: string) => {
        commitData(ymlStr as string, values);
      });
    }
  };

  const checkForm = () => {
    form.validateFields().then((values: any) => {
      handleSubmit(values);
      form.resetFields();
    });
  };

  const onCancel = () => {
    changeMode({ editFile: false, addFile: false });
  };

  const onAddData = (addData: any) => {
    const { node, data } = addData;
    const curChosenExternal = get(node, externalKey);
    if (curChosenExternal) {
      const { resource, action = {}, executionCondition } = data;
      const newData = { ...resource, logoUrl: action.logoUrl, displayName: action.displayName };
      if (executionCondition) {
        // 执行条件
        newData.if = executionCondition;
      }
      const { nodeType, xIndex, yIndex, insertPos } = curChosenExternal;
      const newYmlObj = produce(ymlObj, (draft: IPipelineYmlStructure) => {
        if (!draft.stages) {
          draft.stages = [];
        }
        if (nodeType === NodeType.addRow) {
          draft.stages.splice(insertPos, 0, [newData]);
        } else if (nodeType === NodeType.addNode) {
          // 添加节点
          draft.stages[xIndex] = [...draft.stages[xIndex], newData];
        } else {
          // 修改节点
          draft.stages[xIndex][yIndex] = newData;
        }
      });
      updater.ymlObj(newYmlObj);
      message.success(i18n.t('dop:please click save to submit the configuration'));
    }
  };

  const renderSaveBtn = () => {
    if (!editing) return null;
    const getFieldsList = () => {
      const { branch } = getInfoFromRefName(tree.refName);
      const fieldsList = [
        {
          name: 'message',
          type: 'textArea',
          itemProps: {
            placeholder: i18n.t('dop:submit information'),
            autoSize: { minRows: 3, maxRows: 7 },
            maxLength: 200,
          },
          initialValue: `Update ${fileName}`,
        },
        {
          name: 'branch',
          type: 'select',
          initialValue: branch,
          options: (info.branches || []).map((a: any) => ({ name: a, value: a })),
          itemProps: {
            placeholder: i18n.t('dop:submit branch'),
            disabled: true,
          },
        },
      ];
      return fieldsList;
    };

    return (
      <>
        <RenderForm ref={formRef} className="commit-file-form" list={getFieldsList()} />
        <div className="p-4">
          <Button type="primary" className="mr-3" onClick={checkForm}>
            {i18n.t('save')}
          </Button>
          <Button
            onClick={() => {
              resetToOrigin();
              onCancel();
            }}
          >
            {i18n.t('cancel')}
          </Button>
        </div>
      </>
    );
  };

  const resetToOrigin = () => {
    update({
      pipelineYmlStructure: originPipelineYmlStructure,
      txtValue: blob.content,
    });
  };

  const reset = () => {
    const ymlStr = yaml.dump(omit(pipelineYmlStructure, ['upgradedYmlContent', 'needUpgrade']));
    onYmlStringChange(ymlStr);
  };

  const resetAndChangeViewType = () => {
    const ymlStr = yaml.dump(omit(pipelineYmlStructure, ['upgradedYmlContent', 'needUpgrade']));
    const pass = onYmlStringChange(ymlStr);
    if (isPromise(pass)) {
      pass.then(() => {
        update({
          errorMsg: '',
          viewType: viewType === ViewType.graphic ? ViewType.code : ViewType.graphic,
        });
      });
    }
  };

  const onYmlStringChange = (val: string, tipWithModal = true) => {
    try {
      yaml.load(val);
    } catch (e) {
      const msg = `${i18n.t('dop:input format error')}：${e.message}`;
      if (!tipWithModal) {
        notify('error', <pre className="prewrap">{msg}</pre>);
      } else {
        updater.errorMsg(`${i18n.t('dop:input format error')}：${e.message}`);
      }
      return false;
    }
    return parsePipelineYmlStructure({ pipelineYmlContent: val }).then((res: any) => {
      const ymlStr = get(res, 'data.ymlContent');
      update({
        ymlObj: res && res.data,
        txtValue: ymlStr,
      });
      return ymlStr;
    });
  };

  const changeViewType = (_type: string) => {
    let pass = true as any;
    let ymlStr = '';
    if (_type === ViewType.graphic) {
      // 转图编辑，检测value
      pass = onYmlStringChange(txtValue);
    } else if (_type === ViewType.code) {
      // 转文本
      ymlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade']));
      pass = onYmlStringChange(ymlStr);
    }
    if (isPromise(pass)) {
      pass
        .then(() => {
          updater.viewType(_type);
        })
        .catch(() => {
          // 错误情况下，允许切到code编辑
          if (_type === ViewType.code) {
            update({ txtValue: ymlStr, viewType: _type });
          }
        });
    }
  };

  const editOps = (
    <>
      <Radio.Group
        className="flex justify-between items-center"
        size="small"
        value={viewType}
        onChange={(e: any) => changeViewType(e.target.value)}
      >
        <Radio.Button value={ViewType.graphic}>
          <CustomIcon type="lc" />
        </Radio.Button>
        <Radio.Button value={ViewType.code}>
          <CustomIcon type="html1" />
        </Radio.Button>
      </Radio.Group>
      <Tooltip title={i18n.t('reset')}>
        <Popconfirm title={i18n.t('confirm to reset?')} onConfirm={reset} placement="bottom">
          <CustomIcon type="zhongzhi" className="ml-2 cursor-pointer" />
        </Popconfirm>
      </Tooltip>
    </>
  );

  return (
    <div>
      <FileContainer
        className={`new-yml-editor app-repo-pipeline flex flex-col justify-center full-spin-height ${
          viewType === ViewType.graphic ? 'graphic' : ''
        }`}
        name={editing ? `${i18n.t('edit')} ${fileName}` : fileName}
        ops={editing ? editOps : ops}
      >
        <Spin spinning={loading}>
          {viewType === ViewType.graphic ? (
            <PipelineGraphicEditor
              ymlObj={ymlObj as PIPELINE.IPipelineYmlStructure}
              editing={editing}
              onDeleteData={onDeleteData}
              addDrawerProps={{ scope: curScope }}
              onAddData={onAddData}
              chartProps={{
                nodeEleMap: {
                  startNode: () => <NodeEleMap.startNode disabled />,
                  endNode: () => <NodeEleMap.endNode disabled />,
                },
              }}
            />
          ) : (
            <FileEditor
              name={fileName}
              fileExtension={'xml'}
              value={txtValue}
              key={`${editing}`}
              focus={editing ? true : undefined}
              readOnly={editing ? undefined : true}
              minLines={8}
              onChange={(val: string) => updater.txtValue(val)}
            />
          )}
          {renderSaveBtn()}
        </Spin>
      </FileContainer>
      <Modal
        visible={!isEmpty(errorMsg)}
        title={
          <div>
            <CustomIcon type="guanbi-fill" className="text-danger" />
            {i18n.t('error')}
          </div>
        }
        maskClosable={false}
        footer={[
          <Button
            key="cancel"
            onClick={() => {
              updater.errorMsg('');
            }}
          >
            {i18n.t('cancel')}
          </Button>,
          ...insertWhen(originYmlValid, [
            <Button key="ok" type="primary" onClick={() => resetAndChangeViewType()}>
              {i18n.t('dop:reset and switch')}
            </Button>,
          ]),
        ]}
      >
        <pre className="prewrap">{errorMsg}</pre>
      </Modal>
    </div>
  );
};

export default PipelineEditor;
