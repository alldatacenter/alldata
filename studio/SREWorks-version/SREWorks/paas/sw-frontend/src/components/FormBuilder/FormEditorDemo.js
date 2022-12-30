/**
 * Created by caoshuaibiao on 2021/1/12.
 * 利用表单定义器定义表单及利用定义生成表单示例
 */
import React, { PureComponent } from 'react';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import {
    message,
    Button,
    Card,
    Tooltip,
    Popover,
    Modal,
    Row,
    Col,
    Radio,
    Spin,
    Collapse,
    Select,
    Divider,
} from 'antd';
import FormEditor from './FormEditor';
import SuperForm from './SuperForm';



export default class FormEditorDemo extends PureComponent {

    constructor(props) {
        super(props);
        this.state={parameterDefiner:null,reloadCount:0};
    }

    componentDidMount() {
    }

    getParameterDefiner=()=>{
        this.setState({
            parameterDefiner:this.editor.getParameterDefiner(),
            reloadCount:++this.state.reloadCount
        });
    };

    getGenParameterDefiner=()=>{
        console.log("表单的定义对象为----->",this.superForm.getParameterDefiner());
    };

    getGenFormValues=()=>{
        console.log("表单原生值----->",this.superForm.getFieldsValue());
    };

    render() {
        let {parameterDefiner,reloadCount}=this.state;
        return (
            <Card size="small" bordered={false}>
                <Divider>定义表单</Divider>
                <FormEditor onReady={(editor)=>this.editor=editor} />
                <Divider>生成表单</Divider>
                <div>
                    <Button type="primary" onClick={this.getParameterDefiner}>生成表单</Button>
                    <Button type="primary" style={{margin:"0px 16px"}} onClick={this.getGenParameterDefiner}>获取表单定义对象</Button>
                    <Button type="primary" onClick={this.getGenFormValues}>获取表单原生值</Button>
                </div>
                {
                    parameterDefiner&&
                        <SuperForm parameterDefiner={parameterDefiner}
                                   key={reloadCount}
                                   ref={r => this.superForm = r}
                                   formItemLayout={
                                       {
                                           labelCol: {
                                               xs: { span: 24 },
                                               sm: { span: 24 },
                                               md: { span: 6 },
                                           },
                                           wrapperCol: {
                                               xs: { span: 24 },
                                               sm: { span: 24 },
                                               md: { span: 12 },
                                           },
                                       }
                                   }
                        />
                }
            </Card>
        );
    }
}