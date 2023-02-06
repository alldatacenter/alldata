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

import yaml from 'js-yaml';
import CreateAddOn from 'application/common/components/create-add-on';
import DiceYamlEditor from 'application/common/components/dice-yaml-editor';
import Drawer from 'application/common/components/dice-yaml-editor-drawer';
import DiceYamlEditorItem, { IDiceYamlEditorItem } from 'application/common/components/dice-yaml-editor-item';
import { DiceFlowType } from 'application/common/components/dice-yaml-editor-type';
import EditService from 'application/common/components/edit-service';
import EditStage from 'application/common/components/edit-stage';
import FileContainer from 'application/common/components/file-container';
import convertByDiceYml from 'application/common/dice-yml-data-convert';
import convertByPipelineYml from 'application/common/pipeline-yml-data-convert';
import {
  clearNullValue,
  getAddonPlanCN,
  getWorkFlowType,
  WORK_FLOW_TYPE,
  isPipelineWorkflowYml,
} from 'application/common/yml-flow-util';
import { getInfoFromRefName } from 'application/pages/repo/util';
import classnames from 'classnames';
import { FormModal, RenderForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo, notify } from 'common/utils';

import { cloneDeep, filter, find, findIndex, forEach, get, omit, isEmpty } from 'lodash';
import { Button, message, Spin } from 'antd';
import React from 'react';
import { useMount } from 'react-use';
import i18n from 'i18n';
import appDeployStore from 'application/stores/deploy';
import './yml-editor.scss';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';
// import routeInfoStore from 'core/stores/route';

export interface IProps {
  content: string;
  fileName: string;
  ops: any;
  editing: boolean;
}

interface IBlockContainerProps {
  title: string;
  className?: string;
  children?: any;
}
const BlockContainer = (props: IBlockContainerProps) => {
  return (
    <div className={classnames('yaml-editor-block-container', props.className)}>
      <span className="yaml-editor-block-title">{props.title}</span>
      <div className="yaml-editor-block-content">{props.children}</div>
    </div>
  );
};
const defaultJson = {
  services: {},
  envs: {},
  addons: {},
  version: '1.0',
};
const emptyObj = {};

