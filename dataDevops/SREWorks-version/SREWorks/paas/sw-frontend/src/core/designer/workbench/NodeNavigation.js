/**
 * Created by caoshuaibiao on 2020/11/3.
 * 菜单节点上的tabs定义导航
 */
import React from 'react';

import {
    FilterOutlined,
    FormOutlined,
    LayoutOutlined,
    PicRightOutlined,
    PlusCircleOutlined,
    QuestionCircleOutlined,
    ToolOutlined,
} from '@ant-design/icons';

import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';

import {
    Layout,
    Menu,
    Dropdown,
    Tree,
    Tooltip,
    Modal,
    Spin,
    Button,
    Popover,
    Select,
    Popconfirm,
} from 'antd';
import uuidv4 from 'uuid/v4';
import Constants from '../../framework/model/Constants';
import {BlockPropsSettingForm} from '../editors/BlockEditor';


export default class NodeTabsNavigation extends React.Component {

    static defaultProps = {
        nodeGroupData:[]
    };

    constructor(props) {
        super(props);
        this.state={
            visible: false,
            createKey:0,
            nodeGroupData:props.nodeGroupData
        };
        this.currentCreateConf={};
    }

    addElement=(eType)=>{
        let {onAddItem,nodeGroupData}=this.props;
        if(this.createForm){
            this.createForm.props.form.validateFieldsAndScroll((err, values) => {
                if (!err) {
                    let {name,category,label}=this.currentCreateConf;
                    let item={
                        "id": name,
                        "label":label,
                        "name":name,
                        "type":Constants.BLOCK_TYPE_BLOCK,
                        "category":category
                    };
                    onAddItem&&onAddItem(item);
                    this.close();
                }
            });
        }
        /*
        let selectGroup=nodeGroupData.filter(group=>group.type===eType)[0];
        let {items,type}=selectGroup;
        let item={
                "id": uuidv4(),
                "label":`新建${selectGroup.label}${items.length}`,
                "type":type
        };
        */

    };

    removeElement=(eModel)=>{
       console.log("removeElement------->",eModel);
    };

    handleVisibleChange = visible => {
        this.setState({ visible });
    };

    setSettingForm=(form)=>{
        this.createForm=form;
    };

    renderItemGroups=(groups=[])=>{
        let {visible,createKey}=this.state,setSettingForm=this.setSettingForm;
        if(groups && groups.length) {
            return groups.map(group=>{
                let {items,icon,type,label, tooltip}=group;
                return (
                    <Menu.ItemGroup key={type}
                                    group={group}
                                    title={
                                        <div style={{position:"relative"}}>
                                            <label>
                                                {label}&nbsp;
                                                {tooltip && <em className="optional">
                                                    <Tooltip title={tooltip}>
                                                        <QuestionCircleOutlined />
                                                    </Tooltip>
                                                </em>}
                                            </label>
                                            <span style={{position:'absolute',right:8}}>
                                                <Popover placement="right" title={"选择区块类型"}
                                                         visible={visible}
                                                         onVisibleChange={this.handleVisibleChange}
                                                         overlayStyle={{width:660}}
                                                         content={
                                                             <div  style={{overflow:"hidden"}}>
                                                                 <div>
                                                                     <BlockPropsSettingForm  key={createKey} wrappedComponentRef={(form) => setSettingForm(form)} config={{category:Constants.BLOCK_CATEGORY_ACTION,name:uuidv4()}} onValuesChange={(changedField,allValue)=>this.currentCreateConf=allValue}/>
                                                                 </div>
                                                                 <div style={{float: "right"}}>
                                                                     <Button style={{marginRight: 10}} onClick={this.close}
                                                                             size="small">取消</Button>
                                                                     <Button size="small" type="primary"
                                                                             onClick={this.addElement}>确认</Button>
                                                                 </div>
     
                                                             </div>
                                                         } trigger="click">
                                                    <a style={{cursor: "pointer"}}><PlusCircleOutlined /></a>
                                                 </Popover>
                                            </span>
                                        </div>
                                    }>
                        {
                            items.map(item=>{
                                let {id,label,name,category}=item;
                                return (
                                    <Menu.Item key={id}>
                                        <span style={{marginLeft:15}}>
                                            <Tooltip title={`${label}`}>
                                              {category===Constants.BLOCK_CATEGORY_ACTION&&<ToolOutlined />}
                                              {category===Constants.BLOCK_CATEGORY_FILTER&&<FilterOutlined />}
                                              {category===Constants.BLOCK_CATEGORY_PAGE&&<PicRightOutlined />}
                                              {category===Constants.BLOCK_CATEGORY_FORM&&<FormOutlined />}
                                              {label}
                                            </Tooltip>
                                        </span>
                                    </Menu.Item>
                                );
                            })
                        }
                    </Menu.ItemGroup>
                );
             });
        }
    };

    handleClick=(menu)=>{
        //console.log("click menu----->",menu);
        let {nodeGroupData}=this.props;
        let {mainPageClick,onItemClick}=this.props;
        if(menu.key==="MAIN_PAGE"){
            mainPageClick&&mainPageClick();
        }else{
            //点击的为其他区块或者表单
            let selectItem=null;
            nodeGroupData.forEach(group=>{
                group.items.forEach(item=>{
                    if(item.id===menu.key){
                        selectItem=item;
                    }
                })
            });
            onItemClick&&onItemClick(selectItem)
        }

    };

    close = e => {
        this.setState({
            visible: false,
            createKey:++this.state.createKey,
        });
    };

    render() {
        let {nodeGroupData}=this.props;
        return (
            <div>
                <Menu
                    onClick={this.handleClick}
                    style={{ width: 180 }}
                    inlineIndent={12}
                    defaultSelectedKeys={['MAIN_PAGE']}
                    mode="inline"
                >
                    <Menu.ItemGroup key="MAIN" title={<div>页面</div>}>
                        <Menu.Item key="MAIN_PAGE">
                            <span style={{marginLeft:15}}><LayoutOutlined />主页面</span>
                        </Menu.Item>
                    </Menu.ItemGroup>
                    {
                        this.renderItemGroups(nodeGroupData)
                    }
                </Menu>
            </div>
        );
    }
}