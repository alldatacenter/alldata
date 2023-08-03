import React, { useState } from 'react';
import './index.less';
import {
    Form, Input,
} from 'antd';
import { UserOutlined, LockOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useIntl } from 'react-intl';
import shareData from 'src/utils/shareData';
import { DV_STORAGE_LOGIN } from 'src/utils/constants';
import { useHistory } from 'react-router-dom';
import { SwitchLanguage } from '@/component';
import { $http } from '@/http';
import { useCommonActions } from '@/store';

type TLoginValues = {
    username: string,
    password: string,
}

const Login = () => {
    const { setIsDetailPage } = useCommonActions();
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();
    const intl = useIntl();
    const history = useHistory();
    const onFinish = async () => {
        form.validateFields().then(async (values:TLoginValues) => {
            try {
                setLoading(true);
                const res = await $http.post('/login', values, { showWholeData: true });
                shareData.sessionSet(DV_STORAGE_LOGIN, {
                    ...(res.data),
                    token: res.token,
                });
                setIsDetailPage(false);
                history.push('/main/home');
            } catch (error: any) {
            } finally {
                setLoading(false);
            }
        }).catch(() => {

        });
    };
    return (
        <div className="dv-login">
            <div className="dv-login__switch-language"><SwitchLanguage /></div>
            <div className="dv-login-containner">
                <div className="dv-login-wrap">
                    <div className="dv-login-title main-color">Datavines</div>
                    <Form
                        form={form}
                        layout="vertical"
                        name="dv-login"
                    >
                        <Form.Item
                            name="username"
                            style={{ marginBottom: 15 }}
                            rules={[{ required: true, message: intl.formatMessage({ id: 'login_username_msg' }) }]}
                        >
                            <Input autoComplete="off" style={{ height: 50 }} size="large" prefix={<UserOutlined />} />
                        </Form.Item>

                        <Form.Item
                            name="password"
                            style={{ marginBottom: 15 }}
                            rules={[{ required: true, message: intl.formatMessage({ id: 'login_password_msg' }) }]}
                        >
                            <Input.Password style={{ height: 50 }} size="large" prefix={<LockOutlined />} />
                        </Form.Item>
                        <p className="dv-login-btn">

                            <a href="#/register" className="dv-register-btn">
                                {intl.formatMessage({ id: 'register' })}
                            </a>
                            <span onClick={() => onFinish()}>
                                {intl.formatMessage({ id: 'login_btn_text' })}
                                <ArrowRightOutlined />
                            </span>
                        </p>
                    </Form>
                </div>
            </div>
        </div>
    );
};
export default Login;
