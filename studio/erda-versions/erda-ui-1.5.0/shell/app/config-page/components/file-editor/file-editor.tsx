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
import { Button } from 'antd';
import { FileEditor as PureFileEditor } from 'common';
import { notify } from 'common/utils';
import { OperationAction } from 'config-page/utils';
import yaml from 'js-yaml';
import i18n from 'i18n';

const fileTypeReg = {
  yaml: {
    reg: (val: string) => {
      try {
        yaml.load(val);
        return true;
      } catch (e) {
        const msg = `${i18n.t('dop:input format error')}：${e.message}`;
        notify('error', <pre className="prewrap">{msg}</pre>);
        return false;
      }
    },
  },
  json: {
    reg: (val: string) => {
      try {
        JSON.parse(val);
        return true;
      } catch (_) {
        notify('error', i18n.t('dop:JSON format error'));
        return false;
      }
    },
  },
  'not-empty': {
    reg: (val: string) => {
      if (val) return true;
      notify('error', i18n.t('can not be empty'));
      return false;
    },
  },
};

const validator = (val: string, fileValidate?: CP_FILE_EDITOR.FileValidate | CP_FILE_EDITOR.FileValidate[]) => {
  if (!fileValidate) return true;
  if (Array.isArray(fileValidate)) {
    fileValidate.forEach((item) => {
      const curReg = fileTypeReg[item];
      if (curReg && !curReg.reg(val)) {
        return false;
      }
    });
    return true;
  }
  const curReg = fileTypeReg[fileValidate as string];
  return curReg && curReg.reg(val);
};

const FileEditor = (props: CP_FILE_EDITOR.Props) => {
  const { props: pProps, state, execOperation, customOp, operations } = props;
  const [value, setValue] = React.useState(state.value);
  const { bordered, fileValidate, style, ...rest } = pProps || {};

  React.useEffect(() => {
    setValue(state.value);
  }, [state.value]);

  const onChange = (val: string) => {
    setValue(val);
  };

  const getFooter = () => {
    if (operations?.submit) {
      const onSubmit = (op?: CP_COMMON.Operation) => {
        if (op) {
          if (validator(value, fileValidate)) {
            execOperation(op, { value });
            customOp?.[op.key] && customOp[op.key](op);
          }
        }
      };
      return (
        <div className="mt-2">
          <OperationAction onClick={() => onSubmit(operations.submit)} operation={operations.submit}>
            <Button type="primary">{operations.submit?.text || i18n.t('ok')}</Button>
          </OperationAction>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="flex flex-col h-full">
      <div style={style} className={`flex-1 overflow-auto h-full ${bordered ? 'border-all rounded' : ''}`}>
        <PureFileEditor autoHeight fileExtension="json" value={value} onChange={onChange} {...rest} />
      </div>
      {getFooter()}
    </div>
  );
};

export default FileEditor;
