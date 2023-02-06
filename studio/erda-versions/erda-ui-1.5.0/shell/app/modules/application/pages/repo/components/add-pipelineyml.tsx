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
import { Button, message, Radio, Alert, Modal, Tooltip, Input } from 'antd';
import { RenderForm, Icon as CustomIcon, CardsLayout, IF, FileEditor } from 'common';
import { useUpdate } from 'common/use-hooks';
import { notify, isPromise } from 'common/utils';
import FileContainer from 'application/common/components/file-container';
import { getInfoFromRefName } from '../util';
import { isEmpty, find, get, omit } from 'lodash';
import { useMount } from 'react-use';
import { PipelineGraphicEditor } from 'yml-chart/common/pipeline-graphic-editor';
import { NodeEleMap, externalKey, NodeType } from 'yml-chart/config';
import { produce } from 'immer';
import yaml from 'js-yaml';
import i18n from 'i18n';
import { parsePipelineYmlStructure } from 'application/services/repo';
import repoStore from 'application/stores/repo';
import './add-pipelineyml.scss';

interface IState {
  value: string;
  fileName: string;
  viewType: string;
  txtValue: string;
  ymlObj: null | IPipelineYmlStructure;
  fileSuffix: string;
  errorMsg: string;
}

enum ViewType {
  graphic = 'graphic',
  code = 'code',
}

const pipelineTempQuery = { scopeID: '0', scopeType: 'dice' };

const AddPipelineYml = () => {
  const [tree, info] = repoStore.useStore((s) => [s.tree, s.info]);
  const { commit, getRepoTree } = repoStore.effects;
  const { changeMode } = repoStore.reducers;
  const isRootPath = tree && tree.path === ''; // 是根目录

  const editViewRef = React.useRef(null as any);
  const selectorRef = React.useRef(null as any);

  const [{ txtValue, fileName, viewType, ymlObj, errorMsg, fileSuffix }, updater, update] = useUpdate({
    txtValue: '', // 文本编辑值
    fileName: isRootPath ? 'pipeline' : '',
    fileSuffix: '.yml',
    viewType: ViewType.graphic,
    ymlObj: {},
    errorMsg: '',
  } as IState);

  const onYmlStringChange = (val: string, tipWithModal = true) => {
    try {
      yaml.load(val);
    } catch (e) {
      const msg = `${i18n.t('dop:input format error')}：${e.message}`;
      if (tipWithModal) {
        updater.errorMsg(msg);
      } else {
        notify('error', <pre className="prewrap">{msg}</pre>);
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

  const resetAndChangeViewType = () => {
    const ymlStr = selectorRef && selectorRef.current.currentTemplate;
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

  const changeViewType = (_type: string) => {
    let pass = true as any;
    if (_type === ViewType.graphic) {
      // 转图编辑，检测value
      pass = onYmlStringChange(txtValue);
    } else if (_type === ViewType.code) {
      // 转文本
      const ymlStr = yaml.dump(omit(ymlObj, ['upgradedYmlContent', 'needUpgrade']));
      pass = onYmlStringChange(ymlStr);
    }
    if (isPromise(pass)) {
      pass.then(() => {
        updater.viewType(_type);
      });
    }
  };

  const handleSubmit = (form: FormInstance) => {
    const path = `${tree.path ? `${tree.path}/` : ''}${fileName}${fileSuffix}`;
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
        try {
          yaml.load(ymlStr);
        } catch (e) {
          notify('error', <pre className="prewrap">{`${i18n.t('dop:input format error')}：${e.message}`}</pre>);
          return;
        }

        form.validateFields().then((values: Pick<REPOSITORY.Commit, 'message' | 'branch'>) => {
          commit({
            ...values,
            actions: [
              {
                action: 'add',
                content: ymlStr,
                path,
                pathType: 'blob',
              },
            ],
          }).then((res: any) => {
            if (res.success) {
              cancelEdit();
              message.success(i18n.t('dop:file created successfully'));
              getRepoTree({ force: true });
            }
          });
        });
      });
    }
  };

  const cancelEdit = () => {
    changeMode({ editFile: false, addFile: false, addFileName: '' });
  };

  const getFieldsList = () => {
    const { branch } = getInfoFromRefName(tree.refName);
    const branches = info.branches.length ? info.branches : ['master'];
    const fieldsList = [
      {
        name: 'message',
        type: 'textArea',
        itemProps: {
          placeholder: i18n.t('dop:submit information'),
          maxLength: 200,
          autoSize: { minRows: 3, maxRows: 7 },
        },
        initialValue: `Add ${fileName}${fileSuffix}`,
      },
      {
        name: 'branch',
        type: 'select',
        initialValue: branch || 'master',
        options: branches.map((a: string) => ({ name: a, value: a })),
        itemProps: {
          placeholder: i18n.t('dop:submit branch'),
          disabled: true,
        },
      },
      {
        getComp: ({ form }: { form: FormInstance }) => (
          <div>
            <Button type="primary" onClick={() => handleSubmit(form)}>
              {i18n.t('save')}
            </Button>
            <Button className="ml-3" onClick={cancelEdit}>
              {i18n.t('cancel')}
            </Button>
          </div>
        ),
      },
    ];
    return fieldsList;
  };

  const reset = () => {
    const ymlStr = selectorRef && selectorRef.current.currentTemplate;
    onYmlStringChange(ymlStr);
  };

  const ops = (
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
        <CustomIcon type="zhongzhi" className="ml-2 cursor-pointer" onClick={reset} />
      </Tooltip>
    </>
  );

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

  const chartData = React.useMemo(() => {
    return get(ymlObj, 'stages') || [];
  }, [ymlObj]);

  return (
    <div className="repo-add-pipelineyml">
      <Alert message={i18n.t('dop:add-pipeline-tip')} type="info" showIcon />
      <PipelineTemplateSelector
        ref={selectorRef}
        onChange={(val: string) => {
          onYmlStringChange(val);
          editViewRef && editViewRef.current && editViewRef.current.scrollIntoView(true);
        }}
      />
      <div ref={editViewRef}>
        <div className="font-bold text-base my-3">{i18n.t('dop:pipeline configuration')}</div>
        <FileContainer
          name={
            <FileNameInput
              value={fileName}
              disabled={isRootPath}
              onChange={(val: string) => updater.fileName(val)}
              addonAfter={fileSuffix}
            />
          }
          ops={ops}
        >
          <div className="add-pipeline-file-container">
            <IF check={viewType === ViewType.graphic}>
              <PipelineGraphicEditor
                ymlObj={ymlObj as PIPELINE.IPipelineYmlStructure}
                editing
                onDeleteData={onDeleteData}
                onAddData={onAddData}
                chartProps={{
                  nodeEleMap: {
                    startNode: () => <NodeEleMap.startNode disabled />,
                    endNode: () => <NodeEleMap.endNode disabled />,
                  },
                }}
              />
            </IF>
            <IF check={viewType === ViewType.code}>
              <FileEditor
                name={`${fileName}${fileSuffix}`}
                fileExtension={'xml'}
                value={txtValue}
                focus
                minLines={8}
                onChange={(val: string) => updater.txtValue(val)}
              />
            </IF>
          </div>
          <RenderForm className="p-4 border-top" list={getFieldsList()} />
        </FileContainer>
      </div>
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
          <Button key="ok" type="primary" onClick={() => resetAndChangeViewType()}>
            {i18n.t('dop:reset and switch')}
          </Button>,
        ]}
      >
        <pre className="prewrap">{errorMsg}</pre>
      </Modal>
    </div>
  );
};

