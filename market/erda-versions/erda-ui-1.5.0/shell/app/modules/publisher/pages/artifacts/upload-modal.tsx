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

/**
 * Created by 含光<jiankang.pjk@alibaba-inc.com> on 2021/1/25 17:57.
 */
import React from 'react';
import { Button, message, Upload } from 'antd';
import { getUploadProps } from 'common/utils/upload-props';
import { FormInstance } from 'core/common/interface';
import { FormModal, IFormItem, ErdaIcon } from 'common';
import publisherStore from 'publisher/stores/publisher';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';

export interface IProps {
  visible: boolean;
  onCancel: () => void;
  afterUpload: (type: PUBLISHER.OfflinePackageType) => void;
}

const UploadModal = (props: IProps) => {
  const { visible, onCancel, afterUpload } = props;
  const [uploadFile, setUploadFile] = React.useState('');
  const File = React.useRef<File>();
  const { publisherItemId } = routeInfoStore.useStore((s) => s.params);
  const { uploadOfflinePackage } = publisherStore.effects;
  const [isLoading] = useLoading(publisherStore, ['uploadOfflinePackage']);

  const handleCancel = () => {
    setUploadFile('');
    File.current = undefined;
    onCancel();
  };

  const handleOk = async (data: { desc: string }) => {
    const formData: FormData = new FormData();
    formData.append('file', File.current as File);
    formData.append('desc', data.desc);
    const res = await uploadOfflinePackage({
      publishItemId: publisherItemId,
      data: formData,
    });
    handleCancel();
    afterUpload(res);
  };
  const fieldsList: IFormItem[] = [
    {
      label: i18n.t('file'),
      name: 'file',
      required: true,
      rules: [{ required: true, message: i18n.t('common:Please select the file to be uploaded') }],
      getComp: ({ form }: { form: FormInstance }) => {
        const uploadProps = getUploadProps({
          beforeUpload: (file: any) => {
            // 和后端保持一致
            const UPLOAD_SIZE_LIMIT = 300; // M
            const isLtSize = file.size / 1024 / 1024 < UPLOAD_SIZE_LIMIT;
            if (!isLtSize) {
              message.warning(i18n.t('common:the uploaded file must not exceed {size}M', { size: UPLOAD_SIZE_LIMIT }));
              return false;
            }
            form.setFieldsValue({ file });
            File.current = file;
            setUploadFile(file.name);
            // 阻止默认上传
            return false;
          },
        });
        return (
          <div className="upload-container">
            <Upload accept=".apk, .ipa" {...uploadProps}>
              <Button className="flex items-center">
                <ErdaIcon className="mr-1" type="upload" size="14" /> {i18n.t('upload')}
              </Button>
            </Upload>
            <span className="text-desc ml-2">{uploadFile ? i18n.t('selected {xx}', { xx: uploadFile }) : null}</span>
          </div>
        );
      },
    },
    // 暂时隐藏描述
    // {
    //   label: i18n.t('description'),
    //   name: 'desc',
    //   required: false,
    //   type: 'textArea',
    //   itemProps: {
    //     placeholder: i18n.t('please enter'),
    //     autoComplete: 'off',
    //     maxLength: 200,
    //   },
    // }
  ];
  return (
    <FormModal
      loading={isLoading}
      tip={i18n.t('dop:uploading, please do not leave')}
      title={i18n.t('upload offline package')}
      visible={visible}
      onCancel={handleCancel}
      fieldsList={fieldsList}
      onOk={handleOk}
    />
  );
};

export default UploadModal;
