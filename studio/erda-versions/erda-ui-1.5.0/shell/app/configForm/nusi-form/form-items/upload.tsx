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

import { Form, Upload } from 'antd';
import React from 'react';
import { get } from 'lodash';
import { getLabel, noop } from './common';
import { Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import { commonFields, rulesField, checkWhen } from './common/config';
import { getUploadProps } from 'common/utils/upload-props';
import './upload.scss';

const FormItem = Form.Item;

export const FormUpload = ({
  fixOut = noop,
  fixIn = noop,
  extensionFix,
  requiredCheck,
  trigger = 'onChange',
}: any = {}) =>
  React.memo(({ fieldConfig, form }: any = {}) => {
    const {
      key,
      handleBeforeUpload,
      value,
      label,
      visible,
      valid = [],
      disabled,
      required,
      registerRequiredCheck = noop,
      componentProps,
      wrapperProps,
      labelTip,
      fixIn: itemFixIn,
      fixOut: itemFixOut,
      requiredCheck: _requiredCheck,
    } = fieldConfig || {};

    const curFixIn = itemFixIn || fixIn;
    const curFixOut = itemFixOut || fixOut;
    const [loading, setLoading] = React.useState(false);
    const [imageUrl, setImageUrl] = React.useState('');

    React.useEffect(() => {
      setImageUrl(value);
    }, [value]);

    registerRequiredCheck(_requiredCheck || requiredCheck);
    const { uploadText, sizeLimit, supportFormat = [] } = componentProps || {};
    const _placeholder = uploadText || '上传图片';
    const accept = supportFormat.map((x) => `.${x}`).join(',');

    const uploadButton = (
      <div className="form-item-upload-button">
        <CustomIcon type="cir-add" className="text-xl" />
        <div>{_placeholder}</div>
      </div>
    );

    function handleChange(info: any) {
      if (info.file.status === 'done') {
        const { response } = info.file;
        if (!response) {
          return;
        }
        // FIXME: 为什么要将 http(s) 去掉？
        const url = (get(response, 'data.url') || '').replace(/^http(s)?:/g, '');
        setImageUrl(url);
        form.setFieldValue(key, curFixOut(url));
        componentProps?.onChange?.(url);
      }
    }

    return (
      <FormItem
        colon
        label={getLabel(label, labelTip)}
        className={visible ? '' : 'hidden'}
        validateStatus={valid[0]}
        help={valid[1]}
        required={required}
        {...wrapperProps}
      >
        <Upload listType="picture" accept={accept} onChange={handleChange} {...getUploadProps({}, sizeLimit)}>
          {imageUrl ? <img src={imageUrl} alt="avatar" className="form-item-upload-img" /> : uploadButton}
          <div className="form-item-upload-tip">
            支持格式: {supportFormat?.join(' / ')}，不超过 {sizeLimit} M
          </div>
        </Upload>
      </FormItem>
    );
  });

export const config = {
  name: 'upload',
  Component: FormUpload, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [value !== undefined && value !== '', i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value, options) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  upload: {
    name: '上传',
    value: 'upload',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields, rulesField, ...checkWhen],
      },
      componentProps: {
        key: 'componentProps',
        name: '组件配置',
        fields: [
          {
            label: '上传文案',
            key: 'componentProps.uploadText',
            type: 'input',
            component: 'input',
          },
          {
            label: '文件大小限制（M）',
            key: 'componentProps.sizeLimit',
            type: 'inputNumber',
            component: 'inputNumber',
          },
          {
            label: '支持格式',
            component: 'select',
            required: true,
            key: 'componentProps.supportFormat',
            componentProps: {
              mode: 'multiple',
              placeholder: '请选择支持格式',
              options: [
                {
                  name: 'png',
                  desc: '',
                  value: 'png',
                },
                {
                  name: 'jpg',
                  desc: '',
                  value: 'jpg',
                },
                {
                  name: 'jpeg',
                  desc: '',
                  value: 'jpeg',
                },
                {
                  name: 'gif',
                  desc: '',
                  value: 'gif',
                },
                {
                  name: 'bmp',
                  desc: '',
                  value: 'bmp',
                },
              ],
            },
            dataSource: {
              type: 'static',
            },
          },
        ],
      },
    },
  },
};
