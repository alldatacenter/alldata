import { Form, FormProps, Modal, ModalProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CommonFormTypes } from 'globalConstants';
import { forwardRef, ReactNode, useCallback, useImperativeHandle } from 'react';

export interface ModalFormProps extends ModalProps {
  type?: CommonFormTypes;
  formProps?: FormProps;
  onSave: (values) => void;
  children?: ReactNode;
}

export const ModalForm = forwardRef(
  (
    { type, formProps, onSave, afterClose, children, ...rest }: ModalFormProps,
    ref,
  ) => {
    const [form] = Form.useForm();
    const tg = useI18NPrefix('global');
    useImperativeHandle(ref, () => form);

    const onOk = useCallback(() => {
      form.submit();
    }, [form]);

    const onAfterClose = useCallback(() => {
      form.resetFields();
      afterClose && afterClose();
    }, [form, afterClose]);

    return (
      <Modal
        {...rest}
        title={
          type === CommonFormTypes.SaveAs
            ? tg('button.saveAs')
            : `${type ? tg(`modal.title.${type}`) : ''}${rest.title}`
        }
        onOk={onOk}
        afterClose={onAfterClose}
      >
        <Form form={form} onFinish={onSave} {...formProps}>
          {children}
        </Form>
      </Modal>
    );
  },
);
