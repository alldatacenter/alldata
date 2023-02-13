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
import Clipboard from 'clipboard';
import { isString } from 'lodash';
import { message } from 'antd';
import i18n from 'i18n';

const selectorMap = {};
const innerClassName = 'cursor-copy';
const innerSelector = `.${innerClassName}`;

interface IProps {
  selector?: string;
  opts?: object;
  copyText?: string;
  className?: string;
  tipName?: string;
  onSuccess?: (e: React.SyntheticEvent<HTMLSpanElement, Event>) => void;
  onError?: (e: React.SyntheticEvent<HTMLSpanElement, Event>) => void;
  onEdit?: () => void;
}
class Copy extends React.PureComponent<IProps> {
  clipboard: any;

  selector: string | '.cursor-copy';

  componentDidMount() {
    this.initClipBoard();
  }

  componentDidUpdate() {
    this.initClipBoard();
  }

  componentWillUnmount() {
    if (this.clipboard) {
      this.clipboard.destroy();
      selectorMap[this.selector] = undefined;
    }
  }

  initClipBoard() {
    const { children, selector, opts = {}, onSuccess, onError, tipName = '' } = this.props;
    // click event bind on body, make sure one selector only trigger once
    this.selector = isString(children) ? innerSelector : selector || innerSelector;
    if (this.selector && !selectorMap[this.selector]) {
      selectorMap[this.selector] = true;
      this.clipboard = new Clipboard(this.selector, opts);
      this.clipboard.on('success', (e: any) => {
        if (typeof onSuccess === 'function') {
          onSuccess(e);
        }
        message.success(
          `${i18n.t('copy')} ${e.trigger.getAttribute('data-clipboard-tip') || tipName} ${i18n.t('succeed')}`,
          1,
        );
        e.clearSelection();
      });
      this.clipboard.on('error', (e: any) => {
        if (typeof onError === 'function') {
          onError(e);
        }
        message.error(
          `${i18n.t('copy')} ${e.trigger.getAttribute('data-clipboard-tip') || tipName} ${i18n.t('failed')}`,
          1,
        );
      });
    }
  }

  render() {
    // 增加被复制项，需求为有时children超长显示不全
    const { children, copyText, className = '', ...rest } = this.props;
    return isString(children) ? (
      <span className={`${innerClassName} ${className}`} data-clipboard-text={copyText || children} {...rest}>
        {children}
      </span>
    ) : (
      children || null
    );
  }
}

export default Copy;
