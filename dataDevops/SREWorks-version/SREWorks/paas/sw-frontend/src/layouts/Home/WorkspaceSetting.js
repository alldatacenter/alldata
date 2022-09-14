/**
 * Created by caoshuaibiao on 2020/12/10.
 * 桌面设定
 */
import React from "react";
import {
    DeleteOutlined,
    EditOutlined,
    MinusCircleOutlined,
    PlusCircleOutlined,
    PlusOutlined,
} from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import './workspaceSetting.less';
import { Modal, Divider, Upload, Tabs, Button, Spin, message, Tooltip } from "antd";
import { connect } from 'dva';
import AppStore from './AppStore';
import AppService from "../../core/services/appService"
import SimpleForm from "../../components/FormBuilder/SimpleForm";
import uuidv4 from 'uuid/v4';
import * as util from "../../utils/utils";
import properties from "appRoot/properties";
import { localImglist, requiredImglist } from "./localImglist";
import cacheRepository from "../../utils/cacheRepository";
const { TabPane } = Tabs;

const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 6 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 24 },
        md: { span: 16 },
    },
};
@connect(({ home }) => ({
    home: home
}))
export default class WorkspaceSetting extends React.Component {


    constructor(props) {
        super(props);
        this.state = {
            visible: false,
            isDelete: false,
            quickVisible: false,
            imgList: [],
            loading: false,
            bacgroundSettingVisible: false,
            targetDesktopIndex: -1,
            activeBackgroundImgIndex: -1,
        }
    }

    componentDidMount() {
    }

    handleOk = e => {
        const { dispatch, home } = this.props;
        const { checkedAppStore, appStore, customQuickList, workspaces, desktopIndex } = home;
        let { settingType } = this.state;
        if (settingType === "add") {
            let checked = [];
            (appStore.concat(customQuickList) || []).map(item => {
                if (checkedAppStore[item.id]) {
                    checked.push(item);
                }
            });
            if (workspaces.length === 0) {
                dispatch({
                    type: "home/setWorkspaces", workspaces: [{
                        name: "主页",
                        type: "main",
                        hasSearch: true,
                        items: checked,
                    }],
                });
            } else {
                workspaces[desktopIndex].items = checked;
                dispatch({
                    type: "home/setWorkspaces", workspaces,
                });
            }
        }
        this.setState({
            visible: false,
        }, () => {
            //清空选择
            dispatch({ type: "home/updateCheckedAppStore", checkedAppStore: {} });
        });
    };


    showSetting = (type) => {
        this.setState({
            visible: true,
            settingType: type,
        }, () => {
            const { dispatch, home } = this.props;
            const { checkedAppStore, appStore, workspaces, desktopIndex } = home;
            if (workspaces[desktopIndex]) {
                (workspaces[desktopIndex].items || []).map(item => {
                    checkedAppStore[item.id] = true;
                });
                dispatch({ type: "home/updateCheckedAppStore", checkedAppStore });
            }
        });
    };

    handleCancel = e => {
        const { dispatch } = this.props;
        this.setState({
            visible: false,
        }, () => {
            dispatch({ type: "home/updateCheckedAppStore", checkedAppStore: {} });
        });
    };

    handleDelete = () => {
        const { dispatch } = this.props;
        dispatch({ type: 'home/switchDeleteState' });
    };

    handleAddWorkspace = () => {
        const { dispatch, home } = this.props;
        dispatch({ type: 'home/addWorkspace', workspace: { name: "新建桌面 " + home.workspaces.length } });
    };

    handleRemoveWorkspace = (index) => {
        const { dispatch, home } = this.props;
        let { workspaces } = home;
        workspaces.splice(index, 1);
        dispatch({ type: "home/updateWorkspace", workspaces: workspaces });
    };
    handleUpdateWorkspace() {
        const { dispatch, home } = this.props;
        const { activeBackgroundImgIndex, targetDesktopIndex, imgList } = this.state;
        let mixImglist = localImglist.concat(imgList);
        if (activeBackgroundImgIndex === -1) {
            message.info("请先选择要设置的背景图片")
        }
        let { workspaces } = home;
        workspaces.forEach((item, index) => {
            if (index === targetDesktopIndex) {
                item.background = mixImglist[activeBackgroundImgIndex]
            }
        })
        dispatch({ type: "home/updateWorkspace", workspaces: workspaces });
        this.setState({ bacgroundSettingVisible: false });
    }
    handleAddItems = () => {
        const { dispatch } = this.props;
    };

    handleSave = () => {
        const { home } = this.props;
        const { workspaces, collectList } = home;
        let params = {
            workspaces,
            collectList,
        };
        let namespaceId = util.getNewBizApp().split(",")[1];
        let stageId = util.getNewBizApp().split(",")[2];
        AppService.postWorkspaces(params, namespaceId, stageId)
            .then(res => {

            });
    };

    handleCreateQuick = () => {
        this.setState({
            quickVisible: true,
        });
    };

