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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { Icon as CustomIcon, EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Spin, Tooltip, Pagination } from 'antd';
import { useDrop } from 'react-dnd';
import moment from 'moment';
import iterationStore from 'project/stores/iteration';
import issueStore from 'project/stores/issues';
import { Form } from 'dop/pages/form-editor/index';
import i18n from 'i18n';
import { isEmpty, map } from 'lodash';
import { IssueItem, BACKLOG_ISSUE_TYPE } from './issue-item';
import EditIssueDrawer, { CloseDrawerParam } from 'project/common/components/issue/edit-issue-drawer';
import { mergeSearch } from 'common/utils';
import { ISSUE_ICON } from 'project/common/components/issue/issue-config';
import './iteration-item.scss';

interface IProps {
  data: ITERATION.Detail;
  onEdit: (data: ITERATION.Detail) => void;
  deleteItem: (data: ITERATION.Detail) => void;
}

const noop = () => {};
export const IterationItem = (props: IProps) => {
  const { data } = props;

  const { getIterationsIssues } = iterationStore.effects;
  const { updateIssue } = issueStore.effects;
  const issuesMap = iterationStore.useStore((s) => s.issuesMap);
  const { total, list } = issuesMap[data.id] || { total: 0, list: [] };

  const [{ isOpen, loading, curIssueDetail, drawerVisible, pageNo }, updater, update] = useUpdate({
    isOpen: false,
    loading: false,
    curIssueDetail: {} as ISSUE.Issue,
    drawerVisible: false,
    pageNo: 1,
  });

  React.useEffect(() => {
    if (isOpen) {
      getIssues();
    }
  }, [isOpen]);

  React.useEffect(() => {
    getIssues();
  }, [pageNo]);

  const onIssueDrop = (val: ISSUE.IssueType) => {
    return updateIssue({ ...val, iterationID: data.id }).then(() => {
      isOpen ? getIssues() : updater.isOpen(true);
    });
  };

  const [{ isOver }, drop] = useDrop({
    accept: BACKLOG_ISSUE_TYPE.undoneIssue,
    drop: (item: any) => ({ res: onIssueDrop(item.data) }), // drop需要返回一个Obj，如果直接返回Promise是无效的
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });

  const getIssues = () => {
    updater.loading(true);
    getIterationsIssues({ iterationId: data.id, pageNo }).finally(() => {
      updater.loading(false);
    });
  };

  const onClickIssue = (val: ISSUE.Issue) => {
    update({
      drawerVisible: true,
      curIssueDetail: val,
    });
  };

  const closeDrawer = ({ hasEdited, isCreate, isDelete }: CloseDrawerParam) => {
    update({
      drawerVisible: false,
      curIssueDetail: {},
    });
    if (hasEdited || isCreate || isDelete) {
      getIssues();
    }
  };

  return (
    <div className={`backlog-iteration-item-container ${isOver ? 'drag-over' : ''}`} ref={drop}>
      <div
        className="backlog-iteration-item flex justify-between items-center hover-active-bg"
        onClick={() => updater.isOpen(!isOpen)}
      >
        <div className={'iteration-info h-full'}>
          <CustomIcon type="chevron-down" className={`open-icon ${isOpen ? 'open' : 'close'}`} />
          {ISSUE_ICON.iteration}
          {data ? (
            <Tooltip title={data.title}>
              <div className="font-bold nowrap">{data.title}</div>
            </Tooltip>
          ) : null}
        </div>
        <div className="iteration-time-duration h-full ml-2 mr-2 text-sub">
          {`${moment(data.startedAt).format('YYYY/MM/DD')} - ${moment(data.finishedAt).format('YYYY/MM/DD')}`}
        </div>
      </div>
      <div className={`backlog-iteration-issues ${isOpen ? '' : 'hidden'}`}>
        <Spin spinning={loading}>
          {isEmpty(list) ? (
            <EmptyHolder relative />
          ) : (
            map(list, (item) => (
              <IssueItem
                key={item.id}
                data={item}
                issueType={BACKLOG_ISSUE_TYPE.iterationIssue}
                onDragDelete={getIssues}
                onClickIssue={onClickIssue}
              />
            ))
          )}
        </Spin>
        <div>
          {total && (
            <Pagination
              className="flex items-center flex-wrap justify-end pt-2"
              simple
              defaultCurrent={1}
              total={total}
              pageSize={20}
              onChange={(_page: number) => {
                update({ pageNo: _page });
              }}
            />
          )}
        </div>
      </div>
      {drawerVisible ? (
        <EditIssueDrawer
          iterationID={data.id}
          id={curIssueDetail.id}
          shareLink={`${location.href.split('?')[0]}?${mergeSearch(
            { id: curIssueDetail.id, issueType: curIssueDetail.type },
            true,
          )}`}
          issueType={curIssueDetail.type}
          visible={drawerVisible}
          closeDrawer={closeDrawer}
        />
      ) : null}
    </div>
  );
};

interface IIterationFormProps {
  onCancel: () => void;
  onOk: (data: ITERATION.CreateBody) => void;
}

export const IterarionForm = (props: IIterationFormProps) => {
  const { onCancel = noop, onOk = noop } = props;
  const formRef = React.useRef(null as any);
  const fields = [
    {
      label: '',
      component: 'input',
      key: 'title',
      required: true,
      componentProps: {
        placeholder: i18n.t('dop:please enter a name'),
        maxLength: 255,
        size: 'small',
        className: 'backlog-iteration-form-title',
      },
      type: 'input',
      requiredCheck: (v: string) => [v !== undefined && v !== '', ''],
    },
    {
      label: '',
      component: 'datePicker',
      key: 'time',
      required: true,
      componentProps: {
        dateType: 'range',
        placeholder: [i18n.t('common:start at'), i18n.t('common:end at')],
        format: 'YYYY-MM-DD',
        size: 'small',
        className: 'backlog-iteration-form-datepicker',
      },
      type: 'input',
      requiredCheck: (v: string[]) => [!isEmpty(v), ''],
    },
  ];
  const onAdd = () => {
    const curForm = formRef && formRef.current;
    if (curForm) {
      curForm.onSubmit((val: any) => {
        const { time, ...rest } = val;
        onOk({
          startedAt: moment(time[0]).startOf('day').format(),
          finishedAt: moment(time[1]).endOf('day').format(),
          ...rest,
        });
      });
    }
  };
  return (
    <div className="backlog-iteration-item flex justify-between items-center hover-active-bg">
      <div className={'iteration-info h-full'}>
        <CustomIcon type="chevron-down" className={'open-icon close'} />
        {ISSUE_ICON.iteration}
        <Form fields={fields} formRef={formRef} formProps={{ layout: 'inline', className: 'backlog-iteration-form' }} />
      </div>
      <div className="table-operations ml-2">
        <span className="table-operations-btn" onClick={onAdd}>
          {i18n.t('save')}
        </span>
        <span className="table-operations-btn" onClick={onCancel}>
          {i18n.t('cancel')}
        </span>
      </div>
    </div>
  );
};
