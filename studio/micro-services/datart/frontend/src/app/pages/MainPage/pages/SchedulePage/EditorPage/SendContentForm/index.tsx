import { Form } from 'antd';
import { FC } from 'react';
import { VizFolderTree } from './VizFolderTree';

interface SendContentFormProps {}
export const SendContentForm: FC<SendContentFormProps> = () => {
  return (
    <Form.Item noStyle name="folderContent">
      <VizFolderTree />
    </Form.Item>
  );
};
