import React from 'react';
import { LockOutlined, MobileOutlined, UserOutlined, LoadingOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Button, Input, message, Modal, Select, } from 'antd';
import FormMethod from './account/formWrapper';
import { getAccoutLoginSms, getAccoutLogin, getAccoutLoginTo, getLoginOption, getUserLang } from './api';
import localeHelper from '../../utils/localeHelper';
import properties from 'appRoot/properties';
import "./index.less";
// require('./index.less');
const FormItem = Form.Item;
const Option = Select.Option;


class FormWrapper extends React.Component {
    render() {
        let SpecialForm = Form.create()(CustomFormComponent);
        return <SpecialForm
            onSubmit={this.props.onSubmit}
            loading={this.props.loading}
            submitLoading={this.props.submitLoading}
            loginOption={this.props.loginOption}
            layout={this.props.layout} />;
    }
}

class CustomFormComponent extends React.Component {
    isNeedsmsCode = true;
    inputLoading = false;
    loginOption;
    aliyunId;
    errorInfo;
    password;
    smsCode;
    currentAliyunId;
    currentPassWord;
    currentSmsCode;
    passwordValue;
    langValue = [];
    constructor(props) {
        super(props);
        this.state = {
            isgetCode: false,
            count: 60,
            loading: false,
            upDataShow: false,
            inputLoading: false,
            inputloading: false

        };
    }
    componentWillMount() {
        getUserLang().then((res) => {
            if (res.status === 200) {
                res.info.langs.map((item) => {
                    console.log(item);
                    if (item === 'zh_CN' || item === 'zh_MO') {
                        this.langValue.push({
                            value: item,
                            label: item
                        });
                        console.log();
                    }
                });
                this.setState({ loading: false });
            }
        });
    }

