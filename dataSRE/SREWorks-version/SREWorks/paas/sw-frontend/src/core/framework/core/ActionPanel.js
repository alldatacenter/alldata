/**
 * Created by caoshuaibiao on 2021/3/20.
 * 操作面板，划出/弹出
 */
import React from 'react';
import { Drawer, Button,Modal } from 'antd';
import PageContent from './PageContent';
import PageModel from '../model/PageModel';
import Bus from '../../../utils/eventBus'

const sizeMapping={
    "small":{
        percent:0.4,
        min:360,
        labelSpan:6
    },
    "default":{
        percent:0.6,
        min:640,
        labelSpan:4
    },
    "large":{
        percent:0.8,
        min:900,
        labelSpan:3
    },
};

class ActionPanel extends React.Component {

    constructor(props) {
        super(props);
        let {nodeModel,currentAction}=props;
        let {block,name}=currentAction;
        //侧滑出来的一定是定义好的区块页面,name为兼容非工具栏模式下配置人容易识别的业务操作标识
        let blockDef=nodeModel.getBlocks().filter(blockMeta=>blockMeta.elementId===block||blockMeta.name===name)[0],pageModel=null;
        if(blockDef){
            pageModel=new PageModel(blockDef);
            pageModel.setNodeModel(nodeModel);
        }
        this.state = {
            visible: true,
            pageModel
        };
    }

    componentDidMount() {
       Bus.on('stepFormClose', (msg) => {
            this.setState({
                visible: false,
            });
        });
    }
    componentWillUnmount() {
        Bus.off('stepFormClose',(msg) => {
            this.setState({
                visible: false,
            });
        });
    }
    showDrawer = () => {
        this.setState({
            visible: true,
        });
    };

    onClose = () => {
        let {onClose}=this.props;
        this.setState({
            visible: false,
        });
        onClose&&onClose();
    };

    render() {
        const {pageModel,visible}=this.state, {currentAction}=this.props;
        let {displayType,size,label,actionParams}=currentAction;
        let sizeDef=sizeMapping[size];
        if(!sizeDef){
            sizeDef=sizeMapping.default;
        }
        let defaultSize=document.body.clientWidth*sizeDef.percent;
        if(sizeDef.min>defaultSize){
            defaultSize=sizeDef;
        }
        if(sizeDef.min>document.body.clientWidth){
            defaultSize=document.body.clientWidth;
        }
        let actionContent=null;
        if(!pageModel){
            actionContent=<div style={{width:"100%",height:"100%",justifyContent: "center",alignItems:"center",display:"flex"}}><h3>未找到定义</h3></div>;
        }else{
            actionContent=<PageContent onClose={this.onClose} {...this.props}  actionParams={actionParams}  pageModel={pageModel}/>;
        }
        if(displayType==='modal'){
            return (
                <Modal
                    title={label||(pageModel&&pageModel.label)}
                    visible={visible}
                    footer={null}
                    width={defaultSize}
                    maskClosable={true}
                    destroyOnClose={true}
                    onCancel={this.onClose}
                >
                    {actionContent}
                </Modal>
            )
        }
        return (
            <div>
                <Drawer
                    title={label||(pageModel&&pageModel.label)}
                    placement="right"
                    width={defaultSize}
                    closable={true}
                    onClose={this.onClose}
                    visible={visible}
                >
                    {actionContent}
                </Drawer>
            </div>
        );
    }
}

export default ActionPanel;