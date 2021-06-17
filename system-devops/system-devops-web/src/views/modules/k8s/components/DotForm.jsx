import React from 'react';
import { Form, Select, Input, Button, Radio, DatePicker, InputNumber, Icon, Row, Col } from 'antd';
import moment from 'moment';
import InterfaceFn from 'js/interface.js';
const { Option } = Select;
const FormItem = Form.Item;
const RadioGroup = Radio.Group;
const { TextArea } = Input;

class DotForm extends React.Component{
    constructor(props){
        super(props);
        this.state = {
            firstDotList: ['wulinhao', 'wulinhao','wulinhao'],
            secondDotList: ['lalala','dadadda','aaaa'],
        }
    }
    
    handleDotFromSubmit = (e) => {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
               
            }
        });
    }

    //修改机器污点
    handleChangeDot = () => {

    }
    
    render(){
        const { getFieldDecorator} = this.props.form;
        //当编辑的时候请了Dot的数据,需要渲染出来
        return(
            <Form
                onSubmit = {this.handleDotFromSubmit}
                className = "m_group_container"
            >
                <div className = "m_group">
                    <div className = "group_content"> 
                        <FormItem
                            label = {'机器污点1'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator( 'name' , {
                                rules: [{ required: true}],
                                // initialValue: this.props.data.name || '',
                            })(
                                <Select value={''} style={{ width: 120 }}>
                                    {
                                        this.state.firstDotList.map((item) => {
                                            return (
                                                <Option value={item}>{item}</Option>
                                            )
                                        })
                                    }
                                </Select>
                            )}
                        </FormItem>
                        <FormItem
                            label = {'机器污点2'}
                            labelCol = {{ span: 4 }}
                            wrapperCol = {{ span: 18 }}
                        >
                            {getFieldDecorator('department' , {
                                rules: [{ required: true}],
                                // initialValue: this.props.data.department || '',
                            })(
                                <Select value={''} style={{ width: 120 }}>
                                    {
                                        this.state.secondDotList.map((item) => {
                                            return (
                                                <Option value={item}>{item}</Option>
                                            )
                                        })
                                    }
                                </Select>
                            )}
                        </FormItem>
                    </div>
                </div>
                
                
                <div className = "w_button_group">
                    <Button type = "primary" onClick = {this.props.toggleModalDotForm} style = {{ float: 'right', marginLeft: 20 }} >
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


DotForm = Form.create()(DotForm);
export default DotForm ;