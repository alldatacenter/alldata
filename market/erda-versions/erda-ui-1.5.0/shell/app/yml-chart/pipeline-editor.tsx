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
import { get, omit, isEmpty, cloneDeep } from 'lodash';
import { notify, isPromise } from 'common/utils';
import { Spin, Button, message, Radio, Modal } from 'antd';
import { FileEditor, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import FileContainer from 'application/common/components/file-container';
import { NodeType } from './config';
import { externalKey } from './chart';
import { produce } from 'immer';
import { insertWhen } from 'common/utils';
import { PipelineGraphicEditor, IPipelineGraphicEditorProps, IChartProps } from './common/pipeline-graphic-editor';
import { parsePipelineYmlStructure } from 'application/services/repo';
import './pipeline-editor.scss';

enum ViewType {
  graphic = 'graphic',
  code = 'code',
}

interface IPipelineEditorProps {
  YmlGraphicEditor?: (props: IPipelineGraphicEditorProps) => JSX.Element;
  title?: string;
  ymlStr: string;
  viewType?: string;
  addDrawerProps?: Obj;
  editable: boolean;
  loading?: boolean;
  onChangeViewType?: (arg: string) => void;
  onSubmit?: (arg: string) => any;
  chartProps?: IChartProps;
}

const PipelineEditor = React.forwardRef((props: IPipelineEditorProps, ref: any) => {
  const {
    ymlStr: propsYmlStr,
    onChangeViewType,
    onSubmit,
    title,
    loading = false,
    YmlGraphicEditor = PipelineGraphicEditor,
    editable = true,
    addDrawerProps,
    ...rest
  } = props;
  const editorRef = React.useRef(null as any);
  const [{ ymlObj, editing, viewType, ymlStr, errorMsg, originYmlObj, originYmlStr, originYmlValid }, updater, update] =
    useUpdate({
      ymlObj: {} as PIPELINE.IPipelineYmlStructure,
      viewType: ViewType.graphic,
      ymlStr: '',
      originYmlValid: true,
      errorMsg: '',
      editing: false,
      originYmlObj: {} as PIPELINE.IPipelineYmlStructure,
      originYmlStr: propsYmlStr,
    });

  React.useEffect(() => {
    if (propsYmlStr) {
      updater.originYmlStr(propsYmlStr);
    }
  }, [propsYmlStr, updater]);

  React.useEffect(() => {
    if (ref) {
      // eslint-disable-next-line no-param-reassign
      ref.current = editorRef.current;
    }
  }, [ref, ymlObj, ymlStr]);

  React.useEffect(() => {
    if (originYmlStr) {
      (parsePipelineYmlStructure({ pipelineYmlContent: originYmlStr }) as unknown as Promise<any>)
        .then((res: any) => {
          update({
            ymlObj: res && res.data,
            originYmlObj: res && res.data,
          });
        })
        .catch(() => {
          update({
            ymlStr: originYmlStr,
            viewType: ViewType.code,
            originYmlValid: false,
          });
        });
    } else {
      update({
        ymlObj: {},
        ymlStr: originYmlStr,
        originYmlObj: {},
        // viewType: ViewType.graphic,
      });
    }
  }, [update, updater, originYmlStr]);

  React.useEffect(() => {
    onChangeViewType && onChangeViewType(viewType);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewType]);

  const onDeleteData = (nodeData: any) => {
    const { [externalKey]: externalData } = nodeData;
    const { xIndex, yIndex } = externalData || {};

    const newYmlObj = cloneDeep(ymlObj);
    newYmlObj.stages[xIndex].splice(yIndex, 1);
    if (newYmlObj.stages[xIndex].length === 0) {
      newYmlObj.stages.splice(xIndex, 1);
    }

    updater.ymlObj(newYmlObj);
    message.success(i18n.t('dop:please click save to submit the configuration'));
  };

  const handleSubmit = () => {
    let pass = true as any;
    if (viewType === ViewType.code) {
      // 转图编辑，检测value
      pass = onYmlStringChange(ymlStr, false);
    } else if (viewType === ViewType.graphic) {
      // 转文本
      const curYmlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade', 'ymlContent']));
      pass = onYmlStringChange(curYmlStr, false);
    }
    if (isPromise(pass)) {
      pass.then((curYmlStr: string) => {
        if (onSubmit) {
          const submitRes = onSubmit(curYmlStr);
          if (isPromise(submitRes)) {
            submitRes.then(() => {
              updater.editing(false);
            });
          }
        }
      });
    }
  };

  // 添加出参
  const onAddOutParams = (outP: PIPELINE.IPipelineOutParams[]) => {
    const newYmlObj = produce(ymlObj, (draft: PIPELINE.IPipelineYmlStructure) => {
      draft.outputs = outP;
    });
    updater.ymlObj(newYmlObj);
    message.success(i18n.t('dop:please click save to submit the configuration'));
  };

  // 添加出参
  const onAddInParams = (inP: PIPELINE.IPipelineInParams[]) => {
    const newYmlObj = produce(ymlObj, (draft: PIPELINE.IPipelineYmlStructure) => {
      draft.params = inP;
    });
    updater.ymlObj(newYmlObj);
    message.success(i18n.t('dop:please click save to submit the configuration'));
  };

  // 添加普通节点
  const onAddData = (addData: any) => {
    const { node, data } = addData;
    const curChosenExternal = get(node, externalKey);
    if (curChosenExternal) {
      const { resource, action = {}, executionCondition } = data;
      let newData = {} as PIPELINE.IStageTask;
      if (resource) {
        newData = { ...resource, logoUrl: action.logoUrl, displayName: action.displayName };
      } else {
        newData = { ...data };
      }

      if (executionCondition) {
        // 执行条件
        newData.if = executionCondition;
      }

      const { nodeType, xIndex, yIndex, insertPos } = curChosenExternal;
      const newYmlObj = cloneDeep(ymlObj);
      if (!newYmlObj.stages) {
        newYmlObj.stages = [];
      }

      if (nodeType === NodeType.addRow) {
        newYmlObj.stages.splice(insertPos, 0, [newData]);
      } else if (nodeType === NodeType.addNode) {
        // 添加节点
        newYmlObj.stages[xIndex] = [...newYmlObj.stages[xIndex], newData];
      } else {
        // 修改节点
        newYmlObj.stages[xIndex][yIndex] = newData;
      }

      updater.ymlObj(newYmlObj);
      message.success(i18n.t('dop:please click save to submit the configuration'));
    }
  };

  const resetToOrigin = () => {
    update({
      ymlObj: originYmlObj,
      ymlStr: originYmlStr,
    });
  };

  const resetAndChangeViewType = () => {
    const curYmlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade', 'ymlContent']));
    const pass = onYmlStringChange(curYmlStr) as unknown as Promise<any>;
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
    return (parsePipelineYmlStructure({ pipelineYmlContent: val }) as unknown as Promise<any>).then((res: any) => {
      const curYmlStr = get(res, 'data.ymlContent');
      update({
        ymlObj: res && res.data,
        ymlStr: curYmlStr,
      });
      return curYmlStr;
    });
  };

  const changeViewType = (_type: ViewType) => {
    let pass = true as any;
    let curYmlStr = '';
    if (!editing && !originYmlValid) {
      updater.viewType(_type);
      return;
    }
    if (_type === ViewType.graphic) {
      // 转图编辑，检测value
      pass = onYmlStringChange(ymlStr);
    } else if (_type === ViewType.code) {
      // 转文本
      curYmlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade', 'ymlContent']));
      pass = onYmlStringChange(curYmlStr);
    }
    if (isPromise(pass)) {
      pass
        .then(() => {
          updater.viewType(_type);
        })
        .catch(() => {
          // 错误情况下，允许切到code编辑
          if (_type === ViewType.code) {
            update({ ymlStr: curYmlStr, viewType: _type });
          }
        });
    }
  };

  const onCancel = () => {
    resetToOrigin();
    updater.editing(false);
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
          <ErdaIcon className="hover mt-0.5" width="20" height="21" fill="black-400" type="lc" />
        </Radio.Button>
        <Radio.Button value={ViewType.code}>
          <ErdaIcon className="hover mt-0.5" width="20" height="21" fill="black-400" type="html1" />
        </Radio.Button>
      </Radio.Group>
      {!editing ? (
        <Button disabled={!editable} onClick={() => updater.editing(true)} className="ml-2" size="small">
          {i18n.t('edit')}
        </Button>
      ) : (
        <div className="px-3 py-2">
          <Button onClick={onCancel} size="small">
            {i18n.t('cancel')}
          </Button>
          <Button onClick={handleSubmit} type="primary" className="ml-2" size="small">
            {i18n.t('save')}
          </Button>
        </div>
      )}
    </>
  );

  const name = title ? (editing ? `${i18n.t('edit')} ${title}` : title) : '';

  return (
    <div>
      <FileContainer
        className={`pipeline-yml-editor flex flex-col justify-center full-spin-height ${
          viewType === ViewType.graphic ? 'graphic' : ''
        }`}
        name={name}
        ops={editOps}
      >
        <Spin spinning={loading}>
          {viewType === ViewType.graphic ? (
            <YmlGraphicEditor
              {...rest}
              ymlObj={ymlObj as PIPELINE.IPipelineYmlStructure}
              editing={editing}
              addDrawerProps={addDrawerProps}
              onDeleteData={onDeleteData}
              onAddData={onAddData}
              onAddInParams={onAddInParams}
              onAddOutParams={onAddOutParams}
            />
          ) : (
            <FileEditor
              name={name}
              fileExtension={'xml'}
              value={ymlStr}
              key={`${editing}`}
              focus={editing ? true : undefined}
              readOnly={editing ? undefined : true}
              minLines={8}
              onChange={(val: string) => updater.ymlStr(val)}
            />
          )}
        </Spin>
      </FileContainer>
      <Modal
        visible={!isEmpty(errorMsg)}
        title={
          <div>
            <ErdaIcon size="16" type="guanbi-fill" fill="danger" />
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
});

export default PipelineEditor;
