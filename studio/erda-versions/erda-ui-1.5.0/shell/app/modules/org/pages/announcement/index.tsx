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

import React, { useCallback, useEffect } from 'react';
import { Button, Input, Popconfirm, Table, Spin } from 'antd';
import { PaginationConfig } from 'core/common/interface';
import moment from 'moment';
import { get, throttle } from 'lodash';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import announcementStore from 'org/stores/announcement';
import './index.scss';
import { PAGINATION } from 'app/constants';
import layoutStore from 'app/layout/stores/layout';

const { Search } = Input;

const defaultPageSize = PAGINATION.pageSize;

interface Column {
  title: string;
  dataIndex: string;
  className?: string;
  width?: number;
  render?: (value: string, record: ORG_ANNOUNCEMENT.Item) => {};
}

const statusMap = {
  published: i18n.t('cmp:notice published'),
  unpublished: i18n.t('cmp:notice unpublished'),
  deprecated: i18n.t('cmp:notice deprecated'),
};

const columns: Column[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 72,
  },
  {
    title: i18n.t('cmp:announcement content'),
    dataIndex: 'content',
  },
  {
    title: i18n.t('cmp:notice createdAt'),
    dataIndex: 'createdAt',
    width: 180,
    render(value) {
      return moment(value).format('YYYY-MM-DD HH:mm:ss');
    },
  },
  {
    title: i18n.t('cmp:notice status'),
    dataIndex: 'status',
    width: 100,
    render(value) {
      return statusMap[value];
    },
  },
];

const NoticeManage = () => {
  const [{ showModal, searchKey, noticeId, formData }, updater, update] = useUpdate({
    showModal: false,
    searchKey: '',
    formData: {},
    noticeId: undefined,
  });
  const [list, { pageNo, total, pageSize }] = announcementStore.useStore((s) => [s.list, s.noticePaging]);
  const { effects } = announcementStore;
  const [loading] = useLoading(announcementStore, ['getAnnouncementList']);
  useEffect(() => {
    effects.getAnnouncementList({ content: searchKey, pageNo: 1, pageSize: defaultPageSize });
  }, [effects, searchKey]);

  const handleOk = ({ content }: { content: string }) => {
    const saveService = noticeId ? effects.updateAnnouncement : effects.addAnnouncement;
    saveService({ id: noticeId, content }).then(() => {
      updater.showModal(false);
      updateList(1);
    });
  };

  const updateList = (current: number) => {
    effects.getAnnouncementList({ content: searchKey, pageNo: current, pageSize: defaultPageSize });
  };

  /**
   * 搜索公告
   * @param event {React.ChangeEvent<HTMLInputElement>}
   */
  const onSearchKeyChange = throttle((event: React.ChangeEvent<HTMLInputElement>) => {
    const value = get(event, 'target.value');
    update({
      searchKey: value,
    });
  }, 200);
  const editNotice = (id: any, content: any) => {
    update({
      formData: id ? { content } : {},
      noticeId: id,
      showModal: true,
    });
    // toggleModal(true);
  };

  const updatePublishedList = async () => {
    updateList(1);
    // after stop announcement, required to update published announcement list
    const list = await effects.getAllNoticeListByStatus('published');
    layoutStore.reducers.setAnnouncementList(list);
  };

  const publishAnnouncement = async (id: number) => {
    await effects.publishAnnouncement({ id });
    updatePublishedList();
  };

  const deprecateNotice = async (id: number) => {
    await effects.unPublishAnnouncement({ id });
    updatePublishedList();
  };

  /**
   * 删除公告
   * @param id {number}
   */
  const deleteAnnouncement = (id: number) => {
    effects.deleteAnnouncement({ id }).then(() => {
      updateList(1);
    });
  };

  const fieldList = [
    {
      label: i18n.t('cmp:announcement content'),
      name: 'content',
      required: true,
      type: 'textArea',
      itemProps: {
        maxLength: 1000,
      },
    },
  ];
  // 大于一页显示分页
  const pagination: PaginationConfig = {
    total,
    pageSize,
    current: pageNo,
    onChange: (page: number) => {
      updateList(page);
    },
  };
  const opCol: Column[] = [
    {
      title: i18n.t('cmp:notice operation'),
      dataIndex: 'orgID',
      width: 160,
      render(_value, { status, content, id }) {
        return (
          <div className="operation-td">
            {status === 'published' ? (
              <Popconfirm
                title={`${i18n.t('cmp:confirm to deprecate')}?`}
                onConfirm={() => {
                  deprecateNotice(id);
                }}
              >
                <span>{i18n.t('cmp:notice deprecate')}</span>
              </Popconfirm>
            ) : (
              <>
                <span
                  onClick={() => {
                    publishAnnouncement(id);
                  }}
                >
                  {i18n.t('cmp:notice publish')}
                </span>
                <span
                  onClick={() => {
                    editNotice(id, content);
                  }}
                >
                  {i18n.t('cmp:notice edit')}
                </span>
                <Popconfirm
                  title={`${i18n.t('common:confirm to delete')}?`}
                  onConfirm={() => {
                    deleteAnnouncement(id);
                  }}
                >
                  <span>{i18n.t('cmp:notice delete')}</span>
                </Popconfirm>
              </>
            )}
          </div>
        );
      },
    },
  ];
  return (
    <div className="org-notice-manage">
      <Spin spinning={loading}>
        <div className="top-button-group">
          <Button
            type="primary"
            onClick={() => {
              editNotice(undefined, undefined);
            }}
          >
            {i18n.t('cmp:create announcement')}
          </Button>
        </div>
        <div className="notice-filter">
          <Search
            className="data-select"
            value={searchKey}
            onChange={onSearchKeyChange}
            placeholder={i18n.t('cmp:search by content')}
          />
        </div>
        <Table
          rowKey="id"
          columns={[...columns, ...opCol]}
          dataSource={list}
          pagination={pagination}
          scroll={{ x: '100%' }}
        />
        <FormModal
          formData={formData}
          name={i18n.t('cmp:announcement management')}
          fieldsList={fieldList}
          visible={showModal}
          onCancel={useCallback(() => {
            updater.showModal(false);
          }, [updater])}
          modalProps={{
            destroyOnClose: true,
            maskClosable: false,
          }}
          onOk={useCallback(handleOk, [noticeId])}
        />
      </Spin>
    </div>
  );
};

export default NoticeManage;
