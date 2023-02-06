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
import 'ace-builds';
import AceEditor, { IAceEditorProps } from 'react-ace';
import { Icon as CustomIcon, Copy } from 'common';
import { Tooltip } from 'antd';
import { compact } from 'lodash';
import { isValidJsonStr } from 'common/utils';
import i18n from 'i18n';

import 'ace-builds/src-noconflict/ext-searchbox';
import highlight from 'ace-builds/src-noconflict/ext-static_highlight';
import 'ace-builds/src-noconflict/theme-github';

import javascriptMode from 'ace-builds/src-noconflict/mode-javascript';
import javaMode from 'ace-builds/src-noconflict/mode-java';
import kotlinMode from 'ace-builds/src-noconflict/mode-kotlin';
import xmlMode from 'ace-builds/src-noconflict/mode-xml';
import sassMode from 'ace-builds/src-noconflict/mode-sass';
import mysqlMode from 'ace-builds/src-noconflict/mode-mysql';
import jsonMode from 'ace-builds/src-noconflict/mode-json';
import htmlMode from 'ace-builds/src-noconflict/mode-html';
import golangMode from 'ace-builds/src-noconflict/mode-golang';
import cssMode from 'ace-builds/src-noconflict/mode-css';
import yamlMode from 'ace-builds/src-noconflict/mode-yaml';
import shMode from 'ace-builds/src-noconflict/mode-sh';
import typescriptMode from 'ace-builds/src-noconflict/mode-typescript';
import svgMode from 'ace-builds/src-noconflict/mode-svg';
import jsxMode from 'ace-builds/src-noconflict/mode-jsx';
import tsxMode from 'ace-builds/src-noconflict/mode-tsx';
import lessMode from 'ace-builds/src-noconflict/mode-less';
import rubyMode from 'ace-builds/src-noconflict/mode-ruby';
import pythonMode from 'ace-builds/src-noconflict/mode-python';
import dockerfileMode from 'ace-builds/src-noconflict/mode-dockerfile';

import './index.scss';

const supportLang = [
  'javascript',
  'java',
  'kotlin',
  'xml',
  'sass',
  'mysql',
  'json',
  'html',
  'golang',
  'css',
  'yaml',
  'sh',
  'typescript',
  'svg',
  'jsx',
  'tsx',
  'less',
  'ruby',
  'python',
  'dockerfile',
];
const modeMap = {
  'ace/mode/javascript': javascriptMode,
  'ace/mode/java': javaMode,
  'ace/mode/kotlin': kotlinMode,
  'ace/mode/xml': xmlMode,
  'ace/mode/sass': sassMode,
  'ace/mode/mysql': mysqlMode,
  'ace/mode/json': jsonMode,
  'ace/mode/html': htmlMode,
  'ace/mode/golang': golangMode,
  'ace/mode/css': cssMode,
  'ace/mode/yaml': yamlMode,
  'ace/mode/sh': shMode,
  'ace/mode/typescript': typescriptMode,
  'ace/mode/svg': svgMode,
  'ace/mode/jsx': jsxMode,
  'ace/mode/tsx': tsxMode,
  'ace/mode/less': lessMode,
  'ace/mode/ruby': rubyMode,
  'ace/mode/python': pythonMode,
  'ace/mode/dockerfile': dockerfileMode,
};

const extMap = {
  yml: 'yaml',
  kt: 'kotlin',
  js: 'javascript',
  ts: 'typescript',
  go: 'golang',
  sql: 'mysql',
  rb: 'ruby',
  py: 'python',
  Dockerfile: 'dockerfile',
};

// fileEditor外层多包了一个div，用于宽高的设定，传了style，但是不需要其他属性影响子元素的样式
const filterSizeStyle = (style: Obj) => {
  const { height, width, minHeight, maxHeight, minWidth, maxWidth } = style || {};

  return {
    width,
    height,
    minHeight,
    minWidth,
    maxHeight,
    maxWidth,
  };
};

interface IActions {
  copy?: boolean;
  format?: boolean;
  extra?: React.ReactNode;
}

