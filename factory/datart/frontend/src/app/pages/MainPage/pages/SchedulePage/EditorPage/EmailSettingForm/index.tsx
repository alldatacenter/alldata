import { DownCircleOutlined, UpCircleOutlined } from '@ant-design/icons';
import { Checkbox, Col, Form, Input, InputNumber, Row } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, useMemo, useState } from 'react';
import { FileTypes, FILE_TYPE_OPTIONS } from '../../constants';
import { CommonRichText } from './CommonRichText';
import { MailTagFormItem } from './MailTagFormItem';

interface EmailSettingFormProps {
  fileType?: FileTypes[];
  onFileTypeChange: (v: FileTypes[]) => void;
}
export const EmailSettingForm: FC<EmailSettingFormProps> = ({
  fileType,
  onFileTypeChange,
}) => {
  const [showBcc, setShowBcc] = useState(false);
  const t = useI18NPrefix('schedule.editor.emailSettingForm.index');
  const hasImageWidth = useMemo(() => {
    return fileType && fileType?.length > 0
      ? fileType?.includes(FileTypes.Image)
      : false;
  }, [fileType]);
  const ccLabel = useMemo(() => {
    return (
      <>
        {t('CC') + ' '}
        <span onClick={() => setShowBcc(!showBcc)}>
          {showBcc ? <UpCircleOutlined /> : <DownCircleOutlined />}
        </span>
      </>
    );
  }, [showBcc, t]);

  return (
    <>
      <Form.Item
        label={t('theme')}
        name="subject"
        rules={[{ required: true, message: t('subjectIsRequired') }]}
      >
        <Input />
      </Form.Item>
      <Row>
        <Col span={15}>
          <Form.Item
            labelCol={{ span: 8 }}
            label={t('fileType')}
            name="type"
            rules={[{ required: true }]}
          >
            <Checkbox.Group
              options={FILE_TYPE_OPTIONS}
              onChange={v => onFileTypeChange(v as FileTypes[])}
            />
          </Form.Item>
        </Col>
        <Col span={9}>
          {hasImageWidth ? (
            <div className="image_width_form_item_wrapper">
              <Form.Item
                label={t('picWidth')}
                labelCol={{ span: 10 }}
                name="imageWidth"
                rules={[{ required: true }]}
              >
                <InputNumber min={100} />
              </Form.Item>
              <span className="image_width_unit">{t('px')}</span>
            </div>
          ) : null}
        </Col>
      </Row>
      <Form.Item
        label={t('recipient')}
        name="to"
        rules={[{ required: true, message: t('recipientIsRequired') }]}
      >
        <MailTagFormItem />
      </Form.Item>
      <Form.Item label={ccLabel} name="cc">
        <MailTagFormItem />
      </Form.Item>
      {showBcc ? (
        <Form.Item label={t('bcc')} name="bcc">
          <MailTagFormItem />
        </Form.Item>
      ) : null}
      <Form.Item label={t('contentOfEmail')} validateFirst name="textContent">
        <CommonRichText placeholder="This email comes from cron job on the datart." />
      </Form.Item>
    </>
  );
};
