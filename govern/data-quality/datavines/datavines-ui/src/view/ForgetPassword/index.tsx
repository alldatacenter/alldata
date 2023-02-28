/* eslint-disable no-useless-escape */
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import {
    Input, Button, Form, Modal,
} from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import { useHistory } from 'react-router-dom';
import { FormRender, IFormRender } from '@/common';
import { useVerificationCode } from '@/hooks';
import { PWD_REG, EMAIL_REG, CODE_REG } from '@/utils/constants';
import { $http } from '@/http';
import './index.less';

const Index = () => {
    const intl = useIntl();
    const [loading, setLoading] = useState(false);
    const history = useHistory();
    const { RenderImage, verificationCodeJwt } = useVerificationCode();
    const [form] = Form.useForm();
    const inputTip = intl.formatMessage({ id: 'common_input_tip' });
    const requiredTop = intl.formatMessage({ id: 'common_required_top' });
    const oldPasswordText = intl.formatMessage({ id: 'old_password_text' });
    const newPasswordText = intl.formatMessage({ id: 'new_password_text' });
    const emailText = intl.formatMessage({ id: 'email_text' });
    const userNameText = intl.formatMessage({ id: 'userName_text' });
    const verificationCodeText = intl.formatMessage({ id: 'verification_code_text' });
    const patternTip = intl.formatMessage({ id: 'common_input_pattern_tip' });
    const phoneText = intl.formatMessage({ id: 'phone_text' });

    const schema: IFormRender = {
        name: 'userFormRender',
        layout: 'vertical',
        formItemProps: {
            style: { marginBottom: 10 },
        },
        meta: [
            {
                label: emailText,
                name: 'email',
                rules: [
                    { required: true, message: `${emailText}${requiredTop}` },
                    { pattern: EMAIL_REG, message: `${patternTip}${emailText}` },
                ],
                widget: <Input placeholder={`${inputTip}${emailText}`} />,
            },
            {
                label: oldPasswordText,
                name: 'oldpassword',
                rules: [
                    { required: true, message: `${oldPasswordText}${requiredTop}` },
                    { pattern: PWD_REG, message: `${patternTip}${oldPasswordText}` },
                ],
                widget: <Input.Password placeholder={`${inputTip}${oldPasswordText}`} />,
            },
            {
                label: newPasswordText,
                name: 'password',
                rules: [
                    { required: true, message: `${newPasswordText}${requiredTop}` },
                    { pattern: PWD_REG, message: `${patternTip}${newPasswordText}` },
                ],
                widget: <Input.Password placeholder={`${inputTip}${newPasswordText}`} />,
            },
            {
                label: userNameText,
                name: 'username',
                rules: [
                    { required: true, message: `${userNameText}${requiredTop}` },
                    { pattern: /^[\u4E00-\u9FA5_a-zA-Z0-9]{2,32}$/, message: `${patternTip}${userNameText}` },
                ],
                widget: <Input placeholder={`${inputTip}${userNameText}`} />,
            },
            {
                label: phoneText,
                name: 'phone',
                rules: [
                    { required: true, message: `${phoneText}${requiredTop}` },
                ],
                widget: <Input placeholder={`${inputTip}${emailText}`} />,
            },
            {
                label: `${verificationCodeText}`,
                name: 'verificationCode',
                rules: [
                    { required: true, message: `${verificationCodeText}${requiredTop}` },
                    { pattern: CODE_REG, message: `${patternTip}${verificationCodeText}` },
                ],
                widget: <Input
                    addonAfter={(
                        <RenderImage
                            style={{
                                display: 'inline-block',
                                height: '30px',
                                margin: '0 -11px',
                            }}
                        />
                    )}
                />,
            },
        ],
    };
    const onForgetPwd = async (values: any) => {
        try {
            setLoading(true);
            const res = await $http.post('/user/resetPassword', {
                ...values,
                verificationCodeJwt,
            });
            if (res?.result) {
                Modal.confirm({
                    icon: <CheckOutlined style={{ color: '#6bab5e' }} />,
                    content: intl.formatMessage({ id: 'forget_success' }),
                    onOk() {
                        Modal.destroyAll();
                        history.push('/login');
                    },
                    onCancel() {
                        window.location.reload();
                    },
                });
            }
        } catch (error: any) {
        } finally {
            setLoading(false);
        }
    };

    const btnClick = async () => {
        try {
            const values = await form.validateFields();
            onForgetPwd(values);
        } catch (errorInfo) {
        }
    };
    return (
        <div className="dv-gray-bg">
            <div className="dv-forget-title dv-margin-auto">
                {intl.formatMessage({ id: 'forget_pwd_title' })}
            </div>
            <div className="dv-margin-auto dv-forget-container">
                <FormRender {...schema} form={form} />
            </div>
            <div className="dv-margin-auto dv-forget-container" style={{ paddingTop: 20 }}>
                <Button
                    loading={loading}
                    onClick={btnClick}
                    style={{ width: '100%', marginBottom: 20 }}
                    size="large"
                    type="primary"
                >
                    {intl.formatMessage({ id: 'forget_pwd_btn' })}
                </Button>
            </div>
        </div>
    );
};

export default Index;
