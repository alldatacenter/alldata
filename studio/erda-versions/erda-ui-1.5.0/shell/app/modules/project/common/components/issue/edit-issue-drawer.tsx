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
import { Button, Select, Tabs, message, Spin, Dropdown, Menu, Divider, Popconfirm } from 'antd';
import { IssueIcon, getIssueTypeOption } from 'project/common/components/issue/issue-icon';
import { map, has, cloneDeep, includes, isEmpty, merge, find } from 'lodash';
import moment from 'moment';
import { EditField, Icon as CustomIcon, IF, MemberSelector } from 'common';
import { Link } from 'react-router-dom';
import { goTo, insertWhen } from 'common/utils';
import {
  ISSUE_TYPE,
  ISSUE_TYPE_MAP,
  ISSUE_PRIORITY_LIST,
  ISSUE_PRIORITY_MAP,
  ISSUE_COMPLEXITY_MAP,
  EDIT_PROPS,
  BUG_SEVERITY_MAP,
} from 'project/common/components/issue/issue-config';
import i18n from 'i18n';
import issueStore from 'project/stores/issues';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { IssueDrawer } from 'project/common/components/issue/issue-drawer';
import { IssueCommentBox } from 'project/common/components/issue/comment-box';
import { AddRelation } from 'project/common/components/issue/add-relation';
import { IssueActivities } from 'project/common/components/issue/issue-activities';
import { updateSearch } from 'common/utils/query-string';
import projectMemberStore from 'common/stores/project-member';
import iterationStore from 'app/modules/project/stores/iteration';
import labelStore from 'project/stores/label';
import { getUserMap } from 'core/stores/userMap';
import IssueState from 'project/common/components/issue/issue-state';
import { useMount } from 'react-use';
import userStore from 'app/user/stores';
import IterationSelect from './iteration-select';
import { usePerm, WithAuth, getAuth, isAssignee, isCreator } from 'user/common';
import { TimeInput } from './time-input';
import { TextFieldInput, NumberFieldInput } from './text-field-input';
import { TimeTrace } from './time-trace';
import { IssueRelation } from './issue-relation';
import { IssueTestCaseRelation } from './issue-testCase-relation';
import { FIELD_WITH_OPTION, FIELD_TYPE_ICON_MAP } from 'org/common/config';
import { produce } from 'immer';
import issueFieldStore from 'org/stores/issue-field';
import orgStore from 'app/org-home/stores/org';
import { templateMap } from 'project/common/issue-config';

import './edit-issue-drawer.scss';

export const ColorIcon = ({ icon }: { icon: string }) => {
  return (
    <CustomIcon type={icon} className="mr-2" color style={{ height: '20px', width: '20px', verticalAlign: 'sub' }} />
  );
};

const { Option } = Select;
const { TabPane } = Tabs;
const priorityOptions = map(ISSUE_PRIORITY_LIST, ({ iconLabel, value }) => (
  <Option key={value} value={value}>
    {iconLabel}
  </Option>
));
const complexityOptions = map(ISSUE_COMPLEXITY_MAP, ({ iconLabel, value }) => (
  <Option key={value} value={value}>
    {iconLabel}
  </Option>
));
const severityOptions = map(BUG_SEVERITY_MAP, ({ iconLabel, value }) => (
  <Option key={value} value={value}>
    {iconLabel}
  </Option>
));

const getCustomOptions = (enumeratedValues: any[]) => {
  return map(enumeratedValues, ({ name, id }) => (
    <Option key={name} value={id}>
      {name}
    </Option>
  ));
};

