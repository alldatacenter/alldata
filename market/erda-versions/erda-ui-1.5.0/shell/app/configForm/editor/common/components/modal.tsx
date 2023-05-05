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
import './modal.scss';

interface IProps {
  visible: boolean;
  maskClosable?: boolean;
  onCancel: () => void;
  onOk: () => void;
  children?: any;
  title?: string;
  width?: number;
}

export const Modal = (props: IProps) => {
  const { visible, maskClosable = false, onCancel, onOk, children, title, width = 600 } = props;
  const clickMask = () => {
    maskClosable && onCancel();
  };

  const clickOk = () => {
    onOk();
  };

  return (
    <div className={`dice-form-modal ${visible ? 'open' : ''}`}>
      <div className="mask" onClick={clickMask}>
        <div className={'container shallow-shadow'} style={{ width }} onClick={(e) => e.stopPropagation()}>
          <div className="title">
            {title}
            <span className="close" onClick={onCancel}>
              x
            </span>
          </div>
          <div className="content">{visible ? children : null}</div>
          <div className="footer">
            <button className="dice-form-editor-button footer-btn" onClick={onCancel}>
              取消
            </button>
            <button className="dice-form-editor-button footer-btn primary" onClick={clickOk}>
              确定
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