    onSubmit(e) {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
                // 保存当前输入框里面的值
                this.currentAliyunId = this.aliyunId;
                this.currentPassWord = this.password;
                this.currentSmsCode = this.smsCode;
                console.log('base component Received values of form: ', values);
                this.setState({ inputloading: true });
                if (properties.envFlag === properties.ENV.ApsaraStack || properties.envFlag === properties.ENV.DXZ || properties.envFlag === properties.ENV.RQY) {
                    getAccoutLoginTo(values.aliyunId, values.password, values.lang, values.smsCode).then((res) => {
                        window.location.reload();
                    })
                        .catch(res => {
                            this.setState({ inputloading: false });
                        })
                } else if (properties.envFlag === properties.ENV.OXS || properties.envFlag === properties.ENV.Standalone || properties.envFlag === properties.ENV.PaaS) {
                    getAccoutLogin(values.aliyunId, values.password).then((res) => {
                        window.location.reload();
                    })
                        .catch(res => {
                            this.setState({ inputloading: false });
                        })
                }
            }
        });
    }

    render() {
        let stateCount = 'time秒后重发'.replace('time', this.state.count);
        if (this.password !== this.currentPassWord) {
            if (this.errorInfo) {
                this.errorInfo.password = null;
            }
        }
        if (this.aliyunId !== this.currentAliyunId) {
            if (this.errorInfo) {
                this.errorInfo.aliyunId = null;
            }
        }
        if (this.smsCode !== this.currentSmsCode) {
            if (this.errorInfo) {
                this.errorInfo.smsCode = null;
            }
        }
        const formItemLayout = this.props.layout ? this.props.layout : {
            labelCol: { span: 4 },
            wrapperCol: { span: 8 },
        };
        const { getFieldDecorator } = this.props.form;
        let buttonAfter;
        if (this.state.isgetCode) {
            buttonAfter = <span>{stateCount}</span>;
        } else {
            buttonAfter = <a onClick={this.getCode.bind(this)}>获取验证码</a>;
        }
        return (
            <div>
                <Form onSubmit={this.onSubmit.bind(this)}>
                    <FormItem
                        style={{ marginButton: 0 }}
                        {...formItemLayout}
                        validateStatus={this.errorInfo && this.errorInfo.aliyunId ? 'error' : 'success'}
                        help={this.errorInfo && this.errorInfo.aliyunId ? this.errorInfo.aliyunId : ''}
                    >
                        {getFieldDecorator('aliyunId', {
                        })(
                            <Input size="small" placeholder={localeHelper.get('LoginFormAccountPlaceholder', '账号')} onBlur={this.onblur.bind(this)} onChange={this.changeValue.bind(this)} prefix={<UserOutlined style={{ top: "4px", position: "absolute" }} />} />
                        )}
                    </FormItem>
                    <FormItem
                        {...formItemLayout}
                        validateStatus={this.errorInfo && this.errorInfo.password ? 'error' : 'success'}
                        help={this.errorInfo && this.errorInfo.password ? this.errorInfo.password : ''}
                    >
                        {getFieldDecorator('password', {
                        })(
                            <Input size="small" type="password" placeholder={localeHelper.get('LoginFormPassswordPlaceholder', '密码')} onChange={this.pswValue.bind(this)} prefix={<LockOutlined style={{ top: "4px", position: "absolute" }} />} />
                        )}
                    </FormItem>
                    {/* <FormItem
                        {...formItemLayout}
                    >
                        {getFieldDecorator('lang', {
                            initialValue: 'zh_CN'
                        })(
                            <Select placeholder={localeHelper.get('LogincheckoutLanguage', '选择语言')} allowClear={true} onChange={this.langValueOnchange.bind(this)}>
                                {this.langValue ? this.langValue.map((res) => {
                                    return <Option key={res.value} value={res.value}>{res.label}</Option>;
                                }) : <span></span>}
                            </Select>
                        )}
                    </FormItem> */}
                    <FormItem
                        style={{ display: this.loginOption === 'password_mobile' ? 'block' : 'none' }}
                        {...formItemLayout}
                        validateStatus={this.errorInfo && this.errorInfo.smsCode ? 'error' : 'success'}
                        help={this.errorInfo && this.errorInfo.smsCode ? this.errorInfo.smsCode : ''}
                    >
                        {getFieldDecorator('smsCode', {
                        })(
                            <Input placeholder={localeHelper.get('LoginFormCodePlaceholder', '手机验证码')} size="small" addonAfter={buttonAfter} onChange={this.codeValue.bind(this)} prefix={<MobileOutlined />} />
                        )}
                    </FormItem>
                    <FormItem  {...formItemLayout}>
                        <Button type="primary" htmlType="submit" block style={{
                            width: "100%", marginLeft: 0, marginTop: 10,
                            background: "#252525", border: "none"
                        }}>
                            {this.state.inputloading && <LoadingOutlined />}{localeHelper.get('LoginFormSubmit', '登录')}</Button>
                    </FormItem>
                </Form>
                <Modal
                    visible={this.state.upDataShow}
                    title={localeHelper.get('LoginFirstUpdataPwdModal', '首次登录密码修改')}
                    onCancel={this.onCancel.bind(this)}
                    footer={null}
                >
                    <FormMethod
                        type="upDataPassword"
                        aliyunId={this.currentAliyunId}
                        onSubmit={this.onInputSubmit.bind(this)}
                        loading={this.inputLoading}
                        layout={{
                            labelCol: { span: 4, offset: 4 },
                            wrapperCol: { span: 12 },
                        }}
                    />
                </Modal>
            </div>
        );
    }
    langValueOnchange(value) {
        console.log(value);
    }
    getDataOpion(aliyunId) {
        getLoginOption(aliyunId).then((res) => {
            this.loginOption = res.info.validation;
            this.setState({ loading: false });
        });
    }
    onblur(e) {
        this.getDataOpion(e.target.value);
    }
    onCancel() {// 首次登录取消
        this.setState({ upDataShow: false });
    }
    onInputSubmit(values) {// 首次登录确认按钮的时候
        message.success(localeHelper.get('LoginFirstUpdataPwdSuccess', '修改成功'));
        this.setState({
            upDataShow: false,
            inputLoading: false
        });
        window.location.href = '/';
    }
    changeValue(e) {
        this.aliyunId = e.target.value;
    }
    pswValue(e) {
        this.password = e.target.value;
    }
    codeValue(e) {
        this.smsCode = e.target.value;
    }
    getCode() {// 获取手机验证码
        getAccoutLoginSms(this.aliyunId, this.password).then((res) => {
            if (res.status === 200) {
                this.setState({ isgetCode: true });
                let that = this;
                let timer = setInterval(function () {
                    var count = that.state.count;
                    count -= 1;
                    if (count < 1) {
                        that.setState({
                            isgetCode: false
                        });
                        count = 60;
                        clearInterval(timer);
                    }
                    that.setState({
                        count: count
                    });
                }.bind(this), 1000);
                message.success(localeHelper.get('LoginGetCodeSuccess', '已经向手机号为') + res.info.phone + localeHelper.get('LoginGetCodeReview', '发送验证码，请注意查收短信'), 6);
            } else {
                if (res.status === 422) {
                    this.errorInfo = res.info;
                    this.currentAliyunId = this.aliyunId;
                    this.currentPassWord = this.password;
                    this.setState({ loading: false });
                } else {
                    message.error(res.message);
                }
            }
        }).catch(() => {
            message.error(localeHelper.get('LoginFormSubmitError', '服务器异常'));
        });
    }
}

export default FormWrapper;