const { getLabels } = labelStore.effects;
const IssueMetaFields = React.forwardRef(
  ({ labels, isEditMode, isBacklog, editAuth, issueType, formData, setFieldCb, projectId, ticketType }: any, ref) => {
    const userMap = getUserMap();
    const projectMembers = projectMemberStore.useStore((s) => s.list);
    const urlParams = routeInfoStore.useStore((s) => s.params);
    // const isRequirement = issueType === ISSUE_TYPE.REQUIREMENT;
    const isEpic = issueType === ISSUE_TYPE.EPIC;
    const iterationList = iterationStore.useStore((s) => s.iterationList);
    const isMonitorTicket = ticketType === 'monitor';
    const customFieldDetail = issueStore.getState((s) => s.customFieldDetail);

    const [bugStageList, taskTypeList] = issueFieldStore.useStore((s) => [s.bugStageList, s.taskTypeList]);
    const [optionList, setOptionList] = React.useState(labels);
    const stageOptions = React.useMemo(() => {
      return map(bugStageList, ({ name, id, value }) => (
        <Option key={id} value={value}>
          {name}
        </Option>
      ));
    }, [bugStageList]);
    const taskTypeOptions = React.useMemo(() => {
      return map(taskTypeList, ({ name, id, value }) => (
        <Option key={id} value={value}>
          {name}
        </Option>
      ));
    }, [taskTypeList]);

    React.useEffect(() => {
      if (ref && !ref.current) {
        const customFieldKeys = map(customFieldDetail?.property, 'propertyName').concat([
          'taskType',
          'bugStage',
          'owner',
        ]);
        ref.current = {
          onFocus: (fKey: string) => {
            const curRef = ref.current?.refMap[fKey];
            if (fKey === 'issueManHour.elapsedTime') {
              // click to open time trace
              curRef.click();
            } else if (customFieldKeys.includes(fKey)) {
              curRef?.focus?.();
            } else {
              curRef?.focus?.();
            }
          },
          refMap: {},
        };
      }
    }, [ref, customFieldDetail]);

    useMount(() => {
      getLabels({ type: 'issue', projectID: Number(projectId) });
      if (!iterationList.length && !isBacklog) {
        iterationStore.effects.getIterations({
          pageNo: 1,
          pageSize: 100,
          projectID: +urlParams.projectId,
          withoutIssueSummary: true,
        });
      }
    });

    React.useEffect(() => {
      setOptionList(labels);
    }, [labels, formData.labels]); // reset list after change

    const customFieldList = React.useMemo(() => {
      return map(customFieldDetail?.property, (filedData: ISSUE_FIELD.IFiledItem) => {
        const { propertyName, displayName = '', required, propertyType, enumeratedValues } = filedData;

        return {
          className: `mb-5`,
          name: propertyName,
          label: displayName,
          required,
          type: FIELD_TYPE_ICON_MAP[propertyType]?.component,
          showRequiredMark: required,
          itemProps: {
            className: 'w-full',
            options: getCustomOptions(enumeratedValues || []),
            mode: propertyType === 'MultiSelect' ? 'multiple' : undefined,
            allowClear: !required,
          },
          getComp: ({ value, disabled, originalValue, onSave }: any) => {
            return propertyType === 'Person' ? (
              <MemberSelector
                scopeType="project"
                ref={(r) => {
                  const _refMap = ref?.current?.refMap;
                  _refMap && (_refMap[propertyName] = r);
                }}
                className="issue-field-owner"
                disabled={!editAuth}
                scopeId={urlParams.projectId || String(projectId)}
                onChange={(val: any) => onSave(val)}
                value={value}
                allowClear={!required}
              />
            ) : propertyType === 'Number' ? (
              <NumberFieldInput
                className="w-full"
                value={value}
                ref={(r) => {
                  const _refMap = ref?.current?.refMap;
                  _refMap && (_refMap[propertyName] = r);
                }}
                onChange={(e: any) => {
                  onSave(e);
                }}
              />
            ) : (
              <TextFieldInput
                showErrTip
                ref={(r) => {
                  const _refMap = ref?.current?.refMap;
                  _refMap && (_refMap[propertyName] = r);
                }}
                value={value}
                displayName={displayName}
                rule={(FIELD_TYPE_ICON_MAP[propertyType] as Obj)?.rule}
                passAndTrigger
                triggerChangeOnButton
                originalValue={originalValue}
                onChange={onSave}
                disabled={disabled}
              />
            );
          },
        };
      });
    }, [customFieldDetail?.property, editAuth, urlParams.projectId, projectId, ref]);
    let editFieldList = [
      ...insertWhen(isEditMode, [
        {
          className: 'mb-5',
          name: 'state',
          label: i18n.t('dop:state'),
          type: 'select',
          itemProps: {
            options: map(formData.issueButton, ({ stateID, permission: curAuth }) => (
              <Option disabled={!curAuth} key={stateID} value={stateID}>
                <IssueState stateID={stateID} />
              </Option>
            )),
            allowClear: false,
          },
        },
      ]),
      {
        className: 'mb-5 w-full',
        name: 'assignee',
        label: i18n.t('dop:assignee'),
        type: 'custom',
        showRequiredMark: true,
        getComp: ({ value, onSave }: any) => {
          return (
            <MemberSelector
              scopeType="project"
              className="issue-field-assignee"
              disabled={!editAuth}
              scopeId={urlParams.projectId || String(projectId)}
              onChange={(val: any) => onSave(val)}
              value={value}
              allowClear={false}
              showSelfChosen
            />
          );
        },
      },
      ...insertWhen(issueType === ISSUE_TYPE.BUG && isEditMode, [
        {
          className: 'mb-5 w-full',
          type: 'custom',
          name: 'owner',
          label: i18n.t('dop:responsible person'),
          getComp: ({ value, onSave }: any) => {
            return (
              <MemberSelector
                scopeType="project"
                className="issue-field-owner"
                ref={(r) => {
                  const _refMap = ref?.current?.refMap;
                  _refMap && (_refMap.owner = r);
                }}
                disabled={!editAuth}
                scopeId={urlParams.projectId || String(projectId)}
                onChange={(val: any) => onSave(val)}
                value={value}
                allowClear={false}
              />
            );
          },
        },
      ]),
      ...insertWhen(issueType !== ISSUE_TYPE.TICKET && issueType !== ISSUE_TYPE.EPIC, [
        {
          className: 'mb-5 w-full',
          name: 'iterationID',
          label: i18n.t('dop:owned iteration'),
          type: 'custom',
          valueRender: (value: string) => {
            const match = iterationList.find((item) => String(item.id) === String(value));
            return match ? match.title : value;
          },
          getComp: ({ value, onSave }: any) => (
            <IterationSelect
              fullWidth
              value={value}
              onChange={onSave}
              disabled={!editAuth}
              placeholder={i18n.t('please choose {name}', { name: i18n.t('dop:owned iteration') })}
            />
          ),
        },
      ]),
      ...insertWhen(issueType === ISSUE_TYPE.TICKET && !isMonitorTicket, [
        {
          className: 'mb-5',
          name: 'source',
          label: i18n.t('dop:source'),
          itemProps: {
            placeholder: i18n.t('please enter'),
            maxLength: 200,
          },
        },
      ]),
      {
        name: 'splitLine1',
        type: 'custom',
        getComp: () => <Divider className="mb-6 mt-0" />,
      },
      {
        name: 'priority',
        className: 'mb-5',
        label: i18n.t('dop:priority'),
        type: 'select',
        itemProps: { options: priorityOptions, allowClear: false },
      },
      ...insertWhen(issueType === ISSUE_TYPE.TICKET || issueType === ISSUE_TYPE.BUG, [
        {
          className: 'mb-5',
          name: 'severity',
          label: i18n.t('dop:severity'),
          type: 'select',
          itemProps: {
            options: severityOptions,
            allowClear: false,
            placeholder: i18n.t('please choose {name}', { name: i18n.t('dop:severity') }),
          },
        },
      ]),
      ...insertWhen(issueType !== ISSUE_TYPE.TICKET, [
        {
          className: 'mb-5',
          name: 'complexity',
          label: i18n.t('dop:complexity'),
          type: 'select',
          itemProps: {
            options: complexityOptions,
            allowClear: false,
            placeholder: i18n.t('please choose {name}', { name: i18n.t('dop:complexity') }),
          },
        },
      ]),
      {
        className: 'mb-5 w-full',
        name: 'planStartedAt',
        label: i18n.t('common:start at'),
        type: 'datePicker',
        showRequiredMark: ISSUE_TYPE.EPIC === issueType,
        itemProps: {
          allowClear: true,
        },
      },
      {
        className: 'mb-5 w-full',
        name: 'planFinishedAt',
        label: i18n.t('deadline'),
        type: 'datePicker',
        showRequiredMark: ISSUE_TYPE.EPIC === issueType,
        itemProps: {
          allowClear: true,
          endDay: true,
        },
      },
      ...insertWhen(![ISSUE_TYPE.TICKET, ISSUE_TYPE.EPIC].includes(issueType), [
        {
          className: 'mb-5',
          name: ['issueManHour', 'estimateTime'],
          label: i18n.t('dop:EstimateTime'),
          type: 'custom',
          getComp: ({ value, disabled, originalValue }: any) => (
            <TimeInput
              showErrTip
              value={value}
              passAndTrigger
              ref={(r) => {
                const _refMap = ref?.current?.refMap;
                _refMap && (_refMap['issueManHour.estimateTime'] = r);
              }}
              triggerChangeOnButton
              originalValue={originalValue}
              onChange={(v) => {
                if (isEditMode && formData?.issueManHour?.isModifiedRemainingTime !== false) {
                  setFieldCb({ issueManHour: { estimateTime: v || 0 } });
                } else {
                  // 创建模式或编辑模式但剩余时间为空时，设置剩余时间为预估时间
                  setFieldCb({ issueManHour: { estimateTime: v || 0, remainingTime: v || 0 } });
                }
              }}
              disabled={disabled}
            />
          ),
        },
        ...insertWhen(!isEpic && isEditMode, [
          {
            className: 'mb-5',
            name: 'issueManHour',
            label: i18n.t('dop:Time tracking'),
            type: 'custom',
            getComp: ({ value, disabled }: any) => (
              <TimeTrace
                value={value}
                ref={(r) => {
                  const _refMap = ref?.current?.refMap;
                  _refMap && (_refMap['issueManHour.elapsedTime'] = r);
                }}
                onChange={(v) => setFieldCb({ issueManHour: v })}
                isModifiedRemainingTime={formData?.issueManHour?.isModifiedRemainingTime}
                disabled={disabled}
              />
            ),
          },
        ]),
      ]),
      {
        className: 'mb-5 w-full',
        name: 'labels',
        label: i18n.t('label'),
        type: 'select', // 需要新建不存在的tag，用 tagName 作为值传递，不要用 LabelSelect
        itemProps: {
          options: map(optionList, ({ id: labelId, name, isNewLabel }) => {
            if (isNewLabel) {
              return (
                <Option key={labelId} value={name} title={name}>
                  {i18n.t('does not exist')}
                  {name}
                </Option>
              );
            } else {
              return (
                <Option key={labelId} value={name} title={name}>
                  {name}
                </Option>
              );
            }
          }),
          mode: 'tags',
          optionLabelProp: 'title', // 给select组件添加 optionLabelProp 属性，改变回填到选择框的 Option 的属性值
          dropdownRender: (menu: any) => (
            <div>
              {menu}
              <Divider className="my-1" />
              <Link
                to={goTo.resolve.projectLabel()}
                onClick={(e) => {
                  e.preventDefault();
                  goTo(goTo.resolve.projectLabel(), { jumpOut: true });
                }}
              >
                <div className="mx-3">{i18n.t('dop:edit label')}</div>
              </Link>
            </div>
          ),
          onSearch: (value: string) => {
            if (!value) {
              setOptionList(labels);
              return;
            }
            const match = labels.filter((item: any) => item.name.toLowerCase().includes(value.toLowerCase()));
            if (!match.length) {
              setOptionList([]);
              return;
            }
            setOptionList(match);
          },
        },
      },
      ...insertWhen(issueType === ISSUE_TYPE.TASK, [
        {
          className: `mb-5 w-full`,
          name: 'taskType',
          label: i18n.t('task type'),
          type: 'select',
          showRequiredMark: true,
          itemProps: { options: taskTypeOptions, allowClear: false },
        },
      ]),
      ...insertWhen(issueType === ISSUE_TYPE.BUG, [
        {
          className: `mb-5 w-full`,
          type: 'select',
          name: 'bugStage',
          label: i18n.t('dop:import source'),
          showRequiredMark: true,
          itemProps: { options: stageOptions, allowClear: false },
        },
      ]),
      ...insertWhen(!!customFieldList.length, [
        {
          type: 'readonly',
          name: '',
          label: '',
          valueRender: () => {
            return (
              <Divider className="mb-6 mt-0.5 text-xs text-desc" plain>
                {i18n.t('common:custom')}
              </Divider>
            );
          },
        },
      ]),
      ...customFieldList,
      ...insertWhen(isEditMode, [
        {
          name: 'creator',
          label: '',
          type: 'readonly',
          valueRender: (value: string) => {
            let user = projectMembers.find((item: IMember) => String(item.userId) === String(value)) as IMember;
            if (!user) {
              user = userMap[value] || {};
            }

            return (
              <>
                <Divider className="mb-6 mt-0.5" />
                <div className="text-desc text-xs prewrap">
                  {user.nick || user.name}&nbsp;{i18n.t('created at')}&nbsp;
                  {moment(formData.createdAt).format('YYYY/MM/DD')}
                </div>
              </>
            );
          },
        },
      ]),
    ];

    editFieldList = map(editFieldList, (fieldProps: any) => ({
      onChangeCb: setFieldCb,
      labelStyle: 'desc',
      data: formData,
      ...fieldProps,
    }));

    return (
      <div>
        {map(editFieldList, (fieldProps: any) => {
          return (
            <EditField
              ref={(r) => {
                const _refMap = ref?.current?.refMap;
                _refMap && (_refMap[fieldProps.name] = r);
              }}
              refMap={ref?.current?.refMap}
              key={fieldProps.name}
              {...fieldProps}
              disabled={!editAuth}
            />
          );
        })}
      </div>
    );
  },
);

