/**
 * Created by caoshuaibiao on 2021/1/11.
 * 高级表单,由FormEditor定义生成,支持表单参数来源api,json配置等
 */
import React from 'react';
import {
    Spin,
    Button,
    Card,
    Modal,
    Tooltip,
    Drawer,
    message,
    Alert,
    Popover,
    Input,
    Radio,
} from 'antd';
import SimpleForm from './SimpleForm';
import * as util from '../../utils/utils';

import ParameterDefiner from '../ParameterMappingBuilder/ParameterDefiner';

const defaultLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 4 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 20 },
    },
};

export default class SuperForm extends React.Component  {

    constructor(props) {
        super(props);
        let {parameterDefiner,defaultParams={},onSubmit}=props;
        if(onSubmit){
            onSubmit.validate=this.validate;
        }
        this.parameterDefiner=new ParameterDefiner(parameterDefiner);
        this.parameterDefiner.updateParametersValue(defaultParams);
        this.state = {dispalyItems:[]}
    }

    componentDidMount() {
        let {defaultParams}=this.props;
        this.parameterDefiner.getParameterDef().then(defs=>{
            let dispalyItems=defs,initParams={},dynamicItems=[];
            if(dynamicItems&&dynamicItems.length>0){
                dynamicItems.forEach(di=> {
                        let diString=JSON.stringify(di);
                        //存在变量替换
                        if(diString.includes("$(")){
                            di=JSON.parse(diString,function (key,value) {
                                if(typeof value ==='string'&&value.includes("$")){
                                    return util.renderTemplateString(value,defaultParams)
                                }
                                return value;
                            });
                        }
                        let item = dispalyItems.filter(d => d.name === di.name)[0];
                        if(item){
                            Object.assign(item,di);
                        }else{
                            //增加可能存在的初始值
                            let initValue=defaultParams[di.name];
                            if([4,11,14,17].includes(di.type)&&initValue&&!Array.isArray(initValue)){
                                initValue=initValue.split(",");
                            }
                            if(initValue) {
                                di.initValue=initValue
                            }
                            dispalyItems.push(di);
                        }
                    }
                )
            }
            dispalyItems.forEach((di,index)=>{
                if(!di.order){
                    di.order=index+1;
                }
                //添加元素表单元素定义扩展元素
                di.extensionProps={
                    formItems:dispalyItems,
                    history: this.props.history,
                    formInitParams:defaultParams,
                    closeAction:this.closeAndRefresh
                };
                if(di.initValue){
                    initParams[di.name]=di.initValue
                }
            });
            dispalyItems.sort(function(a,b){return a.order-b.order});
            this.setState({
                loading: false,
                dispalyItems:dispalyItems
            });

        })
    }

    validate = () => {
        let pass=false,{onSubmit,items}=this.props;
        this.action_form_container.validateFieldsAndScroll((err, values) => {
            if (!err) {
                let allValues={};
                /*items.forEach((item,index)=> {
                    let {defModel={}}=item;
                    let {displayType}=defModel;
                    if(displayType==='advanced'){
                        allValues[item.name]=null;
                    }
                });*/
                Object.assign(allValues,values);
                this.prevSubmitValues=Object.assign({},allValues);
                if(onSubmit) onSubmit(allValues);
                pass=allValues;
            }
        });
        return pass
    };

    getParameterDefiner=()=>{
        let values=this.action_form_container.getFieldsValue();
        this.parameterDefiner.updateParametersValue(values);
        return JSON.parse(JSON.stringify(this.parameterDefiner));
    };

    getFieldsValue = () => {
        return this.action_form_container.getFieldsValue();
    };

    render() {
        let {dispalyItems,loading}=this.state,{formItemLayout,onValuesChange,bordered=true}=this.props;
        return (
            <Card size="small" bordered={bordered}><div>
                <Spin spinning={loading}>
                    <SimpleForm
                        items={dispalyItems}
                        formItemLayout={formItemLayout||defaultLayout}
                        ref={r => this.action_form_container = r}
                        onValuesChange={onValuesChange}
                    />
                </Spin>
            </div></Card>
        )
    }
}