export default AddPipelineYml;

interface ITemplateSelector {
  onChange: (val: any) => void;
}

const PipelineTemplateSelector = React.forwardRef((props: ITemplateSelector, ref: any) => {
  const { onChange } = props;
  const [value, setValue] = React.useState(-1);
  const [ymlContentMap, setYmlContentMap] = React.useState({});
  const pipelineTemplates = repoStore.useStore((s) => s.pipelineTemplates);
  const { getPipelineTemplates, getPipelineTemplateYmlContent } = repoStore.effects;

  useMount(() => {
    if (isEmpty(pipelineTemplates)) {
      getPipelineTemplates(pipelineTempQuery);
    }
  });

  React.useEffect(() => {
    if (ref) {
      // eslint-disable-next-line no-param-reassign
      ref.current = {
        currentTemplate: ymlContentMap[value],
      };
    }
  }, [ymlContentMap, value, ref]);

  React.useEffect(() => {
    if (!isEmpty(pipelineTemplates)) {
      changeValue(get(pipelineTemplates, '[0].id'));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pipelineTemplates]);

  const changeValue = (id: number) => {
    setValue(id);
    if (ymlContentMap[id]) {
      return onChange(ymlContentMap[id]);
    }
    const curTemplate = find(pipelineTemplates, { id });
    if (curTemplate) {
      const { name, version } = curTemplate;
      getPipelineTemplateYmlContent({ name, version, ...pipelineTempQuery }).then((res) => {
        onChange(res.pipelineYaml);
        setYmlContentMap({ ...ymlContentMap, [id]: res.pipelineYaml });
      });
    }
  };

  const templateRender = (template: REPOSITORY.IPipelineTemplate) => {
    const { id, logoUrl, name, desc } = template;
    return (
      <div
        key={template.id}
        className={`pipeline-template-item rounded p-4 ${value === id ? 'active-item' : ''}`}
        onClick={() => changeValue(id)}
      >
        <div className="logo">
          {logoUrl ? <img src={logoUrl} /> : <CustomIcon type="dm" className="template-icon" />}
        </div>
        <div className="name my-1">{name}</div>
        <div className="desc">{desc}</div>
      </div>
    );
  };

  return (
    <div className="pipeline-template">
      <div className="font-bold text-base my-3">{i18n.t('dop:template select')}</div>
      <CardsLayout dataList={pipelineTemplates} contentRender={templateRender} />
    </div>
  );
});

interface IFileNameProps {
  [pro: string]: any;
  value: string;
  onChange: Function;
  disabled: boolean;
}
const FileNameInput = ({ value, onChange, disabled, ...rest }: IFileNameProps) => {
  return (
    <Input
      style={{ width: 200 }}
      value={value}
      placeholder={i18n.t('please enter {name}', { name: i18n.t('dop:file name') })}
      disabled={disabled}
      onChange={(e: any) => {
        onChange(e.target.value.trim());
      }}
      {...rest}
    />
  );
};