interface IProps extends IAceEditorProps {
  fileExtension: string;
  editorProps?: object;
  autoHeight?: boolean;
  options?: object;
  className?: string;
  value?: string;
  actions?: IActions;
  [prop: string]: any;
}

const valueExceed = (val: string) => {
  const bt = new Blob([val]);
  // more than 500kb
  return bt.size > 500 * 1024;
};

const FileEditor = ({
  fileExtension,
  editorProps,
  options,
  autoHeight = false,
  className,
  style: editorStyle,
  valueLimit = true,
  value,
  actions = {},
  ...rest
}: IProps) => {
  const _rest = { ...rest };
  const style: any = { width: '100%', lineHeight: '1.8', ...editorStyle };
  let mode = extMap[fileExtension] || fileExtension || 'sh';
  if (!supportLang.includes(mode)) {
    mode = 'sh';
  }
  React.useEffect(() => {
    if (!_rest.readOnly) {
      setTimeout(() => {
        // 编辑模式最后一行无法显示，很诡异的问题，需要主动触发一下resize
        window.dispatchEvent(new Event('resize'));
      }, 1000);
    }
  }, [_rest.readOnly]);
  const preDom = React.useRef(null);
  React.useEffect(() => {
    if (preDom.current && value) {
      highlight(preDom.current, {
        mode: new modeMap[`ace/mode/${mode}`].Mode(),
        theme: 'ace/theme/github',
        startLineNumber: 1,
        showGutter: true,
        trim: true,
      });
    }
  }, [mode, value]);

  if (valueLimit && value && valueExceed(value)) {
    const downloadValue = () => {
      const blob = new Blob([value], { type: 'application/vnd.ms-excel;charset=utf-8' });
      const fileName = `result.txt`;
      const objectUrl = URL.createObjectURL(blob);
      const downloadLink = document.createElement('a');
      downloadLink.href = objectUrl;
      downloadLink.setAttribute('download', fileName);
      document.body.appendChild(downloadLink);
      downloadLink.click();
      window.URL.revokeObjectURL(downloadLink.href);
    };

    return (
      <div>
        {i18n.t('common:the current data volume exceeds 500kb, please click')}&nbsp;
        <span className="fake-link" onClick={downloadValue}>
          {i18n.t('download')}
        </span>
        &nbsp;
        {i18n.t('check detail')}
      </div>
    );
  }

  const curActions = compact([
    actions.copy ? (
      <Tooltip title={i18n.t('copy')} key="copy">
        <CustomIcon type="fz1" className="text-lg hover-active cursor-copy" data-clipboard-text={value} />
      </Tooltip>
    ) : null,
    actions.format ? (
      <Tooltip title={i18n.t('format')} key="format">
        <CustomIcon
          type="sx"
          className="text-lg hover-active"
          onClick={() => {
            value &&
              isValidJsonStr(value) &&
              rest.onChange &&
              rest.onChange(JSON.stringify(JSON.parse(value), null, 2));
          }}
        />
      </Tooltip>
    ) : null,
  ]);

  const ActionComp =
    curActions.length || actions.extra ? (
      <div className="flex justify-between items-center file-editor-actions">
        {curActions}
        {actions.extra || null}
      </div>
    ) : null;

  if (_rest.readOnly) {
    return value ? (
      <div style={filterSizeStyle(style)} className={`file-editor-container ${className}`}>
        <Copy selector=".cursor-copy" />
        <pre data-mode={mode} ref={preDom} style={style}>
          {value}
        </pre>
        {ActionComp}
      </div>
    ) : (
      <pre style={{ height: '300px' }} />
    );
  }
  if (autoHeight) {
    style.height = '100%';
  } else if (!_rest.maxLines) {
    _rest.maxLines = 30;
  }
  return (
    <div style={filterSizeStyle(style)} className={`file-editor-container ${className}`}>
      <AceEditor
        mode={mode}
        theme="github"
        fontSize={12}
        style={style}
        editorProps={{ $blockScrolling: true, ...editorProps }}
        setOptions={{ ...options, useWorker: false }} // useWorker为true时切换编辑模式会有个报错
        value={value}
        {..._rest}
      />
      {ActionComp}
      <Copy selector=".cursor-copy" />
    </div>
  );
};

export default FileEditor;
