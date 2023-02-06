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
import { EmptyHolder, Icon as CustomIcon, IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import { map, isEmpty } from 'lodash';
import { Popconfirm, Modal, Divider, Button, Tooltip } from 'antd';
import issueWorkflowStore from 'project/stores/issue-workflow';
import { issueMainStateMap } from 'project/common/components/issue/issue-state';
import { produce } from 'immer';
import WorkflowStateForm from './workflow-state-form';
import routeInfoStore from 'core/stores/route';
import { ISSUE_TYPE } from 'project/common/components/issue/issue-config';
import './issue-workflow-setting-modal.scss';
import { FIELD_TYPE_ICON_MAP } from 'org/common/config';
import { WithAuth, usePerm } from 'user/common';

interface IProps {
  visible: boolean;
  issueType: ISSUE_TYPE;
  onCloseModal: () => void;
}

const IssueWorkflowSettingModal = ({ visible, onCloseModal, issueType }: IProps) => {
  const workflowStateList = issueWorkflowStore.useStore((s) => s.workflowStateList);
  const { deleteIssueState, getStatesByIssue, batchUpdateIssueState } = issueWorkflowStore;
  const { projectId: projectID } = routeInfoStore.useStore((s) => s.params);
  const [{ dataList, formVisible, isChanged }, updater, update] = useUpdate({
    dataList: workflowStateList,
    formVisible: false,
    isChanged: false,
  });

  const hasAuth = usePerm((s) => s.project.setting.customWorkflow.operation.pass);

  const getDataList = React.useCallback(() => {
    getStatesByIssue({ projectID: +projectID });
  }, [getStatesByIssue, projectID]);

  React.useEffect(() => {
    const partWorkflowStateList = workflowStateList.filter((item) => item.issueType === issueType);
    const tempList = map(partWorkflowStateList, (item) => {
      return {
        ...item,
        relations: map(partWorkflowStateList, ({ stateID, stateName }) => {
          return {
            stateID,
            isRelated: item?.stateRelation?.includes(stateID),
            name: stateName,
          };
        }),
      };
    });
    update({ dataList: tempList });
  }, [issueType, update, workflowStateList]);

  const onCancel = React.useCallback(() => {
    update({ dataList: [], isChanged: false, formVisible: false });
    onCloseModal();
  }, [onCloseModal, update]);

  const onSubmit = React.useCallback(async () => {
    await batchUpdateIssueState({
      projectID: +projectID,
      data: dataList,
      issueType,
    });
    onCancel();
  }, [batchUpdateIssueState, projectID, dataList, issueType, onCancel]);

  const onStateChange = (stateDataIndex: number, stateBelong: string) => {
    const tempList = produce(dataList, (draft) => {
      draft[stateDataIndex].stateBelong = stateBelong;
    });

    update({ dataList: tempList, isChanged: true });
  };

  const onRelationChange = (stateDataIndex: number, relationIndex: number, relatedState: boolean) => {
    const tempList = produce(dataList, (draft) => {
      draft[stateDataIndex].relations[relationIndex].isRelated = relatedState;
      const stateRelation: number[] = [];
      map(draft[stateDataIndex].relations, (data) => {
        if (data.isRelated) {
          stateRelation.push(data.stateID);
        }
      });
      draft[stateDataIndex].stateRelation = stateRelation;
    });
    update({ dataList: tempList, isChanged: true });
  };

  const onChangeStateOrder = async (stateDataIndex: number, direction: number) => {
    const tempList = produce(dataList, (draft) => {
      draft[stateDataIndex].index = stateDataIndex + direction;
      draft[stateDataIndex + direction].index = stateDataIndex;
      draft.sort((a, b) => a.index - b.index);
    });
    await batchUpdateIssueState({
      projectID: +projectID,
      data: tempList,
      issueType,
    });
    getDataList();
  };

  const onDeleteState = React.useCallback(
    (stateID: number) => {
      deleteIssueState({ id: stateID, projectID: +projectID }).then(() => {
        getDataList();
      });
    },
    [deleteIssueState, getDataList, projectID],
  );

  const flexWidthClass = React.useMemo(() => {
    return dataList.length <= 4 ? 'w-full' : '';
  }, [dataList]);

  const fName = FIELD_TYPE_ICON_MAP[issueType]?.name;
  return (
    <Modal
      title={i18n.t('edit {name}', { name: `${fName}${i18n.t('dop:workflow')}` })}
      visible={visible}
      width="1010px"
      onCancel={onCancel}
      destroyOnClose
      maskClosable={false}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          {i18n.t('cancel')}
        </Button>,
        <Button key="submit" type="primary" onClick={onSubmit} disabled={!isChanged}>
          {i18n.t('save')}
        </Button>,
      ]}
    >
      <div className="issue-workflow-setting-modal">
        <div className="mb-3">
          <IF check={formVisible}>
            <WorkflowStateForm
              issueType={issueType}
              onOk={getDataList}
              onCancel={() => {
                updater.formVisible(false);
              }}
            />
          </IF>
        </div>
        <div className="form-content">
          <div className="flex justify-between items-center">
            <div className="form-content-left mr-1">
              <IF check={!formVisible}>
                <WithAuth pass={hasAuth}>
                  <Button
                    type="primary"
                    className="w-full add-option-btn text-xs"
                    onClick={() => {
                      updater.formVisible(true);
                    }}
                  >
                    <CustomIcon type="cir-add" className="mr-1" />
                    {i18n.t('add')}
                  </Button>
                </WithAuth>
              </IF>
            </div>
            <div className="form-content-right flex justify-between items-center">
              {map(dataList, ({ stateName, stateID }, stateDataIndex) => {
                return (
                  <div className={`state-radio-group ${flexWidthClass}`} key={stateID}>
                    <div className={`state-option-btn flex justify-between items-center ${flexWidthClass}`}>
                      <WithAuth pass={hasAuth}>
                        <CustomIcon
                          className={`state-move-btn ${stateDataIndex === 0 ? 'disabled' : 'cursor-pointer'}`}
                          type="arrow-left"
                          onClick={() => {
                            onChangeStateOrder(stateDataIndex, -1);
                          }}
                        />
                      </WithAuth>
                      <Tooltip title={stateName}>
                        <div className="text-xs nowrap">{stateName}</div>
                      </Tooltip>
                      <WithAuth pass={hasAuth}>
                        <CustomIcon
                          className={`state-move-btn ${
                            stateDataIndex === dataList.length - 1 ? 'disabled' : 'cursor-pointer'
                          }`}
                          type="arrow-right"
                          onClick={() => {
                            onChangeStateOrder(stateDataIndex, 1);
                          }}
                        />
                      </WithAuth>
                      <WithAuth pass={hasAuth}>
                        <Popconfirm
                          title={`${i18n.t('common:confirm to delete')}?`}
                          onConfirm={() => onDeleteState(stateID)}
                        >
                          <CustomIcon className="state-delete-btn cursor-pointer" type="thin-del" />
                        </Popconfirm>
                      </WithAuth>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          <IF check={!isEmpty(dataList)}>
            <Divider className="my-2" orientation="left">
              {i18n.t('dop:state setting')}
            </Divider>
            <div className="flex justify-between items-center">
              <div className="form-content-left">
                {map(issueMainStateMap[issueType], (stateItem) => {
                  return (
                    <div className="state-td" key={stateItem.stateName}>
                      {stateItem.stateName}
                    </div>
                  );
                })}
              </div>
              <div className="form-content-right flex justify-between items-center">
                {map(dataList, ({ stateBelong, stateID }, stateDataIndex) => {
                  return (
                    <div className={`state-radio-group ${flexWidthClass}`} key={stateID}>
                      {map(issueMainStateMap[issueType], (_v: Obj, k: string) => {
                        return (
                          <div className="state-td" key={k}>
                            <WithAuth pass={hasAuth}>
                              <CustomIcon
                                className="state-radio-btn cursor-pointer"
                                type={stateBelong !== k ? 'circle' : 'circle-fill'}
                                onClick={() => {
                                  onStateChange(stateDataIndex, k);
                                }}
                              />
                            </WithAuth>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </div>
            <Divider className="my-2" orientation="left">
              {i18n.t('dop:circulation setting')}
            </Divider>
            {map(dataList, ({ relations, stateName, stateID }, stateDataIndex) => {
              return (
                <div className="flex justify-between items-center my-3" key={stateID}>
                  <div className="form-content-left text-center text-xs ">
                    <div className="flex justify-between items-center w-120">
                      <Tooltip title={stateName}>
                        <span className="font-medium nowrap state-transfer-name">{stateName}</span>
                      </Tooltip>
                      <span className="ml-2 text-desc">{i18n.t('dop:can circulate to')}</span>
                    </div>
                  </div>
                  <div className="form-content-right flex justify-between items-center">
                    {map(relations, ({ isRelated, name }, relationIndex) => {
                      return (
                        <div className={`state-radio-group ${flexWidthClass}`}>
                          <div className={'state-td state-radio-btn mx-0 my-0'} key={name}>
                            <WithAuth pass={hasAuth}>
                              <CustomIcon
                                type={isRelated ? 'duoxuanxuanzhong' : 'icon-test'}
                                className={`${isRelated ? '' : 'font-bold'} ${
                                  name === stateName ? 'disabled' : 'cursor-pointer'
                                }`}
                                onClick={() => {
                                  name !== stateName && onRelationChange(stateDataIndex, relationIndex, !isRelated);
                                }}
                              />
                            </WithAuth>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </IF>
          <IF check={isEmpty(dataList)}>
            <EmptyHolder relative tip={i18n.t('dop:No workflow data')} />
          </IF>
        </div>
      </div>
    </Modal>
  );
};

export default IssueWorkflowSettingModal;
