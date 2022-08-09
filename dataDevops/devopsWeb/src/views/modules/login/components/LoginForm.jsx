import React from 'react';
import { Link } from 'react-router';
import PropTypes from 'prop-types';

import { Form, Icon, Input, Button } from 'antd';
import InterfaceFn from 'js/interface.js';
import UtilFn from 'js/util.js';
import Variable from 'js/variable.js';
const FormItem = Form.Item;

class LoginForm extends React.Component {
    constructor(props) {
        super(props);
    }

    handleSubmit = (e) => {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
                InterfaceFn.login(values, (resData) => {
                    // UtilFn.setCookie('sessiontoken', resData.sessiontoken);
                    // Variable.isLogin = true;

                    this.props.succCallback(values['username'], resData);
                });
            }
        });
    }

    render() {
        const { getFieldDecorator } = this.props.form;
        return (
            <Form onSubmit={this.handleSubmit} style={{maxWidth: '300px'}}>
                <FormItem>
                    {getFieldDecorator('username', {
                        rules: [{ required: true, message: '请输入你的用户名!' }],
                    })(
                        <Input prefix={<Icon type="user" style={{ fontSize: 13 }} />} placeholder="用户名" />
                    )}
                </FormItem>
                <FormItem>
                    {getFieldDecorator('password', {
                        rules: [{ required: true, message: '请输入你的密码!' }],
                    })(
                        <Input prefix={<Icon type="lock" style={{ fontSize: 13 }} />} type="password" placeholder="密码" />
                    )}
                </FormItem>
                <FormItem>
                    <Button type="primary" htmlType="submit" className="login-form-button" style={{width: '100%'}}>
                        Log in
                    </Button>
                </FormItem>
            </Form>
        );
    }
}

LoginForm = Form.create()(LoginForm);

LoginForm.propTypes = {
    succCallback: PropTypes.func.isRequired
};

export default LoginForm;