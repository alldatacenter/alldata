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

import { Copy, IF, ErdaIcon } from 'common';
import i18n from 'i18n';
import React from 'react';
import useEvent from 'react-use/lib/useEvent';
import { WithAuth } from 'user/common';
import issueStore from 'project/stores/issues';
import { isEqual, find } from 'lodash';
import { Drawer, Spin, Popconfirm, Input, message, Popover, Button, Modal } from 'antd';
import { SubscribersSelector } from './subscribers-selector';
import layoutStore from 'layout/stores/layout';
import './issue-drawer.scss';

type ElementChild = React.ElementType | JSX.Element | string;

interface IProps {
  children: ElementChild[] | undefined[];
  visible: boolean;
  editMode: boolean;
  className?: string;
  loading?: boolean;
  canDelete?: boolean;
  canCreate?: boolean;
  shareLink?: string;
  subDrawer?: JSX.Element | null;
  confirmCloseTip?: string;
  maskClosable?: boolean;
  data: CreateDrawerData;
  issueType: string;
  projectId: string;
  onClose: (e: any) => void;
  onDelete?: () => void;
  handleCopy?: (isCopy: boolean, copyTitle: string) => void;
  setData: (data: object) => void;
  footer: ElementChild[] | ((isChanged: boolean, confirmCloseTip: string | undefined) => ElementChild[]);
}

/**
 * 任务、需求、缺陷、测试用例等Drawer
 * @note 按照title、main、tabs、meta、footer的顺序在children中书写, 不需要的区块使用 {IssueDrawer.Empty} 占位
 *
 * @param editMode 编辑模式
 * @param shareLink 分享链接，创建模式时无效
 * @param handleBlur body上捕获的blur事件监听器
 * @param className
 * @param loading
 * @param visible
 * @param onClose
 */