const YmlEditor = (props: IProps) => {
  // const params = routeInfoStore.useStore(s => s.params);
  const [info, tree, propsGroupedAddonList, pipelineYmlStructure] = repoStore.useStore((s) => [
    s.info,
    s.tree,
    s.groupedAddonList,
    s.pipelineYmlStructure,
  ]);
  const { getAvailableAddonList, getAddonVersions, commit, getRepoBlob, parsePipelineYmlStructure } = repoStore.effects;
  const { changeMode } = repoStore.reducers;
  const [isFetchCommit, isFetchAddon] = useLoading(repoStore, ['commit', 'getAvailableAddonList']);
  const isFetching = isFetchCommit || isFetchAddon;
  const [state, updater] = useUpdate({
    jsonContent: defaultJson,
    originJsonContent: defaultJson,
    modalVisible: false,
    editorData: [],
    addons: [],
    openDrawer: false,
    drawerTitle: i18n.t('edit') as string,
    selectedAddon: null,
    groupedAddonList: [],
    editedYmlStructure: null as null | IPipelineYmlStructure,
  });

  const { fileName, content, editing, ops } = props;

  const {
    openDrawer,
    drawerTitle,
    modalVisible,
    selectedAddon,
    editorData,
    addons,
    jsonContent,
    originJsonContent,
    editedYmlStructure,
    groupedAddonList,
  } = state;

  const actions = appDeployStore.useStore((s) => s.actions);
  const { getActionConfigs, getActions } = appDeployStore.effects;

  const formRef: any = React.useRef(null);
  const form = formRef.current;

  const selectedItemRef = React.useRef(null as any);

  const fileType = React.useRef(null as any as WORK_FLOW_TYPE);

  const convertYamlToJson = React.useCallback(() => {
    try {
      let loadedContent = yaml.load(content);
      loadedContent = initYmlDefaultFields(loadedContent === 'undefined' ? emptyObj : loadedContent);
      updater.originJsonContent(loadedContent);
    } catch (e) {
      notify('error', `${i18n.t('dop:yml format error')}：${e.message}`);
      updater.jsonContent(defaultJson);
    }
  }, [content, updater]);

  const closedDrawer = React.useRef(() => {
    selectedItemRef.current = null;
    updater.selectedAddon(null);
    updater.openDrawer(false);
  });

  const editGlobalVariable = React.useRef((json) => (globalVariable: any) => {
    // if (editedYmlStructure) {
    //   editedYmlStructure.envs = globalVariable;
    // } else {
    //   jsonContent.envs = globalVariable;
    // }
    // eslint-disable-next-line no-param-reassign
    json.envs = globalVariable;

    convertDataByFileName(json, json);
    message.success(i18n.t('dop:please click save to submit the configuration'));
    closedDrawer?.current();
  });

  const editService = React.useRef(
    (service: any, inputJson: any, parentName?: string, _convertDataByFileName?: Function) => {
      const inputJsonCp = cloneDeep(inputJson);
      if (inputJsonCp.services[service.name]) {
        // delete service.originName;
        inputJsonCp.services[service.name] = {
          ...inputJsonCp.services[service.name],
          ...omit(service, ['originName']),
        };

        delete inputJsonCp.services[service.name].name;
        _convertDataByFileName?.(inputJsonCp);
      } else {
        if (parentName) {
          if (!inputJsonCp.services[parentName].depends_on) {
            inputJsonCp.services[parentName].depends_on = [];
          }

          inputJsonCp.services[parentName].depends_on.push(service.name);
        }

        if (service.originName !== service.name) {
          const findResult = find(
            inputJsonCp.services,
            (item: any) => item.depends_on && item.depends_on.includes(service.originName),
          );
          if (findResult) {
            // @ts-ignore
            const index = findIndex(findResult.depends_on, (item: string) => item === service.originName);
            // @ts-ignore
            findResult.depends_on.splice(index, 1);
            // @ts-ignore
            findResult.depends_on.push(service.name);
          }
        }

        if (service.originName && service.name) {
          delete inputJsonCp.services[service.originName];
          // eslint-disable-next-line no-param-reassign
          delete service.originName;
          inputJsonCp.services[service.name] = service;
          delete inputJsonCp.services[service.name].name;
        } else if (!service.name) {
          // eslint-disable-next-line no-param-reassign
          delete service.originName;
          inputJsonCp.services[service.originName] = service;
          delete inputJsonCp.services[service.originName].originName;
        }
        _convertDataByFileName?.(inputJsonCp);
      }

      message.success(i18n.t('dop:please click save to submit the configuration'));
      closedDrawer?.current();
    },
  );

  const editPipelineConvertor = React.useCallback(
    (inputStructure, _convertDataByFileName) => (formTaskData: any) => {
      const { resource } = formTaskData;
      const selected = selectedItemRef.current;
      if (!selected || (!editedYmlStructure && !inputStructure)) {
        return;
      }
      const isCreate = selected.status === 'new';
      const structure = isEmpty(editedYmlStructure) ? inputStructure : editedYmlStructure;
      const { groupIndex, index } = selected;
      if (!structure.stages) {
        structure.stages = [];
      }
      let targetStage = structure.stages[groupIndex - 1];
      if (!targetStage && isCreate) {
        structure.stages.push([]);
        targetStage = structure.stages[groupIndex - 1];
      }
      if (isCreate) {
        targetStage.push(resource);
      } else {
        targetStage[index] = resource;
      }

      _convertDataByFileName(null, structure);
      updater.editedYmlStructure(structure);
      message.success(i18n.t('dop:please click save to submit the configuration'));
      closedDrawer?.current();
    },
    [editedYmlStructure, updater],
  );

  const convertDataByFileName: any = React.useCallback(
    (inputJsonContent: any, inputEditedYmlStructure?: IPipelineYmlStructure | null): void => {
      let result: any = {};
      const _editServie = (_service: Obj, _inputJson: Obj, _parentName?: string) => {
        editService?.current(_service, _inputJson, _parentName, convertDataByFileName);
      };
      const newContent = cloneDeep(inputJsonContent);
      switch (fileType.current) {
        case WORK_FLOW_TYPE.DICE:
          result = convertByDiceYml({
            jsonContent: inputJsonContent,
            editService: _editServie,
            editGlobalVariable: (p) => editGlobalVariable?.current(inputEditedYmlStructure || inputJsonContent)(p),
          });
          break;
        case WORK_FLOW_TYPE.PIPELINE:
          result = convertByPipelineYml({
            title: i18n.t('pipeline'),
            actions,
            editConvertor: editPipelineConvertor(inputEditedYmlStructure || editedYmlStructure, convertDataByFileName),
            editGlobalVariable: (p) => editGlobalVariable?.current(inputEditedYmlStructure || inputJsonContent)(p),
            pipelineYmlStructure: (inputEditedYmlStructure || editedYmlStructure) as IPipelineYmlStructure,
          });
          break;
        case WORK_FLOW_TYPE.WORKFLOW:
          result = convertByPipelineYml({
            actions,
            editConvertor: editPipelineConvertor(inputEditedYmlStructure || editedYmlStructure, convertDataByFileName),
            editGlobalVariable: (p) => editGlobalVariable?.current(inputEditedYmlStructure || inputJsonContent)(p),
            pipelineYmlStructure: (inputEditedYmlStructure || editedYmlStructure) as IPipelineYmlStructure,
          });
          break;
        default:
      }

      updater.jsonContent(newContent);
      updater.editorData(cloneDeep(result.editorData));
      updater.addons(result.addons ? cloneDeep(result.addons) : []);
    },
    [actions, editPipelineConvertor, editedYmlStructure, updater],
  );

  const loadPipelineYmlStructure = React.useCallback(() => {
    const cloneEditedYmlStructure = cloneDeep(pipelineYmlStructure);
    updater.editedYmlStructure(cloneEditedYmlStructure);
    try {
      convertDataByFileName(null, cloneEditedYmlStructure);
    } catch (e) {
      notify('error', `${i18n.t('dop:yml format error')}：${e.message}`);
      updater.editedYmlStructure(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pipelineYmlStructure, updater]);

  useMount(() => {
    fileType.current = getWorkFlowType(fileName);
    if (fileType.current === WORK_FLOW_TYPE.DICE) {
      convertYamlToJson();
      updater.groupedAddonList(propsGroupedAddonList);
    }

    if (isPipelineWorkflowYml(fileName)) {
      getActions();
      loadPipelineYmlStructure();
    } else {
      getAvailableAddonList();
    }
  });

  React.useEffect(() => {
    fileType.current = getWorkFlowType(fileName);
  }, [fileName]);

  React.useEffect(() => {
    originJsonContent && convertDataByFileName(originJsonContent);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [originJsonContent]);

  React.useEffect(() => {
    loadPipelineYmlStructure();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pipelineYmlStructure]);

  React.useEffect(() => {
    if (fileType.current === WORK_FLOW_TYPE.DICE) {
      convertYamlToJson();
    }
  }, [convertYamlToJson]);

  React.useEffect(() => {
    updater.groupedAddonList(propsGroupedAddonList);
  }, [propsGroupedAddonList, updater]);

  React.useEffect(() => {
    updater.openDrawer(false);
  }, [props.editing, updater]);

  const toggleModal = (visible: boolean) => {
    updater.modalVisible(visible);
  };

  const handleDelete = (values: any) => {
    const { path } = tree;
    commit({
      ...values,
      actions: [
        {
          action: 'delete',
          path,
          pathType: 'blob',
        },
      ],
    }).then((res: any) => {
      toggleModal(false);
      if (res.success) {
        message.success(i18n.t('dop:file deleted successfully'));
        goTo('../');
      }
    });
  };

  const checkForm = () => {
    form.validateFields().then((values: any) => {
      handleSubmit(values);
      form.resetFields();
    });
  };

  const cancelEditing = () => {
    convertDataByFileName(originJsonContent);
    closedDrawer?.current();
    changeMode({ editFile: false, addFile: false });
  };

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

  const renderSaveBtn = () => {
    if (!editing) {
      return null;
    }

    return (
      <React.Fragment>
        <RenderForm ref={formRef} className="commit-file-form" list={getFieldsList()} />
        <div className="commit-file-form-container">
          <Button type="primary" className="mr-3" onClick={checkForm}>
            {i18n.t('save')}
          </Button>
          <Button onClick={cancelEditing}>{i18n.t('cancel')}</Button>
        </div>
      </React.Fragment>
    );
  };

  const deleteService = ({ name }: any) => {
    delete jsonContent.services[name];

    const findResult = filter(jsonContent.services, (item: any) => item.depends_on && item.depends_on.includes(name));
    if (findResult) {
      forEach(findResult, (findResultItem: any) => {
        // @ts-ignore
        const index = findIndex(findResultItem.depends_on, (i: string) => i === name);
        if (index !== -1) {
          findResultItem.depends_on.splice(index, 1);
        }
      });
    }

    closedDrawer?.current();
    convertDataByFileName(jsonContent);
  };

  const updateDiceYmlConnect = (fromService: string, toService: string) => {
    const findResult = find(jsonContent.services, (_item: any, name: string) => name === fromService);
    if (findResult) {
      // @ts-ignore
      const index = findIndex(findResult.depends_on, (item: string) => item === toService);
      if (index === -1) {
        // @ts-ignore
        if (findResult.depends_on) {
          // @ts-ignore
          findResult.depends_on.push(toService);
        } else {
          // @ts-ignore
          findResult.depends_on = [toService];
        }
      }
    }

    convertDataByFileName(jsonContent);
  };

  const deletePipelineStage = (item: any) => {
    if (!editedYmlStructure) {
      return;
    }
    editedYmlStructure.stages = editedYmlStructure.stages.reduce((result: any, stageTasks: any) => {
      const targetIndex = findIndex(stageTasks, (task: any) => {
        if (task.alias === item.data.alias) {
          return true;
        }
        return false;
      });
      if (targetIndex !== -1) {
        if (stageTasks.length === 1) {
          return result;
        } else {
          stageTasks.splice(targetIndex, 1);
        }
      }
      result.push(stageTasks);
      return result;
    }, [] as IStageTask[][]);

    selectedItemRef.current = null;
    updater.openDrawer(false);
    convertDataByFileName(null, editedYmlStructure);
  };

  const renderCreateComponent = (service: any, parentName: string) => {
    // eslint-disable-next-line no-param-reassign
    service.editView = (isEditing: boolean) => {
      return (
        <EditService
          editing={isEditing}
          service={service}
          onSubmit={(newService: any) => editService?.current(newService, jsonContent, parentName)}
        />
      );
    };
    selectedItemRef.current = service;
    // updater.selectedItem(service);
    updater.openDrawer(true);
    updater.drawerTitle(i18n.t('edit'));
  };

  const renderCreatePipelineComponent = (stageTask: any) => {
    const pipelineTaskAlias = (get(editedYmlStructure as IPipelineYmlStructure, 'stages') || []).reduce(
      (acc: string[], stage: any[]) => {
        const stageTaskAlias = stage.map((task) => task.alias);
        return acc.concat(stageTaskAlias);
      },
      [],
    );
    // eslint-disable-next-line no-param-reassign
    stageTask.editView = (isEditing: boolean) => {
      return (
        <EditStage
          editing={isEditing}
          actions={actions}
          isCreateTask
          task={stageTask}
          otherTaskAlias={pipelineTaskAlias}
          onSubmit={editPipelineConvertor(editedYmlStructure, convertDataByFileName)}
        />
      );
    };
    selectedItemRef.current = stageTask;
    updater.openDrawer(true);
    updater.drawerTitle(i18n.t('dop:add'));
  };

  const clickAddonItem = (item: IDiceYamlEditorItem) => {
    updater.selectedAddon(item);
    updater.openDrawer(true);
    updater.drawerTitle(editing ? i18n.t('edit') : i18n.t('dop:view'));
  };

  const showCreateAddonModal = () => {
    updater.selectedAddon({
      creatingAddon: true,
      editView: (isEditing: boolean) => {
        if (!isEditing) {
          return;
        }
        return (
          <CreateAddOn
            editing={isEditing}
            groupedAddonList={groupedAddonList}
            getAddonVersions={getAddonVersions}
            onSubmit={createdAddOn}
            cancel={closedDrawer?.current}
          />
        );
      },
    });
    updater.openDrawer(true);
    updater.drawerTitle(i18n.t('edit'));
  };

  const openDrawerForEditor = (item: any) => {
    if (editedYmlStructure) {
      const type = get(item, 'data.type');
      type && getActionConfigs({ actionType: type });
    }
    selectedItemRef.current = item;
    updater.openDrawer(true);
    updater.drawerTitle(editing ? i18n.t('edit') : i18n.t('dop:view'));
  };

  const createdAddOn = (options: any) => {
    jsonContent.addons[options.alias] = {
      plan: options.plan,
      options: {
        version: options.version,
      },
    };
    convertDataByFileName(jsonContent);
  };

  const deleteAddon = (addon: any) => {
    delete jsonContent.addons[addon.name];
    convertDataByFileName(jsonContent);
  };

  /**
   * 根据文件类型初始化默认值
   */
  const initYmlDefaultFields = (data?: any) => {
    const initContent: any = data || {};

    if (fileType.current === WORK_FLOW_TYPE.DICE) {
      if (!initContent.services) {
        initContent.services = {};
      }
      if (!initContent.addons) {
        initContent.addons = {};
      }
      if (!initContent.envs) {
        initContent.envs = {};
      }
    } else {
      if (!initContent.stages) {
        initContent.stages = [];
      }
      if (!initContent.resources) {
        initContent.resources = [];
      }
      if (!initContent.envs) {
        initContent.envs = {};
      }

      if (!initContent.version) {
        initContent.version = '1.0';
      }
    }
    return initContent;
  };

  const renderAddons = () => {
    return addons.map((addon: IDiceYamlEditorItem) => {
      const item = {
        ...addon,
        title: 'Add-On',
        editView: (isEditing: boolean) => (
          <CreateAddOn
            editing={isEditing}
            addOn={addon.data}
            cancel={closedDrawer?.current}
            getAddonVersions={getAddonVersions}
            groupedAddonList={groupedAddonList}
            onSubmit={(values: any) => editAddon(addon, values)}
          />
        ),
        content: () => {
          const version =
            addon.data.options && addon.data.options.version ? (
              <div>
                <span>{i18n.t('version')}：</span>
                <span>{addon.data.options ? addon.data.options.version : '-'}</span>
              </div>
            ) : null;
          return (
            <div>
              {version}
              <div>
                <span>{i18n.t('dop:configuration')}: </span>
                <span>{getAddonPlanCN(addon.data.plan)}</span>
              </div>
            </div>
          );
        },
      };

      return (
        <DiceYamlEditorItem
          editing={editing}
          selectedItem={selectedAddon ? selectedAddon.name === item.name : false}
          key={addon.name}
          className="add-on-item"
          item={item}
          pointType="none"
          deleteItem={() => deleteAddon(addon)}
          onClick={clickAddonItem}
        />
      );
    });
  };

  const commitData = (commitContent: string, values: any) => {
    const { path } = tree;
    const { branch, message: commitMsg } = values;
    updater.openDrawer(false);

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
      changeMode({
        editFile: false,
        addFile: false,
      });
      message.success(i18n.t('dop:file modified successfully'));
      getRepoBlob();
    });
  };

  const handleSubmit = (values: any) => {
    let submitContent;
    if (editedYmlStructure && isPipelineWorkflowYml(fileName)) {
      submitContent = yaml.dump(omit(editedYmlStructure, ['upgradedYmlContent', 'needUpgrade']));
      parsePipelineYmlStructure({ pipelineYmlContent: submitContent }).then(({ ymlContent }) => {
        commitData(ymlContent, values);
      });
    } else {
      clearNullValue(jsonContent);
      submitContent = yaml.dump(jsonContent);
      commitData(submitContent, values);
    }
  };

  const editAddon = (editingAddon: any, values: any) => {
    const addonData: any = addons.find((addon: any) => addon.name === editingAddon.name);
    addonData.data.plan = values.plan;
    addonData.data.name = values.name;
    if (!addonData.data.options) {
      addonData.data.options = {};
    }

    addonData.data.options.version = values.version;
    addonData.name = values.alias;

    if (values.originName) {
      delete jsonContent.addons[values.originName];
    }

    jsonContent.addons[values.alias] = addonData.data;

    convertDataByFileName(jsonContent);
  };

  /**
   * 渲染 Dice.yml 内容
   */
  const renderDiceContent = () => {
    const defaultClass = 'yaml-editor-item add-on-item create-add';

    const className =
      selectedAddon && selectedAddon.creatingAddon ? classnames(defaultClass, 'selected-item') : defaultClass;

    const title = editing ? `${i18n.t('edit')} ${fileName}` : fileName;

    return (
      <FileContainer className="yaml-editor-container" name={title} ops={ops}>
        <Spin spinning={isFetching}>
          <React.Fragment>
            <div className="yml-editor-body services-and-add-ons">
              <BlockContainer title={i18n.t('dop:service architecture')}>
                {openDrawer ? <div className="drawer-shadow" onClick={closedDrawer?.current} /> : null}
                <DiceYamlEditor
                  type={DiceFlowType.EDITOR}
                  isSelectedItem={selectedItemRef.current !== null}
                  editing={editing}
                  dataSource={editorData}
                  deleteItem={deleteService}
                  getCreateView={renderCreateComponent}
                  updateConnect={updateDiceYmlConnect}
                  clickItem={openDrawerForEditor}
                />
              </BlockContainer>
              <BlockContainer className="addons-container" title="Add On">
                <div className="addons-content">
                  {openDrawer ? <div className="drawer-shadow" onClick={closedDrawer?.current} /> : null}
                  {renderAddons()}
                  {editing ? (
                    <div className={className} onClick={showCreateAddonModal}>
                      +
                    </div>
                  ) : null}
                </div>
              </BlockContainer>
            </div>
            {renderSaveBtn()}
          </React.Fragment>
        </Spin>
      </FileContainer>
    );
  };

  /**
   * 渲染 Pipeline 内容
   */
  const renderPipelineContent = () => {
    const title = editing ? `${i18n.t('edit')} ${fileName}` : fileName;

    return (
      <FileContainer className="yaml-editor-container" name={title} ops={ops}>
        <Spin spinning={isFetching}>
          <React.Fragment>
            <div className="yml-editor-body">
              <BlockContainer className="services-and-add-ons" title={i18n.t('pipeline')}>
                {openDrawer ? <div className="drawer-shadow" onClick={closedDrawer?.current} /> : null}
                <DiceYamlEditor
                  type={DiceFlowType.EDITOR}
                  isSelectedItem={selectedItemRef.current !== null}
                  editing={editing}
                  clickItem={openDrawerForEditor}
                  dataSource={editorData}
                  updateConnect={updateDiceYmlConnect}
                  deleteItem={deletePipelineStage}
                  getCreateView={renderCreatePipelineComponent}
                />
              </BlockContainer>
            </div>
            {renderSaveBtn()}
          </React.Fragment>
        </Spin>
      </FileContainer>
    );
  };

  /**
   * 渲染 workflow 内容
   */
  const renderWorkflowContent = () => {
    const title = editing ? `${i18n.t('edit')} ${fileName}` : fileName;

    return (
      <FileContainer className="yaml-editor-container" name={title} ops={ops}>
        <Spin spinning={isFetching}>
          <React.Fragment>
            <div className="yml-editor-body">
              <BlockContainer className="services-and-add-ons" title="Workflow">
                {openDrawer ? <div className="drawer-shadow" onClick={closedDrawer?.current} /> : null}
                <DiceYamlEditor
                  type={DiceFlowType.EDITOR}
                  isSelectedItem={selectedItemRef.current !== null}
                  editing={editing}
                  clickItem={openDrawerForEditor}
                  dataSource={editorData}
                  updateConnect={updateDiceYmlConnect}
                  deleteItem={deletePipelineStage}
                  getCreateView={renderCreatePipelineComponent}
                />
              </BlockContainer>
            </div>
            {renderSaveBtn()}
          </React.Fragment>
        </Spin>
      </FileContainer>
    );
  };

  let fileContent = null;
  let drawerContent = null;

  switch (fileType.current) {
    case WORK_FLOW_TYPE.DICE:
      fileContent = renderDiceContent();
      break;
    case WORK_FLOW_TYPE.PIPELINE:
      fileContent = renderPipelineContent();
      break;
    case WORK_FLOW_TYPE.WORKFLOW:
      fileContent = renderWorkflowContent();
      break;
    default:
      fileContent = null;
      break;
  }

  if (selectedItemRef.current) {
    // @ts-ignore
    drawerContent = selectedItemRef.current.editView(editing);
  } else if (selectedAddon) {
    // @ts-ignore
    drawerContent = selectedAddon.editView(editing);
  }

  return (
    <div className="yml-editor">
      {fileContent}
      <Drawer
        className={selectedAddon ? 'add-on-drawer' : null}
        readonly={!editing}
        visible={openDrawer}
        title={drawerTitle}
        content={drawerContent}
        onClose={closedDrawer?.current}
      />
      <FormModal
        width={620}
        title={`${i18n.t('delete')}${name}`}
        fieldsList={getFieldsList()}
        visible={modalVisible}
        onOk={handleDelete}
        onCancel={() => toggleModal(false)}
      />
    </div>
  );
};

export default YmlEditor;
