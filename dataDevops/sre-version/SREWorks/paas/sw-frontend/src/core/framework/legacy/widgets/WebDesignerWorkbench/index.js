/**
 * Created by caoshuaibiao on 2020/11/3.
 */
import React, { Component } from 'react';
import Workbench from '../../../../designer/workbench';
import { FolderOpenOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import { Spin, Button, Popover, Select, Popconfirm } from "antd";
import httpClient from "../../../../../utils/httpClient";
import * as util from "../../../../../utils/utils";
import FormElementType from '../../../../../components/FormBuilder/FormElementType';
import FormElementFactory from '../../../../../components/FormBuilder/FormElementFactory';
import $ from 'jquery';
import properties from "../../../../../properties";
import cacheRepository from '../../../../../utils/cacheRepository'
//const productopsPrefix = "gateway/v2/common/productops";
// const productopsPrefix = (properties.envFlag===properties.ENV.Standalone?
//     "gateway/v2/foundation/frontend-service/frontend"
//     :"gateway/v2/common/productops");
const productopsPrefix =
    "gateway/v2/foundation/frontend-service/frontend";

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 18 },
    },
};
class TemplateSetting extends React.Component {

    constructor(props) {
        super(props);
        let { config } = props;
        let paramsDef = [], { templateType, layout } = config;
        // paramsDef.push({type:FormElementType.SELECT,name:'templateType',initValue:templateType,required:true,label:"模板类型",inputTip:"应用的前端功能模板",
        //     optionValues:[{value:"blank",label:'空应用'}]
        // });
        paramsDef.push({
            type: FormElementType.RADIO, name: 'layout', initValue: layout, required: true, label: "空间布局", inputTip: "页面菜单布局",
            optionValues: [
                { value: "PaaS", label: '上下布局' },
                { value: "abm", label: '左右布局' },
                { value: "empty", label: '空布局' },
            ]
        });
        this.itemDef = paramsDef;
    }


    buildingFormElements = () => {
        let { form } = this.props;
        let formChildrens = this.itemDef.map(item => FormElementFactory.createFormItem(item, form, formItemLayout));
        return (

            <div>
                {
                    [

                        formChildrens[0],
                    ]
                }
            </div>


        )
    };

    render() {

        let formChildrens = this.buildingFormElements();
        return (
            <Form>
                {formChildrens}
            </Form>
        );
    }

}
const TemplateSettingForm = Form.create({
    onValuesChange: (props, changedValues, allValues) => props.onValuesChange && props.onValuesChange(changedValues, allValues)
})(TemplateSetting);

class WebDesignerWorkbench extends Component {

    constructor(props) {
        super(props);
        this.state = {
            exists: false,
            loading: true,
            showCreate: false,
            appId: props.mode && props.mode.config && props.mode.config.appId
        };
        this.templateConfig = {
            templateType: "blank",
            layout: "PaaS"
        };
    }

    componentWillMount() {
        let { appId } = this.state;
        let envLabel = 'dev';
        httpClient.get(productopsPrefix + `/apps/${appId}/exists?stageId=${envLabel}`, { headers: { 'X-Biz-App': util.getNewBizApp() } }).then(result => {
            this.setState({
                //TODO 根据接口数据来控制是不是显示创建选择页面
                // exists: result.exists,//是否存在
                exists: result.exists,
                loading: false
            });
        })
            .catch(err => {
                // todo 暂时处理
                this.setState({
                    exists: true,
                    loading: false
                });
            })
    }


