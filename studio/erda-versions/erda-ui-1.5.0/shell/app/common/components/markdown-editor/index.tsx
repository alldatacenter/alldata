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

import i18n from 'i18n';
import { Button, message } from 'antd';
import React, { ReactElement } from 'react';
import Editor from './editor';
import { useUpdate } from 'app/common/use-hooks';
import { map } from 'lodash';
import { useMount } from 'react-use';
import { BaseButtonProps } from 'antd/es/button/button';
import './index.scss';

interface BtnProps extends BaseButtonProps {
  text: string;
  onClick: (v: string) => void;
}
interface IProps {
  value?: string | null;
  placeholder?: string;
  maxLength?: number;
  defaultMode?: 'md' | 'html';
  extraRight?: ReactElement | ReactElement[];
  readOnly?: boolean;
  autoFocus?: boolean;
  style?: React.CSSProperties;
  operationBtns?: BtnProps[];
  showMenu?: boolean;
  defaultHeight?: number;
  onChange?: (value: string) => void;
  onFocus?: (e: any) => void;
  onBlur?: (value: any) => void;
}

interface IState {
  content: string;
  tempContent: string;
  view: {
    md: boolean;
    html: boolean;
    menu: boolean;
  };
}

export interface EC_MarkdownEditor {
  clear: () => void;
}

const MarkdownEditor: React.ForwardRefRenderFunction<EC_MarkdownEditor, IProps> = (
  {
    placeholder,
    readOnly,
    extraRight,
    style,
    defaultMode = 'md',
    autoFocus,
    value,
    maxLength,
    operationBtns,
    showMenu = true,
    defaultHeight,
    onChange,
    onFocus,
    onBlur,
  },
  ref,
) => {
  const mdEditorRef = React.useRef<any>(null); // TODO ts type should export from @erda-ui/react-markdown-editor-lite

  const [{ content, view, tempContent }, updater] = useUpdate<IState>({
    content: value || '',
    tempContent: value || '',
    view: {
      md: defaultMode === 'md',
      html: defaultMode === 'html',
      menu: showMenu,
    },
  });

  useMount(() => {
    setTimeout(() => {
      const mdRef = mdEditorRef?.current;
      mdRef?.on('viewchange', (_view: { html: boolean; md: boolean; menu: boolean }) => {
        updater.view(_view);
        if (_view.md) {
          mdRef.nodeMdText.current && mdRef.nodeMdText.current.focus();
        }
      });

      if (autoFocus && view.md && mdRef.nodeMdText.current) {
        mdRef?.nodeMdText.current.focus();
      }
    });
  });

  React.useImperativeHandle(
    ref,
    () => ({
      clear: () => updater.content(''),
    }),
    [updater],
  );

  React.useEffect(() => {
    if (value !== tempContent) {
      updater.content(value || '');
      updater.tempContent(value || '');
    }
  }, [tempContent, updater, value]);

  const onChangeContent = (data: { html: string; text: string }) => {
    let v = data.text;
    if (maxLength && data.text.length > maxLength) {
      message.warn(i18n.t('common:The maximum length is {limit}, please upload with attachment', { limit: maxLength }));
      v = data.text.slice(0, maxLength);
    }
    updater.content(v);
    onChange?.(v);
  };

  const disableEdit = view.html && !view.md; // 纯预览模式时禁用操作栏

  const curShowButton = !!operationBtns?.length;

  // const height: string | number = style.height ? parseInt(style.height, 10) : 400;
  // height = view.menu ? (view.md && curShowButton ? height + 50 : height) : 'auto';

  return (
    <div className="markdown-editor relative">
      <div
        className={`markdown-editor-content flex flex-col ${disableEdit ? 'disable-edit' : ''} ${
          readOnly ? 'read-only' : ''
        } ${curShowButton ? 'show-btn' : ''}`}
      >
        <Editor
          ref={mdEditorRef}
          {...{
            placeholder,
            readOnly,
            extraRight,
            onFocus,
            style,
          }}
          config={{
            view,
          }}
          defaultHeight={defaultHeight || 400}
          value={content}
          onChange={onChangeContent}
          onBlur={() => onBlur?.(content)}
        />
        <div className="absolute left-2 flex bottom-2 space-x-2">
          {map(operationBtns, (operationBtn, i) => {
            const { text, type, onClick } = operationBtn;
            return (
              <Button key={i} type={type} onClick={() => onClick(content)}>
                {text}
              </Button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default React.forwardRef<EC_MarkdownEditor, IProps>(MarkdownEditor);