export interface CloseDrawerParam {
  hasEdited: boolean;
  isCreate: boolean;
  isDelete: boolean;
}
interface IProps {
  issueType: ISSUE_TYPE;
  id?: number;
  iterationID?: number;
  visible: boolean;
  projectId?: number | string;
  ticketType?: 'monitor'; // 区分监控工单
  shareLink?: string;
  subDrawer?: JSX.Element | null;
  customUrl?: string; // 监控特殊 url 用来增改工单
  closeDrawer: (params: CloseDrawerParam) => void;
}

export const EditIssueDrawer = (props: IProps) => {
  const {
    id: propId,
    visible,
    closeDrawer,
    issueType: propsIssueType,
    iterationID,
    projectId,
    shareLink,
    subDrawer,
    ticketType,
    customUrl,
  } = props;
  const [issueType, setIssueType] = React.useState(propsIssueType);
  const type = issueType.toLowerCase();
  const {
    getIssueDetail,
    updateIssue,
    updateType,
    getIssueStreams,
    createIssue,
    copyIssue,
    addIssueStream,
    deleteIssue,
    getFieldsByIssue,
    addFieldsToIssue,
  } = issueStore.effects;
  const { clearIssueDetail } = issueStore.reducers;
  const [bugStageList, taskTypeList, fieldList] = issueFieldStore.useStore((s) => [
    s.bugStageList,
    s.taskTypeList,
    s.fieldList,
  ]);
  const id = propId;
  const isEditMode = !!id;
  const defaultCustomFormData = React.useMemo(() => {
    const customFieldDefaultValues = {};
    map(fieldList, (item) => {
      if (item && item.required) {
        if (item.propertyType === 'Select') {
          customFieldDefaultValues[item.propertyName] = item.enumeratedValues?.[0].id;
        }
        if (item.propertyType === 'MultiSelect') {
          customFieldDefaultValues[item.propertyName] = [item.enumeratedValues?.[0].id];
        }
      }
    });
    return customFieldDefaultValues;
  }, [fieldList]);

  const defaultFormData = React.useMemo(() => {
    return {
      priority: ISSUE_PRIORITY_MAP.NORMAL.value,
      complexity: ISSUE_COMPLEXITY_MAP.NORMAL.value,
      severity: BUG_SEVERITY_MAP.NORMAL.value,
      taskType: taskTypeList?.length ? taskTypeList[0].value : '',
      bugStage: bugStageList?.length ? bugStageList[0].value : '',
      assignee: userStore.getState((s) => s.loginUser.id),
      planFinishedAt: issueType === ISSUE_TYPE.EPIC ? new Date() : undefined,
      planStartedAt: issueType === ISSUE_TYPE.EPIC ? new Date() : undefined,
      iterationID,
      content: isEditMode ? '' : templateMap[issueType] || '',
      ...defaultCustomFormData,
    };
  }, [bugStageList, defaultCustomFormData, isEditMode, issueType, iterationID, taskTypeList]);
  const [formData, setFormData] = React.useState(defaultFormData as any);
  const issueDetail: ISSUE.IssueType = issueStore.useStore((s) => s[`${type}Detail`]);

  // 监听bugDetail、taskDetail、requirementDetail的变化，切换类型后触发刷新
  issueStore.useStore((s) => [s.bugDetail, s.taskDetail, s.requirementDetail]);

  const labels = labelStore.useStore((s) => s.list);
  const [updateIssueLoading] = useLoading(issueStore, ['updateIssue']);
  const labelNames = map(labels, ({ name }) => name);
  const [isLoading, setIsLoading] = React.useState(false);
  const [hasEdited, setHasEdited] = React.useState(false);
  const [tempDescContent, setTempDescContent] = React.useState('');
  const [disableSubmit, setDisableSubmit] = React.useState(false);
  const isBug = issueType === ISSUE_TYPE.BUG;
  const customFieldDetail = issueStore.useStore((s) => s.customFieldDetail);
  const [customFormData, setCustomFormData] = React.useState(customFieldDetail as any);
  const { getFieldsByIssue: getCustomFieldsByProject } = issueFieldStore.effects;

  const savingRef = React.useRef(false);
  const isBacklog = iterationID === -1;
  const isMonitorTicket = ticketType === 'monitor';

  const { creator, assignee, testPlanCaseRels } = issueDetail || {};
  const specialProps = EDIT_PROPS[issueType];
  const projectPerm = usePerm((s) => s.project);
  const permObjMap = {
    [ISSUE_TYPE.REQUIREMENT]: projectPerm.requirement,
    [ISSUE_TYPE.TASK]: projectPerm.task,
    [ISSUE_TYPE.BUG]: projectPerm.bug,
    [ISSUE_TYPE.TICKET]: projectPerm.ticket,
    [ISSUE_TYPE.EPIC]: projectPerm.epic,
  };
  const permObj = permObjMap[issueType];
  const checkRole = [isCreator(creator), isAssignee(assignee)];
  const deleteAuth = isMonitorTicket ? true : getAuth(permObj.delete, checkRole);
  const createAuth = permObj.create.pass;
  const editAuth = isMonitorTicket ? true : !isEditMode || getAuth(permObj.edit, checkRole);
  const switchTypeAuth = getAuth(permObj.switchType, checkRole);

  const addRelatedMattersProjectId = routeInfoStore.getState((s) => s.params).projectId;
  const { addIssueRelation } = issueStore.effects;
  const { updateCustomFieldDetail } = issueStore.reducers;
  const { id: orgID } = orgStore.useStore((s) => s.currentOrg);
  const metaFieldsRef: React.RefObject<unknown> = React.useRef(null);
  const [tempStateData, setTempStateData] = React.useState('');

  React.useEffect(() => {
    setFormData((prev: any) => ({ ...prev, iterationID }));
  }, [iterationID]);

  const getCustomFields = React.useCallback(() => {
    id && getFieldsByIssue({ issueID: id, propertyIssueType: issueType, orgID });
  }, [getFieldsByIssue, id, issueType, orgID]);

  React.useEffect(() => {
    setIssueType(propsIssueType);
  }, [propsIssueType]);

  React.useEffect(() => {
    if (visible) {
      if (id) {
        getIssueDetail({ type: issueType, id });
        getIssueStreams({ type: issueType, id, pageNo: 1, pageSize: 50 });
        getCustomFields();
      }
      getCustomFieldsByProject({
        propertyIssueType: issueType,
        orgID,
      }).then((res) => {
        updateCustomFieldDetail({
          property: res,
          orgID,
          projectID: +addRelatedMattersProjectId,
          issueID: undefined,
        });
      });
    }
  }, [
    addRelatedMattersProjectId,
    getCustomFields,
    getCustomFieldsByProject,
    getFieldsByIssue,
    getIssueDetail,
    getIssueStreams,
    id,
    issueType,
    orgID,
    updateCustomFieldDetail,
    visible,
  ]);

  const customFieldValues = React.useMemo(() => {
    customFieldDetail && setCustomFormData(customFieldDetail);
    const tempFormData = {};
    map(customFieldDetail?.property, (item) => {
      const { arbitraryValue, propertyType, values, propertyName } = item;
      const _values = values || [];
      tempFormData[propertyName] = FIELD_WITH_OPTION[propertyType]
        ? propertyType === 'MultiSelect'
          ? _values
          : _values[0]
        : arbitraryValue;
    });
    return tempFormData;
  }, [customFieldDetail]);

  React.useEffect(() => {
    issueDetail && setFormData({ ...issueDetail, ...customFieldValues });
  }, [customFieldValues, issueDetail]);

  const dataCheck = (_data: Obj) => {
    if (ISSUE_TYPE.TASK === issueType) {
      // 创建时任务必填预估工时, 任务类型
      if (!isEditMode && !_data.taskType && issueType === ISSUE_TYPE.TASK) {
        message.warn(i18n.t('task type'));
        return false;
      }
      // if (!isEditMode && !_data.issueManHour?.estimateTime) {
      //   message.warn(i18n.t('dop:missing estimateTime'));
      //   return false;
      // }
    }
    if (!_data.title) {
      message.warn(i18n.t('dop:missing title'));
      return false;
    }
    if (!_data.assignee) {
      message.warn(i18n.t('dop:missing assignee'));
      return false;
    }

    // if (!_data.iterationID) {
    //   message.warn(i18n.t('please choose {name}', { name: i18n.t('dop:owned iteration') }));
    //   return false;
    // }

    if (ISSUE_TYPE.BUG === issueType) {
      if (!_data.bugStage) {
        message.warn(i18n.t('dop:missing import source'));
        return false;
      }
    }
    if (!_data.planFinishedAt && ISSUE_TYPE.EPIC === issueType) {
      message.warn(i18n.t('dop:missing deadline'));
      return false;
    }

    if (!_data.planStartedAt && ISSUE_TYPE.EPIC === issueType) {
      message.warn(i18n.t('dop:missing startTime'));
      return false;
    }

    return true;
  };

  const checkFieldNotEmpty = (propertyType: ISSUE_FIELD.IPropertyType, propertyValue: any) => {
    if (propertyType === 'MultiSelect') {
      return !isEmpty(propertyValue);
    } else if (propertyType === 'Number') {
      return propertyValue || String(propertyValue) === '0';
    } else {
      return propertyValue;
    }
  };

  const checkCustomFormData = (filterKey?: string) => {
    if (customFormData?.property) {
      const tempList = customFormData.property;
      for (let i = 0, len = tempList.length; i < len; i++) {
        const { displayName, required, arbitraryValue, propertyType, values, propertyName } = tempList[i];
        const _values = values || [];

        let propertyValue = [];
        if (propertyType === 'MultiSelect') {
          propertyValue = _values;
        } else if (propertyType === 'Select') {
          propertyValue = _values.length ? [_values[0]] : [];
        } else {
          propertyValue = arbitraryValue || String(arbitraryValue) === '0' ? [arbitraryValue] : [];
        }

        if (isEmpty(propertyValue) && required && (!filterKey || filterKey !== propertyName)) {
          message.warn(i18n.t('missing {name}', { name: displayName }));
          return false;
        }
      }
    }
    return true;
  };

  const focusOnFields = (fieldKey: string) => {
    metaFieldsRef?.current?.onFocus(fieldKey);
  };

  const setField = (value: Obj<any>) => {
    const formattedValue = value;

    // 处理 issueManHour
    if (has(value, 'issueManHour')) {
      formattedValue.issueManHour = merge({}, formData.issueManHour, value.issueManHour);
    }

    const params: ISSUE.IssueType = merge({}, formData, formattedValue);

    if (value.labels) {
      params.labels = value.labels;
    }
    if (has(value, 'planFinishedAt') && !value.planFinishedAt) {
      params.planFinishedAt = ''; // replace null to mark delete
    }
    if (has(value, 'planStartedAt') && !value.planStartedAt) {
      params.planStartedAt = '';
    }

    if ([ISSUE_TYPE.TASK, ISSUE_TYPE.BUG].includes(issueType)) {
      const warnMessage = [];
      if (value.state && isEditMode) {
        setTempStateData(value.state);
        // 编辑模式下修改状态时，必填时间追踪和预估工时, 任务类型
        if (!params.taskType && issueType === ISSUE_TYPE.TASK) {
          warnMessage.push({ msg: i18n.t('dop:missing task type'), key: 'taskType' });
        }
        if (!params.issueManHour.estimateTime) {
          warnMessage.push({ msg: i18n.t('dop:EstimateTime'), key: 'issueManHour.estimateTime' });
        }
        if (params.issueManHour.elapsedTime === 0 && params.issueManHour.thisElapsedTime === 0) {
          // filter out the working
          const workingState = formData.issueButton.find((item) => item.stateBelong === 'WORKING');
          // When working exists and select working, don't warn
          if (!workingState || value.state !== workingState.stateID) {
            warnMessage.push({ msg: i18n.t('dop:time spent in time tracing'), key: 'issueManHour.elapsedTime' });
          }
        }
      }
      if (warnMessage.length !== 0) {
        message.warn(
          <>
            <span className="font-bold">{map(warnMessage, 'msg').join(', ')}</span>
            <span>{i18n.t('dop:missing')}</span>
          </>,
        );
        focusOnFields(warnMessage[0].key);
        return false;
      }
    }
    // after validation, then set temp state in data. prevent enter line 857. see erda bug #235076
    if (has(value, 'issueManHour') && tempStateData) {
      formattedValue.state = tempStateData;
      setTempStateData('');
    }

    let promise;
    let customFieldKey = '';
    let customFieldValue: any;
    map(Object.keys(value), (k) => {
      customFieldKey = k;
      if (!(['planFinishedAt', 'planStartedAt'].includes(k) && !value[k])) {
        params[k] = value[k];
      }
      customFieldValue = value[k];
    });
    const customFieldData = find(customFormData?.property, (item) => item.propertyName === customFieldKey);
    const tempCustomFormData: ISSUE.ICreateField = produce(customFormData, (draft: any) => {
      map(draft?.property, (draftData) => {
        if (draftData.propertyName === customFieldKey) {
          if (FIELD_WITH_OPTION[draftData?.propertyType]) {
            const _values = customFieldValue || [];
            // eslint-disable-next-line no-param-reassign
            draftData.values = Array.isArray(_values) ? _values : [_values];
          } else if (draftData?.propertyType === 'Number') {
            // eslint-disable-next-line no-param-reassign
            draftData.arbitraryValue = Number(customFieldValue);
          } else {
            // eslint-disable-next-line no-param-reassign
            draftData.arbitraryValue = customFieldValue;
          }
        }
      });
    });
    setCustomFormData(tempCustomFormData);

    if (isEditMode) {
      setHasEdited(true);
      if (dataCheck({ ...params, customUrl })) {
        params.iterationID = +(params.iterationID as number) || -1;
        if (tempDescContent) {
          params.content = tempDescContent;
        }
        if (!customFieldValues?.hasOwnProperty(customFieldKey)) {
          savingRef.current = true;
          promise = updateIssue({ ...params, customUrl }).then(() => {
            getIssueStreams({ type: issueType, id: id as number, pageNo: 1, pageSize: 50 });
            getIssueDetail({ type: issueType, id: id as number }).then(() => {
              savingRef.current = false;
            });
            // setHasEdited(false); // 更新后置为false
          });
        } else {
          addFieldsToIssue(
            { ...tempCustomFormData, orgID, projectID: +addRelatedMattersProjectId },
            { customMsg: i18n.t('updated successfully') },
          ).then(() => {
            getCustomFields();
          });
        }
      }
      if (!checkFieldNotEmpty(customFieldData?.propertyType, customFieldValue) && customFieldData?.required) {
        const name = customFieldData?.displayName;
        message.warn(i18n.t('missing {name}', { name }));

        focusOnFields(name);
        return;
      }

      if (!checkCustomFormData(customFieldKey)) return;
    }
    setFormData(params);

    return promise;
  };

  const setFieldCb = (value: Obj<any>, fieldType?: string) => {
    if (fieldType && fieldType === 'markdown') {
      setTempDescContent(value?.content);
      return;
    }
    if (value.labels) {
      const labelName = cloneDeep(value.labels).pop();
      if (isEmpty(value.labels) || includes(labelNames, labelName)) {
        setField(value);
      } else {
        message.info(i18n.t('dop:the label does not exist, please select again'));
        setField({ ...value, labels: value.labels.slice(0, -1) }); // remove the last label, which is not exist
      }
    } else {
      setField(value);
    }
  };

  const onClose = (isCreate = false, isDelete = false) => {
    if (savingRef.current) return;
    setFormData(defaultFormData);
    closeDrawer({ hasEdited, isCreate, isDelete });
    setTempDescContent('');
    setIssueType(propsIssueType);
    setDisableSubmit(false);
    setHasEdited(false);
    setIsLoading(false);
    updateSearch({
      id: undefined,
    });
    setCustomFormData(customFieldDetail);
    isEditMode && issueType && clearIssueDetail(issueType);
  };

  const onDelete = () => {
    id &&
      deleteIssue(id).then(() => {
        onClose(false, true);
      });
  };

  const handleSubmit = (isCopy = false, copyTitle = '') => {
    if (!dataCheck(formData)) return;
    if (!checkCustomFormData()) return;
    setDisableSubmit(true);
    const params: ISSUE.IssueType = {
      projectID: Number(projectId),
      iterationID, // keep it first, allow form overwrite iterationID
      ...formData,
      type: issueType,
    };
    params.iterationID = +(params.iterationID as number) || -1; // 需求池指定迭代为-1
    isBug && !(params as ISSUE.Bug).owner && ((params as ISSUE.Bug).owner = params.assignee); // 责任人暂时默认设为处理人

    if (isCopy) {
      const { creator, ...restFormData } = formData;
      copyIssue({
        ...restFormData,
        title: copyTitle,
        issueManHour: { ...formData.issueManHour, elapsedTime: undefined },
        customUrl,
      })
        .then((res) => {
          addFieldsToIssue(
            { ...customFormData, issueID: res, projectID: params.projectID },
            { customMsg: i18n.t('copied successfully') },
          );
        })
        .finally(() => {
          onClose(true);
        });
      return;
    }

    if (id) {
      updateIssue({
        ...formData,
        issueManHour: { ...formData.issueManHour, elapsedTime: undefined },
        customUrl,
      }).finally(() => {
        onClose();
      });
    } else {
      createIssue({ ...params, customUrl }, { hideActionMsg: true })
        .then((res) => {
          savingRef.current = false;
          addFieldsToIssue(
            { ...customFormData, issueID: res, projectID: params.projectID },
            { customMsg: i18n.t('created successfully') },
          );
        })
        .finally(() => {
          onClose(true);
        });
    }
  };

  const ref = React.useRef(null);

  const switchType = (currentType: string) => {
    setIsLoading(true);
    updateType({ id: formData.id, type: currentType }).then(() => {
      setHasEdited(true);
      setIssueType(currentType as ISSUE_TYPE);
      setIsLoading(false);
    });
  };

  const handleMenuClick = ({ key }: { key: string }) => {
    iterationStore.effects
      .createIssue({
        // 创建事件
        projectID: +addRelatedMattersProjectId,
        iterationID: -1,
        type: key,
        priority: 'NORMAL',
        title: issueDetail.title,
        content: issueDetail.content,
      })
      .then((res: number) => {
        addRelation(res); // 添加关联
      });
  };

  const addRelation = (val: number) => {
    addIssueRelation({
      relatedIssues: val,
      id: issueDetail.id,
      projectId: +addRelatedMattersProjectId,
      type: 'connection',
    }).then(() => {
      setHasEdited(true);
      const refObj = ref.current as any;
      if (ref && refObj) {
        refObj.getList();
      }
    });
  };

  const addQuickIssueAuth = usePerm((s) => s.project.requirement.create.pass); // 目前迭代、任务、缺陷添加权限都一致

  let footer: any = [];
  if (isEditMode && issueType === ISSUE_TYPE.TICKET) {
    if (addQuickIssueAuth) {
      footer = [
        <Dropdown
          key="quick-add"
          overlay={
            <Menu onClick={handleMenuClick}>
              <Menu.Item key="REQUIREMENT">
                <IssueIcon type="REQUIREMENT" withName />
              </Menu.Item>
              <Menu.Item key="TASK">
                <IssueIcon type="TASK" withName />
              </Menu.Item>
              <Menu.Item key="BUG">
                <IssueIcon type="BUG" withName />
              </Menu.Item>
            </Menu>
          }
        >
          <Button className="mr-2">{i18n.t('dop:one click to backlog')}</Button>
        </Dropdown>,
      ];
    } else {
      footer = [
        <WithAuth key="create" pass={addQuickIssueAuth}>
          <Button className="mr-2">{i18n.t('dop:one click to backlog')}</Button>
        </WithAuth>,
      ];
    }
  }

  if (!isEditMode) {
    footer = (isChanged: boolean, confirmCloseTip: string | undefined) => [
      <div key="holder" />,
      <Spin key="submit" spinning={updateIssueLoading}>
        <div>
          {isChanged && confirmCloseTip ? (
            <Popconfirm title={confirmCloseTip} placement="topLeft" onConfirm={() => onClose()}>
              <Button>{i18n.t('cancel')}</Button>
            </Popconfirm>
          ) : (
            <Button onClick={() => onClose()}>{i18n.t('cancel')}</Button>
          )}

          <Button disabled={disableSubmit} onClick={() => handleSubmit()} type="primary">
            {i18n.t('ok')}
          </Button>
        </div>
      </Spin>,
    ];
  }

  footer = typeof footer === 'function' ? footer : footer.length ? <>{footer}</> : undefined;

  return (
    <IssueDrawer
      editMode={isEditMode}
      visible={visible}
      loading={isLoading || updateIssueLoading}
      onClose={() => onClose()}
      onDelete={isEditMode ? onDelete : undefined}
      shareLink={shareLink}
      subDrawer={subDrawer}
      canDelete={deleteAuth && !isMonitorTicket}
      canCreate={createAuth}
      confirmCloseTip={isEditMode ? undefined : i18n.t('dop:The new data will be lost if closed. Continue?')}
      handleCopy={handleSubmit}
      maskClosable={isEditMode}
      data={formData}
      projectId={projectId}
      issueType={issueType}
      setData={setFormData}
      footer={footer}
      // loading={
      //   loading.createIssue || loading.getIssueDetail || loading.updateIssue
      // }
    >
      <div className="flex justify-between items-center">
        <IF check={isEditMode}>
          {/* className=''是为了覆盖组件里的className="w-full"，否则选择框宽度为100% */}
          {[ISSUE_TYPE.REQUIREMENT, ISSUE_TYPE.TASK, ISSUE_TYPE.BUG].includes(issueType) ? (
            <WithAuth pass={switchTypeAuth}>
              <EditField
                name="type"
                type="select"
                data={formData}
                itemProps={{
                  options: getIssueTypeOption(issueType),
                  optionLabelProp: 'data-icon',
                  dropdownMatchSelectWidth: false,
                  allowClear: false,
                  showArrow: true,
                  className: 'switch-type-selector',
                  style: { width: 60 },
                  getPopupContainer: () => document.body,
                }}
                onChangeCb={(field: any) => {
                  switchType(field.type);
                }}
              />
            </WithAuth>
          ) : (
            <span className="mr-2 flex items-center h-full">{ISSUE_TYPE_MAP[issueType]?.icon}</span>
          )}
        </IF>

        <EditField
          name="title"
          onChangeCb={setFieldCb}
          data={formData}
          disabled={!editAuth}
          className="flex-1"
          itemProps={{
            className: 'text-xl text-normal',
            maxLength: 255,
            placeholder: specialProps.titlePlaceHolder,
          }}
        />
      </div>

      <div className="flex flex-col h-full">
        <EditField
          name="content"
          disabled={!editAuth}
          placeHolder={i18n.t('dop:no content yet')}
          type="markdown"
          onChangeCb={setFieldCb}
          itemProps={{
            hasEdited,
            isEditMode,
            maxLength: 3000,
            defaultMode: isEditMode ? 'html' : 'md',
          }} // 编辑时默认显示预览
          data={formData}
        />
      </div>
      <IF check={isEditMode}>
        <>
          {issueType === ISSUE_TYPE.REQUIREMENT ? (
            <div className="p-6">
              <div className="text-base font-medium mb-2">{i18n.t('dop:included tasks')}</div>
              <IssueRelation
                ref={ref}
                type="inclusion"
                issueDetail={issueDetail}
                iterationID={iterationID}
                onRelationChange={() => {
                  setHasEdited(true);
                }}
              />
            </div>
          ) : null}
        </>

        <Tabs className="issue-drawer-tabs" defaultActiveKey="streams">
          <TabPane tab={i18n.t('dop:activity log')} key="streams">
            <IssueCommentBox onSave={(content) => addIssueStream(issueDetail, { content })} editAuth={editAuth} />
            {issueType !== ISSUE_TYPE.TICKET ? (
              <AddRelation onSave={(data) => addIssueStream(issueDetail, data)} editAuth={editAuth} />
            ) : null}
            <IssueActivities type={issueType} />
          </TabPane>
          <TabPane tab={i18n.t('relate to issue')} key="issue">
            <IssueRelation
              ref={ref}
              type="connection"
              issueDetail={issueDetail}
              iterationID={iterationID}
              onRelationChange={() => {
                setHasEdited(true);
              }}
            />
          </TabPane>
          {issueType === ISSUE_TYPE.BUG ? (
            <TabPane tab={i18n.t('dop:relate to test case')} key="testCase">
              <IssueTestCaseRelation list={testPlanCaseRels || []} />
            </TabPane>
          ) : null}
        </Tabs>
      </IF>
      <IssueMetaFields
        ref={metaFieldsRef}
        projectId={projectId}
        labels={labels}
        isEditMode={isEditMode}
        issueType={issueType}
        isBacklog={isBacklog}
        ticketType={ticketType}
        editAuth={editAuth}
        formData={formData}
        setFieldCb={setFieldCb}
      />
    </IssueDrawer>
  );
};

export default EditIssueDrawer;