    createByTemplate = (template) => {
        let { appId } = this.state;
        let blankTemplate = {
            "admins": [],
            "options": {
                "buCode": "JSPT",
                "description": appId,
                "dingdingQrcodeConfig": "[]",
                "docsUrl": "",
                "initAccessUrl": "",
                "navLink": "",
                "isHasDingdingQrCode": 0,
                "isHasNav": 1,
                "isHasNotify": 1,
                "isHasSearch": true,
                "isNeedDailyEnv": 1,
                "isShowInNav": 1,
                "logoImg": "",
                "nameCn": appId,
                "organization": "组织",
                "productLink": "",
                "saKey": "",
                "saSecret": "",
                "type": 0,
                "templateLabel": "label",
                "appType": "PaaS"
            },
            "templateName": "blank_app",
            "appId": appId,
            "environments": ["default,dev"],
            "version": 0
        };
        let { layout } = this.templateConfig, layoutConfig = {};
        if (layout === 'PaaS') {
            layoutConfig = {
                "header": {
                    type: 'brief'
                }
            }
        } else if (layout === 'empty') {
            layoutConfig = {
                "type": "empty"
            }
        }

        blankTemplate.options.layout = JSON.stringify(layoutConfig);
        if (properties.devFlag === "dev") {
            httpClient.post("gateway/v2/foundation/frontend-service/frontend/apps/init", blankTemplate, { headers: { 'X-Biz-App': util.getNewBizApp(appId) } }).then(result => {
                this.setState({
                    exists: true,
                    showCreate: false
                });
            })
        }
        // 调用新增app api
        let params = {
            options: {
                layout: layoutConfig
            },
            appId
        };
        let roleParams = {
            description: "访客",
            name: "访客",
            options: [{ locale: "zh_CN", name: "访客", description: "访客" }],
            roleId: `${appId}:guest`,
            appId
        }
        httpClient.post("gateway/v2/foundation/appmanager/apps", params, { headers: { 'X-Biz-App': `${appId},${properties.defaultNamespace},dev` } }).then(result => {
        }).catch(error => {
            console.log(error)
        })
        httpClient.post("gateway/v2/common/authProxy/roles", roleParams, { headers: { 'X-Biz-App': `${appId},${properties.defaultNamespace},dev` } }).then(result => {
            console.log(result)
        }).catch(error => {
            console.log(error)
        })
        // 应后端要求的临时初始化方案
        let addonParams = { "addonType": "INTERNAL_ADDON", "addonName": "productopsv2", "addonId": "productopsv2" }
        httpClient.post(`gateway/v2/foundation/appmanager/apps/${appId}/addon`, addonParams, { headers: { 'X-Biz-App': `${appId},${properties.defaultNamespace},dev` } }).then(result => {
            console.log(result)
        }).catch(error => {
            console.log(error)
        })
        // dev/prod环境隔离
        // let {currentProduct} = this.props.global;
        // let stageLabel ='';
        // if(currentProduct && currentProduct.productId) {
        //     stageLabel = cacheRepository.getEnv(currentProduct.productId)==='dev'? 'dev' : 'prod';
        // }
        let stageLabel = 'dev'
        return httpClient.post(productopsPrefix + "/apps/init?stageId=" + stageLabel, blankTemplate, { headers: { 'X-Biz-App': `${appId},${properties.defaultNamespace},dev` } }).then(result => {
            this.setState({
                exists: true,
                showCreate: false
            });
        })

    };


    handleCreate = () => {
        this.setState({
            showCreate: true
        });
    };

    close = () => {
        this.setState({
            visible: false,
        });
    }

    handleVisibleChange = visible => {
        this.setState({ visible });
    };

    render() {
        const { __emp_id__ } = this.props.nodeParams, { config = {} } = this.props.mode, { exists, loading, showCreate, visible } = this.state;
        if (loading) {
            return <Spin spinning={loading} />
        }
        // console.log(x_biz_app);
        //menuHolderHeight 前端设计器顶部菜单占用的内容显示区高度，72为前端开发在顶部作为菜单时候的占用高度
        let x_biz_app = `${this.state.appId}|default|dev`, { menuHolderHeight = 72 } = config;
        //新建的进行模板选择创建
        if (!exists) {
            return (
                <div style={{ height: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column' }}>
                    <div>
                        <h1 style={{ opacity: 0.8 }}> </h1>
                    </div>
                    <div>
                        <FolderOpenOutlined style={{ fontSize: 200 }} />
                    </div>
                    <div style={{ padding: 12 }}>
                        <h3 style={{ opacity: 0.5 }}>当前应用还未开启前端页面托管</h3>
                    </div>
                    {/*todo 暂时是这样的交互*/}
                    <div>
                        <Popover placement="top" title={"选择模版"}
                            visible={this.state.visible}
                            onVisibleChange={this.handleVisibleChange}
                            overlayStyle={{ width: 480 }}
                            content={
                                <div style={{ overflow: "hidden" }}>
                                    <div>
                                        <TemplateSettingForm config={this.templateConfig} onValuesChange={(changedField, allValue) => this.templateConfig = allValue} />
                                    </div>
                                    <div style={{ float: "right" }}>
                                        <Button style={{ marginRight: 10 }} onClick={this.close}
                                            size="small">取消</Button>
                                        <Button size="small" type="primary"
                                            onClick={this.createByTemplate}>确认</Button>
                                    </div>

                                </div>
                            } trigger="click">
                            <Button>立即开启</Button>
                        </Popover>
                    </div>
                </div>
            );
        }
        return (
            <Workbench {...this.props} stageHeight={$('#__MAIN_CONTENT__').height() - menuHolderHeight} x_biz_app={x_biz_app} appId={this.state.appId} userEmpId={__emp_id__} {...config} />
        );
    }
}

export default WebDesignerWorkbench;
