import React from 'react';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Button, Input, Tooltip, message } from 'antd';
import { cookie } from '../../../utils/cookie';
import localeHelper from '../../../utils/localeHelper';
import { accountInfoChange, getAccountCreate, upDataPassword, SmsRegister } from '../api';
import createBrowserHistory from 'history/createBrowserHistory';

const FormItem = Form.Item;
const InputGroup = Input.Group;

let history = createBrowserHistory({ forceRefresh: true });


class FormWrapper extends React.Component {
    render() {
        let SpecialForm = Form.create()(CustomFormComponent);
        return <SpecialForm
            onSubmit={this.props.onSubmit}
            submitLoading={this.props.submitLoading}
            loading={this.props.loading}
            type={this.props.type}
            endpoint={this.props.endpoint}
            token={this.props.token}
            aliyunId={this.props.aliyunId}
            currentPhone={this.props.currentPhone}
            layout={this.props.layout} />;
    }
}

class CustomFormComponent extends React.Component {
    accountInfoError;
    phoneValue;
    phonePrefix;
    firstphoneValue;
    currentFirstphoneValue;
    currentPhoneValue;
    creatInfoError;
    upDataPasswordInfoError;
    passwordValue;
    currentPassword;
    aliyunIdValue;
    currentAliyunId;
    upDataPasswordValue;
    currentUpdataPassword;
    endpoint;
    token;
    constructor(props) {
        super(props);
        this.state = {
            confirmDirty: false,
            loading: false,
            inputloading: false
        };
        this.phonePrefix = this.getTelPrefix();
    }

    getTelPrefix = () => {
        //const locale = cookie.read('aliyun_country') || 'CN';
        //return `+${metadata.countries[locale][0]}`;
        return "+86";
    }

