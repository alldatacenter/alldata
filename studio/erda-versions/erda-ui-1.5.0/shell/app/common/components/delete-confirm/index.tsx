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
import { Modal } from 'antd';
import { get } from 'lodash';
import i18n from 'i18n';

interface IProps {
  [proName: string]: any;
  title?: string;
  secondTitle?: JSX.Element | string;
  okText?: string;
  cancelText?: string;
  onShow?: any;
  countDown?: number;
  onConfirm?: () => void;
  onCancel?: () => void;
}

const { confirm } = Modal;
const noop = () => {};

const DeleteConfirm = (props: IProps) => {
  const { children, countDown = 0 } = props;

  const timer = React.useRef();

  if (!children || React.Children.count(children) > 1) {
    // eslint-disable-next-line no-console
    console.error('DeleteComfirm : children should be a single valid element!');
    return children;
  }

  const childClickEvent = get(children, 'props.onClick') || noop;

  const showModal = (e: any) => {
    e.stopPropagation();
    childClickEvent(e);
    const { title, secondTitle, onConfirm = noop, onCancel = noop, okText, cancelText, onShow } = props;

    if (onShow && typeof onShow === 'function') {
      onShow();
    }
    let _countDown = countDown;
    const _okText = okText || i18n.t('common:yes');
    const modalRef = confirm({
      title: title || `${i18n.t('common:confirm deletion')}？`,
      content: secondTitle || `${i18n.t('common:confirm this action')}？`,
      onOk() {
        onConfirm();
      },
      onCancel() {
        clearInterval(timer.current);
        onCancel();
      },
      okText: _countDown > 0 ? `${_okText}(${_countDown})` : _okText,
      cancelText: cancelText || i18n.t('common:no'),
      okButtonProps: {
        disabled: _countDown > 0,
      },
    });
    timer.current = setInterval(() => {
      _countDown -= 1;
      if (_countDown >= 0) {
        modalRef.update({
          okText: _countDown > 0 ? `${_okText}(${_countDown})` : _okText,
          okButtonProps: {
            disabled: _countDown > 0,
          },
        });
      } else {
        clearInterval(timer.current);
      }
    }, 1000) as any;
  };
  return React.cloneElement(React.isValidElement(children) ? children : <span>{children}</span>, {
    onClick: showModal,
  });
};

export default DeleteConfirm;