export const IssueDrawer = (props: IProps) => {
  const {
    className = '',
    canCreate = false,
    canDelete = false,
    subDrawer = null,
    children,
    editMode,
    shareLink,
    loading = false,
    visible,
    onClose,
    onDelete,
    confirmCloseTip,
    handleCopy,
    maskClosable,
    data,
    issueType,
    projectId,
    setData,
    footer = IssueDrawer.Empty,
    ...rest
  } = props;
  const [title = IssueDrawer.Empty, main = IssueDrawer.Empty, tabs = IssueDrawer.Empty, meta = IssueDrawer.Empty] =
    React.Children.toArray(children);
  const customFieldDetail = issueStore.useStore((s) => s.customFieldDetail);
  const isImagePreviewOpen = layoutStore.useStore((s) => s.isImagePreviewOpen);
  const [copyTitle, setCopyTitle] = React.useState('');
  const [isChanged, setIsChanged] = React.useState(false);
  const [showCopy, setShowCopy] = React.useState(false);
  const preDataRef = React.useRef(data);
  const preData = preDataRef.current;

  const escClose = React.useCallback(
    (e) => {
      if (e.keyCode === 27) {
        if (isImagePreviewOpen) {
          return;
        }
        if (isChanged && confirmCloseTip) {
          Modal.confirm({
            title: confirmCloseTip,
            onOk() {
              onClose(e);
            },
          });
        } else {
          onClose(e);
        }
      }
    },
    [isChanged, confirmCloseTip, isImagePreviewOpen, onClose],
  );

  useEvent('keydown', escClose);

  React.useEffect(() => {
    const isIssueDrawerChanged = (initData: CreateDrawerData, currentData: CreateDrawerData) => {
      setIsChanged(false);

      Object.keys(currentData).forEach((key) => {
        if (key in initData) {
          if (!isEqual(initData[key], currentData[key])) {
            setIsChanged(true);
          }
        } else {
          const defaultValue = find(customFieldDetail?.property, { propertyName: key })?.values;

          // Determine whether the field has changed. When the value is the following conditions, the field has not changed
          const notChange =
            isEqual(defaultValue, currentData[key]) ||
            currentData[key] === undefined ||
            currentData[key] === '' ||
            isEqual(currentData[key], []) ||
            isEqual(currentData[key], { estimateTime: 0, remainingTime: 0 });

          if (!notChange) {
            setIsChanged(true);
          }
        }
      });
    };
    isIssueDrawerChanged(preData, data);
  }, [customFieldDetail?.property, data, preData]);

  return (
    <Drawer
      className={`task-drawer ${className}`}
      width="70vw"
      placement="right"
      closable={false}
      visible={visible}
      onClose={onClose}
      maskClosable={maskClosable || !isChanged}
      keyboard={false}
      {...rest}
    >
      <Spin spinning={loading}>
        <IF check={title !== IssueDrawer.Empty}>
          <div className="task-drawer-header">
            <div className="flex justify-between items-center">
              <div className="flex-1 nowrap">{title}</div>
              <div className="task-drawer-op flex items-center">
                <SubscribersSelector
                  subscribers={data.subscribers}
                  issueID={customFieldDetail?.issueID}
                  issueType={issueType}
                  projectId={projectId}
                  setData={setData}
                  data={data}
                />
                <IF check={editMode && shareLink}>
                  <Copy selector=".copy-share-link" tipName={i18n.t('dop:share link')} />
                  <ErdaIcon
                    type="share-one"
                    className="cursor-copy copy-share-link mr-1 ml-3"
                    size="16"
                    data-clipboard-text={shareLink}
                  />
                </IF>
                <IF check={editMode}>
                  <WithAuth pass={canCreate}>
                    <Popover
                      title={i18n.t('dop:copy issue')}
                      visible={showCopy}
                      onVisibleChange={(v) => setShowCopy(v)}
                      content={
                        <>
                          <Input
                            placeholder={i18n.t('dop:Please enter the issue title')}
                            style={{ width: 400 }}
                            value={copyTitle}
                            onChange={(e) => setCopyTitle(e.target.value)}
                          />
                          <div className="flex items-center flex-wrap justify-end mt-2">
                            <Button
                              className="mr-2"
                              onClick={() => {
                                setCopyTitle('');
                                setShowCopy(false);
                              }}
                            >
                              {i18n.t('cancel')}
                            </Button>
                            <Button
                              onClick={() => {
                                if (copyTitle === '') {
                                  message.error(i18n.t('dop:The title can not be empty'));
                                  return;
                                }
                                handleCopy && handleCopy(true, copyTitle);
                                setCopyTitle('');
                                setShowCopy(false);
                              }}
                              type="primary"
                            >
                              {i18n.t('copy')}
                            </Button>
                          </div>
                        </>
                      }
                      placement="leftTop"
                      trigger="click"
                    >
                      <ErdaIcon type="copy" className="hover-active ml-3" size="16px" />
                    </Popover>
                  </WithAuth>
                </IF>
                {onDelete ? (
                  <WithAuth pass={canDelete}>
                    <Popconfirm
                      title={`${i18n.t('common:confirm deletion')}?`}
                      placement="bottomRight"
                      onConfirm={onDelete}
                    >
                      <ErdaIcon type="delete1" className="hover-active ml-3" size="16" />
                    </Popconfirm>
                  </WithAuth>
                ) : null}
                {isChanged && confirmCloseTip ? (
                  <Popconfirm title={confirmCloseTip} placement="bottomRight" onConfirm={onClose}>
                    <ErdaIcon type="close" className="ml-3 cursor-pointer" size="16" />
                  </Popconfirm>
                ) : (
                  <ErdaIcon type="close" className="ml-3 cursor-pointer" size="16" onClick={onClose} />
                )}
              </div>
            </div>
          </div>
        </IF>
        {subDrawer}
        <div className="task-drawer-body" style={footer !== IssueDrawer.Empty ? { paddingBottom: '60px' } : {}}>
          <div className="task-drawer-main">
            <div className="task-drawer-content">{main}</div>
            <div className="task-drawer-tabs">{tabs}</div>
          </div>
          <IF check={meta !== IssueDrawer.Empty}>
            <div className="task-drawer-meta">{meta}</div>
          </IF>
        </div>
      </Spin>
      <IF check={footer !== IssueDrawer.Empty}>
        <div className="task-drawer-footer">
          {typeof footer === 'function' ? footer(isChanged, confirmCloseTip) : footer}
        </div>
      </IF>
    </Drawer>
  );
};

IssueDrawer.Empty = '__Empty__';