    onSubmit = (res) => {
        const { dispatch, home } = this.props;
        let { customQuickList, collectList, workspaces, } = home;
        if (res) {
            customQuickList.push({ id: uuidv4(), appName: res.appName, href: res.href, introduction: res.href });
            dispatch({ type: "home/setDesktopData", collectList, workspaces, customQuickList });
            this.setState({
                quickVisible: false,
            });
        }
    };

    onClose = () => {
        this.setState({
            quickVisible: false,
        });
    };

    handleDeleteQuick = () => {
        let { isDelete } = this.state;
        isDelete = !isDelete;
        this.setState({
            isDelete
        })
    };
    handleUpdateImge = (imgList) => {
        const params = {
            imgList
        }
        AppService.updateDesktopBackgroundList(params).then(res => {
            this.getImageList();
        });
    };
    getImageList = () => {
        const { dispatch } = this.props;
        AppService.getDesktopBackgroundList().then(res => {
            let imgList = (res && res.imgList) || []
            this.setState({ imgList });
            dispatch({
                type: 'home/setImgList',
                imgList
            })
        })
    };
    deleteImage = (i) => {
        const { imgList } = this.state;
        this.handleUpdateImge(imgList.filter(item => item !== i));
    }
    handleChange = (info) => {
        const { imgList } = this.state;
        if (imgList.length > 10) {
            message.info("最多可内置10个背景图片")
            return false
        }
        if (info.file.status === "uploading") {
            this.setState({ loading: true });
            return;
        } else {
            this.setState({ loading: false });
        }
        if (["done", "removed"].includes(info.file.status)) {
            let { file } = info;
            imgList.push(file.response.data);
            this.handleUpdateImge(imgList);
            message.success(`${info.file.name} 上传成功`);
        } else if (info.file.status === "error") {
            message.error(`${info.file.name} 上传失败`);
        }
    };
    componentWillMount() {
        this.getImageList();
    };
    onCloseBackgroundSetting = () => {
        this.setState({ bacgroundSettingVisible: false })
    };
    showSettingModal = (i) => {
        const { home } = this.props;
        const { workspaces } = home;
        const { imgList } = this.state;
        let mixImglist = localImglist.concat(imgList);
        let active_index = -1;
        mixImglist.forEach((item, j) => {
            if (workspaces[i].background === item) {
                active_index = j
            }
        })
        this.setState({ targetDesktopIndex: i });
        this.setState({ activeBackgroundImgIndex: active_index });
        this.setState({ bacgroundSettingVisible: true })
    };
    setActiveBackgroundIndex = (j) => {
        this.setState({ activeBackgroundImgIndex: j });
    }
    getProductName = () => {
        let productName = window.location.hash.split("/")[1];
        if (!productName) {
            if (properties.defaultProduct && !properties.defaultProduct.includes("$")) {
                productName = properties.defaultProduct;
            } else {
                productName = 'desktop';
            }
        }
        if (productName.includes("?")) {
            productName = productName.split("?")[0];
        }
        return productName;
    }
    render() {
        const { visible, settingType, quickVisible, isDelete } = this.state, { home } = this.props;
        const { imgList, loading, bacgroundSettingVisible, activeBackgroundImgIndex } = this.state;
        let { workspaces } = home;
        let mixImglist = requiredImglist.concat(imgList);
        let originMixImglist = localImglist.concat(imgList);
        let productId = this.getProductName();
        let bizAppId = cacheRepository.getBizEnv(productId)
        const footer = () => {
            if (settingType === "add") {
                return [<Button type="danger" onClick={this.handleDeleteQuick}>删除快捷方式</Button>,
                <Button type="primary" onClick={this.handleCreateQuick}>新建快捷方式</Button>,
                <Button type="primary" onClick={() => window.open(`/#/swadmin/mart/local`)}>去部署</Button>,
                <Button type="primary" onClick={this.handleOk}>添加到桌面</Button>,
                ];
            }
            return null
        };
        return (
            <div className="workspace-setting">
                <div className="edit-icon">
                    <PlusCircleOutlined className="icon" onClick={() => this.showSetting('add')} />
                    <MinusCircleOutlined className="icon" onClick={this.handleDelete} />
                    <EditOutlined className="icon" onClick={() => this.showSetting('setting')} />
                </div>
                <Modal title="新建快捷方式" visible={quickVisible} footer={null} onCancel={this.onClose}>
                    <SimpleForm
                        formItemLayout={formItemLayout}
                        items={[
                            { type: 1, name: "appName", required: true, initValue: "", label: "名称" },
                            {
                                type: 1,
                                name: "href",
                                required: true,
                                initValue: "",
                                label: "url",
                                inputTip: "请输入https:// 或http://开头的地址",
                            },
                        ]}
                        onSubmit={this.onSubmit}
                    />
                    <div style={{ display: "flex", justifyContent: "center" }}>
                        <Button onClick={() => this.onClose()}>取消</Button>
                        <Button style={{ marginLeft: 16 }} type="primary"
                            onClick={() => this.onSubmit.validate()}>确定</Button>
                    </div>
                </Modal>
                <Modal title="设置桌面背景" width="60%" visible={bacgroundSettingVisible} footer={null} onCancel={this.onCloseBackgroundSetting}>
                    <div className="imglist-panel">
                        {
                            !!mixImglist.length && mixImglist.map((img, j) => {
                                return (
                                    <div className={activeBackgroundImgIndex === j ? 'img-wrapper isLight' : 'img-wrapper'} key={img}>
                                        <img src={img} onClick={() => this.setActiveBackgroundIndex(j)} />
                                    </div>
                                )
                            })
                        }
                        {
                            !!(mixImglist.length === 0) && (<div>请先添加自定义桌面背景图片</div>)
                        }
                    </div>
                    <div style={{ display: "flex", justifyContent: "center" }}>
                        <Button onClick={() => this.onCloseBackgroundSetting()}>取消</Button>
                        <Button style={{ marginLeft: 16 }} type="primary"
                            onClick={() => this.handleUpdateWorkspace()}>确定</Button>
                    </div>
                </Modal>

                <Modal
                    title={settingType === 'add' ? "添加桌面内容" : "桌面设置"}
                    visible={visible}
                    footer={footer()}
                    onCancel={this.handleCancel}
                    width="70%"
                    bodyStyle={{ padding: 0, height: 'calc(60vh)' }}
                >
                    {
                        settingType === 'setting' &&
                        <Tabs tabPosition={"left"} size="small" style={{ height: '100%' }} className="abm-home-page-modal">
                            <TabPane tab="桌面空间" key="workspace">
                                <div className="appSet">
                                    {
                                        workspaces.map((child, i) => {
                                            return (
                                                <div className="desktop-item-block">
                                                    {i > 0 && <span className="removeHandle" onClick={() => this.handleRemoveWorkspace(i)}>
                                                        <DeleteOutlined />
                                                    </span>}
                                                    <div>
                                                        <div className="icon-wrapper">
                                                            {/* <img className="icon" src={child.background}  onClick={()=>{child.link&&window.open(child.link,"_blank")}} /> */}
                                                            {
                                                                originMixImglist.includes(child.background) && <img className="icon" src={localImglist.includes(child.background) ? requiredImglist[localImglist.indexOf(child.background)] : child.background} onClick={() => this.showSettingModal(i)} />
                                                            }
                                                            {
                                                                !originMixImglist.includes(child.background) && <img className="icon" src={requiredImglist[0]} onClick={() => this.showSettingModal(i)} />
                                                            }
                                                            <div class="icon-mask" onClick={() => this.showSettingModal(i)}><div style={{color:'var(--PrimaryColor)',textAlign:'center',verticalAlign:'middle',lineHeight:'96px'}}>选择背景</div></div>
                                                        </div>
                                                    </div>
                                                    <div>
                                                        <span>桌面 {i + 1}</span>
                                                    </div>
                                                </div>
                                            );
                                        })
                                    }
                                    <div>
                                        <Button icon={<PlusOutlined />} type="dashed" style={{ height: 96, marginTop: 24 }} onClick={this.handleAddWorkspace}>添加桌面</Button>
                                    </div>
                                </div>
                            </TabPane>
                            <TabPane tab="桌面背景" key="background">
                                <Spin spinning={loading}>
                                    <div className="appSet">
                                        {
                                            mixImglist && !!mixImglist.length && mixImglist.map((child, i) => {
                                                return (
                                                    <div key={child} className="desktop-item-block">
                                                        {i > 3 && <span className="removeHandle" onClick={() => this.deleteImage(child)}>
                                                            <DeleteOutlined />
                                                        </span>}
                                                        <div>
                                                            <span className="icon-wrapper">
                                                                <img alt="image" className="icon" src={child} />
                                                            </span>
                                                        </div>
                                                        <div>
                                                            <span>图片 {i + 1}</span>
                                                        </div>
                                                    </div>
                                                );
                                            })
                                        }
                                        <div style={{ marginTop: 20 }}>
                                            <Upload
                                                listType="picture-card"
                                                className="avatar-uploader"
                                                showUploadList={false}
                                                accept="image/*,.pdf"
                                                withCredentials={true}
                                                maxCount={1}
                                                action={`${properties.baseUrl}gateway/sreworks/other/sreworksFile/put`}
                                                headers= {{
                                                    'x-biz-app': bizAppId
                                                }}
                                                onChange={this.handleChange}
                                            >
                                                <Tooltip placement='top' title="建议添加1960*966分辨率/比例的图片">
                                                    <div icon={<PlusOutlined />}> + 新增背景</div>
                                                </Tooltip>
                                            </Upload>
                                        </div>
                                    </div>
                                </Spin>
                            </TabPane>
                        </Tabs>
                    }
                    {
                        settingType === 'add' &&
                        <Tabs tabPosition={"left"} size="small" style={{ height: '100%' }} className="abm-home-page-modal">
                            <TabPane tab="快捷方式" key="normal">
                                <div style={{ height: 'calc(60vh - 40px)', overflow: "auto" }}>
                                    <AppStore isDelete={isDelete} onChange={this.handleAddItems} />
                                </div>
                            </TabPane>
                            {/* <TabPane tab="快捷卡片" key="mon">

                                </TabPane> */}
                        </Tabs>
                    }
                </Modal>
            </div>
        );
    }
}