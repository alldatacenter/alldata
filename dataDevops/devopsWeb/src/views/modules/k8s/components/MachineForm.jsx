import React from 'react';
import { Form, Select, Input, Button, Radio, DatePicker, InputNumber, Icon, Row, Col } from 'antd';
import moment from 'moment';
import InterfaceFn from 'js/interface.js';
const { Option } = Select;
const FormItem = Form.Item;
const RadioGroup = Radio.Group;
const { TextArea } = Input;

class MachineForm extends React.Component{
    constructor(props){
        super(props);

    }
    
    handleProFromSubmit = (e) => {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
                console.log(values);
                let username = localStorage.getItem('username');
                let reqData = {
                    inip: values.inIp,
                    outip: values.outIp,
                    name: values.name,
                    info: values.info,
                    account: values.account,
                    status: values.status,
                    k8sCluster: values.k8sCluster,
                    k8sName: values.k8sName,
                    operator: username,
                };
                InterfaceFn.addMachine(reqData, (resData) => {
                    this.props.toggleModalMachineForm();
                    this.props.getAllMachine();
                });
                
            }
        });
    }
    render(){
        const { getFieldDecorator} = this.props.form;
        return(
            <Form
                onSubmit = {this.handleProFromSubmit}
                className = "m_group_container"
            >
                <div className = "m_group">
                    <div className = "group_content"> 
                        <FormItem
                            label = {'机器名称'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'name' , {
                                rules: [{ required: true}],
                                initialValue: '',
                            })(
                                <Input placeholder = "请填写机器名称"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'机器组别'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator('account' , {
                                rules: [{ required: true}],
                                initialValue:  '',
                            })(
                                <Input placeholder = "请填写机器组别"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'机器信息'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'info' , {
                                rules: [{ required: true}],
                                initialValue: '',
                            })(
                                <Input placeholder = "请填写机器信息"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'状态'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'status' , {
                                rules: [{ required: true}],
                                initialValue: '',
                            })(
                                <Input placeholder = "请填写机器状态"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'InIp'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'inIp' , {
                                rules: [{ required: true}],
                            })(
                                <Input placeholder = "请填写InIp"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'OutIp'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'outIp' , {
                                rules: [{ required: true}],
                            })(
                                <Input placeholder = "请填写OutIp"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'环境'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'k8sCluster' , {
                                rules: [{ required: true}],
                            })(
                                <Input placeholder = "请填写环境"/>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'k8s名称'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'k8sName' , {
                                rules: [{ required: true}],
                            })(
                                <Input placeholder = "请填写k8s名称"/>
                            )}
                        </FormItem>
                    </div>
                </div>
                
                
                <div className = "w_button_group">
                    <Button type = "primary" onClick = {this.props.toggleModalMachineForm} style = {{ float: 'right', marginLeft: 20 }} >
                        取消
                    </Button>
                    <Button type = "primary" htmlType = "submit" style = {{ float: 'right' }} >
                        确定
                    </Button>
                </div>
            </Form>
        );
    }
}


MachineForm = Form.create()(MachineForm);
export default MachineForm ;