    onSubmit(e) {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
                if (!values.phone) {
                    values.phone = values.firstphone + this.phoneValue;
                }
                if (this.props.aliyunId && this.props.type === 'accountPhone') {
                    this.setState({ inputloading: true });
                    var phone = values.phone ? values.phone : '';
                    accountInfoChange(this.props.aliyunId, values.phone).then((res) => {
                        if (res.status === 200) {
                            this.props.onSubmit(values);
                        } else if (res.status === 422) {
                            this.accountInfoError = res.info;
                            this.currentPhoneValue = this.phoneValue;
                            this.setState({ loading: false });
                        } else if (res.status === 401) {
                            history.push('/login', history.location.pathname);
                        } else {
                            message.error(res.message, 3);
                        }
                        this.setState({ inputloading: false });
                    }).catch(() => {
                        message.error('error');
                        this.setState({ inputloading: false });
                    });
                    return;
                } else if (this.props.type === 'creatAccount') {
                    this.setState({ inputloading: true });
                    var phone = values.phone !== undefined ? values.phone : '';
                    getAccountCreate(values.aliyunId, values.password, phone, false).then((res) => {
                        if (res.status == 200) {
                            this.props.onSubmit(res);
                        } else if (res.status === 422) {
                            this.creatInfoError = res.info;
                            this.currentAliyunId = values.aliyunId;
                            this.currentPassword = values.password;
                            this.currentFirstphoneValue = values.firstphone;
                            this.currentPhoneValue = this.phoneValue;
                            if (res.info.nonExistField) {
                                message.error(res.info.nonExistField);
                            }
                            this.setState({ loading: false });
                        } else if (res.status === 401) {
                            history.push('/login', history.location.pathname);
                        } else {
                            message.error(res.message, 3);
                        }
                        this.setState({ inputloading: false });
                    }).catch(() => {
                        message.error('error');
                        this.setState({ inputloading: false });
                    });
                    return;
                } else if (this.props.aliyunId && this.props.type === 'upDataPassword') {
                    this.setState({ inputloading: true });
                    upDataPassword(this.props.aliyunId, values.password).then((res) => {
                        if (res.status === 200) {
                            this.props.onSubmit(res);
                        } else if (res.status === 401) {
                            history.push('/login', history.location.pathname);
                        } else if (res.status === 422) {
                            this.upDataPasswordInfoError = res.info;
                            this.currentUpdataPassword = values.password;
                            if (res.info.nonExistField) {
                                message.error(res.info.nonExistField);
                            }
                        } else {
                            message.error(res.message, 3);
                        }
                        this.setState({ inputloading: false });
                    }).catch(() => {
                        this.setState({ inputloading: false });
                        message.error('error');
                    });
                    return;
                } else if (this.props.type === 'register') {
                    this.setState({ inputloading: true });
                    SmsRegister(values.endpoint, values.token).then((res) => {
                        if (res.status === 200) {
                            this.props.onSubmit(values);
                        } else if (res.status === 401) {
                            history.push('/login', history.location.pathname);
                        } else if (res.status === 422) {
                            this.endpoint = res.info.endpoint;
                            this.token = res.info.token;
                            this.setState({ loading: false });
                        } else {
                            message.error(res.message, 3);
                        }
                        this.setState({ inputloading: false });
                    }).catch(() => {
                        this.setState({ inputloading: false });
                        message.error('error');
                    });
                    return;
                } else {
                    this.props.onSubmit(values);
                }
            }
        });
    }
    render() {
        if (this.phoneValue !== this.currentPhoneValue) {
            if (this.accountInfoError) {
                this.accountInfoError.phone = null;
            }
            if (this.creatInfoError) {
                this.creatInfoError.phone = null;
            }
        }
        if (this.firstphoneValue && this.firstphoneValue !== this.currentFirstphoneValue) {
            if (this.creatInfoError) {
                this.creatInfoError.phone = null;
            }
        }
        if (this.aliyunIdValue !== this.currentAliyunId) {
            if (this.creatInfoError) {
                this.creatInfoError.aliyunId = null;
            }
        }
        if (this.passwordValue !== this.currentPassword) {
            if (this.creatInfoError) {
                this.creatInfoError.password = null;
            }
        }
        if (this.upDataPasswordValue !== this.currentUpdataPassword) {
            if (this.upDataPasswordInfoError) {
                this.upDataPasswordInfoError.password = null;
            }
        }
        let formItem;
        let btnName;
        const formItemLayout = this.props.layout ? this.props.layout : {
            labelCol: { span: 4 },
            wrapperCol: { span: 9 },
        };
        // const formSwitch = {
        //     labelCol: { span: 10, offset: 5 },
        //     wrapperCol: { span: 7 },
        // };
        const { getFieldDecorator } = this.props.form;
        switch (this.props.type) {
            case 'upDataPassword':
                btnName = localeHelper.get('ButtonOK', '确认');
                formItem = <div>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerFormNewPasswordLabel', '新密码')}
                        validateStatus={this.upDataPasswordInfoError && this.upDataPasswordInfoError.password ? 'error' : 'success'}
                        help={this.upDataPasswordInfoError && this.upDataPasswordInfoError.password ? this.upDataPasswordInfoError.password : null}>
                        {getFieldDecorator('password', {
                        })(
                            <Input type="password" placeholder={localeHelper.get('AccountManagerClouderManagerFormPasswordPlaceholder', '大小写字母、数字、特殊字符，至少三种且长度10到20位')} onChange={this.updataPasswordChange.bind(this)} />
                        )}
                    </FormItem>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerFormConfirmPassword', '确认密码')}
                        hasFeedback>
                        {getFieldDecorator('confirm', {
                            rules: [{
                                required: true, message: localeHelper.get('AccountManagerClouderManagerFormConfirmPasswordTip', '请确认密码')
                            }, {
                                validator: this.checkPassword,
                            }],
                        })(
                            <Input type="password" onBlur={this.handleConfirmBlur} />
                        )}
                    </FormItem>
                </div>;
                break;
            case 'creatAccount':
                btnName = localeHelper.get('create', '创建');
                formItem = (<div>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerFormCreateAccountLabel', '云账号')}
                        validateStatus={this.creatInfoError && this.creatInfoError.aliyunId ? 'error' : 'success'}
                        help={this.creatInfoError && this.creatInfoError.aliyunId ? this.creatInfoError.aliyunId : null}
                    >
                        {getFieldDecorator('aliyunId', {
                        })(
                            <Input onChange={this.aliyunIdChange.bind(this)} />
                        )}
                    </FormItem>
                    <FormItem
                        {...formItemLayout}
                        label={(
                            <span>
                                {localeHelper.get('AccountManagerClouderManagerPassword', '密码')}
                                <Tooltip title={localeHelper.get('AccountManagerClouderManagerFormPasswordLabelTip', '密码设置不能只有字母、数字、特殊符号')}>
                                    <QuestionCircleOutlined />
                                </Tooltip>
                            </span>
                        )}
                        validateStatus={this.creatInfoError && this.creatInfoError.password ? 'error' : 'success'}
                        help={this.creatInfoError && this.creatInfoError.password ? this.creatInfoError.password : null}
                    >
                        {getFieldDecorator('password', {
                        })(
                            <Input type="password" placeholder={localeHelper.get('AccountManagerClouderManagerFormPasswordPlaceholder', '大小写字母、数字、特殊字符，至少三种且长度10到20位')} onChange={this.passwordChange.bind(this)} />
                        )}
                    </FormItem>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerFormMobileLabel', '手机号码')}
                        validateStatus={this.creatInfoError && this.creatInfoError.phone ? 'error' : 'success'}
                        help={this.creatInfoError && this.creatInfoError.phone ? this.creatInfoError.phone : null}
                    >
                        {/* 输入手机号 */}
                        <InputGroup>
                            {getFieldDecorator('firstphone', {
                                initialValue: this.phonePrefix
                            })(
                                <Input style={{ width: '25%' }} onChange={this.firstphoneChange.bind(this)} />
                            )}
                            <Input style={{ width: '75%' }} onChange={this.phoneChange.bind(this)} />
                        </InputGroup>
                        {/* <PhoneInput
                            countrySelectComponent={'String'}
                            internationalIcon={InternationalIcon}
                            metadata={metadata}
                            // placeholder="Enter phone number"
                            value="10"
                            labels={defaultLocale}
                            onChange={(phone) => console.log(phone)}
                        /> */}
                    </FormItem>
                    {/* <FormItem
                        {...formSwitch}
                        label={<span>{localeHelper.get('AccountManagerClouderManagerFormSwitchMobileLabel')}</span>}
                    >
                        {getFieldDecorator('passwordChangeRequired', { valuePropName: 'checked', initialValue: true })(
                            <Switch />
                        )}
                    </FormItem> */}
                </div>);
                break;
            case 'register':
                btnName = this.props.endpoint && this.props.token ? localeHelper.get('Modify', '修改') : localeHelper.get('Register', '注册');
                formItem = (<div>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerAccessPoint', '接入点')}
                        validateStatus={this.endpoint ? 'error' : 'success'}
                        help={this.endpoint ? this.endpoint : null}
                    >
                        {getFieldDecorator('endpoint', {
                            initialValue: this.props.endpoint ? this.props.endpoint : null,
                        })(
                            <Input />
                        )}
                    </FormItem>
                    <FormItem
                        {...formItemLayout}
                        label="Token"
                        validateStatus={this.token ? 'error' : 'success'}
                        help={this.token ? this.token : null}
                    >
                        {getFieldDecorator('token', {
                            initialValue: this.props.token ? this.props.token : null,
                        })(
                            <Input />
                        )}
                    </FormItem>
                </div>);
                break;
            case 'accountPhone':
                btnName = localeHelper.get('ButtonOK', '确认');
                formItem = <div>
                    <FormItem
                        {...formItemLayout}
                        label={localeHelper.get('AccountManagerClouderManagerFormAccountPhone', '新手机号码')}
                        validateStatus={this.accountInfoError && this.accountInfoError.phone ? 'error' : 'success'}
                        help={this.accountInfoError ? this.accountInfoError.phone : null}
                    >
                        {getFieldDecorator('phone', {
                            initialValue: this.props.currentPhone
                        })(
                            <Input onChange={this.phoneChange.bind(this)} />
                        )}
                    </FormItem>
                </div>;
                break;
            default:
                break;
        }
        return (
            <div>
                <Form onSubmit={this.onSubmit.bind(this)}>
                    {formItem}
                    <FormItem wrapperCol={{ span: 10, offset: (formItemLayout.labelCol.offset || 0) + formItemLayout.labelCol.span }}>
                        <Button type="primary" loading={this.state.inputloading} htmlType="submit">{btnName}</Button>
                    </FormItem>
                </Form>
            </div>
        );
    }
    updataPasswordChange(e) {// 修改密码
        this.upDataPasswordValue = e.target.value;
    }
    firstphoneChange(e) {// 手机号码前缀
        this.firstphoneValue = e.target.value;
    }
    phoneChange(e) {
        this.phoneValue = e.target.value;
        this.setState({ loading: false });
    }
    aliyunIdChange(e) {
        this.aliyunIdValue = e.target.value;
    }
    passwordChange(e) {
        this.passwordValue = e.target.value;
    }
    checkPassword = (rule, value, callback) => {
        const form = this.props.form;
        if (value && value !== form.getFieldValue('password')) {
            callback(localeHelper.get('AccountManagerClouderManagerFormCheckPassword', '两次输入的密码不一致'));
        } else {
            callback();
        }
    }
    checkConfirm = (rule, value, callback) => {
        const form = this.props.form;
        if (value && this.state.confirmDirty) {
            form.validateFields(['confirm'], { force: false }, callback());
        }
        callback();
    }
    handleConfirmBlur = (e) => {
        const value = e.target.value;
        this.setState({ confirmDirty: this.state.confirmDirty || !!value });
    }
}

export default FormWrapper;
