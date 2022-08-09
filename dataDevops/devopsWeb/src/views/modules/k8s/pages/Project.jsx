import React from 'react';
import PropTypes from 'prop-types';
import {Layout} from 'antd';
import { Select, Button, Modal, Row, Col, Popconfirm, Radio, Checkbox} from 'antd';
const {Content} = Layout;
const Option = Select.Option;
import { Link } from 'react-router';
import UtilFn from 'js/util.js';
import InterfaceFn from 'js/interface.js';
import ProjectMachine from '../components/ProjectMachine';
import AppMachine from '../components/AppMachine';

class Project extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            projectId: this.props.projectId,
            appId: this.props.appId,
            modalProjectConfigClose: true,
            viewKey: 'projectList',
            projectList: [], // 所有项目数据
            appList: [], // 所有app数据
            appNotExtendsArr: [],//继承机器的APP
            clusterList: null, // 环境列表
            currCluster: '', // 当前环境
            currProject: null, // 当前项目
            currAppList: [], // 当前项目的app列表
            currAppId: 0,
            appModalClose: false,
            visibleFlag: [],//气泡确认框可见
            extendsFlag: [],
            changeFlag: false,
            tipText: '',
        };
    }

    // 切换 currCluster
    handleChangeCluster = (value) => {
        this.setState({
            currCluster: '',
        });
        setTimeout(() => {
            this.state.currCluster = value;
            this.setState({
                currCluster: value,
            });
            this.getAppNotExtends();
        }, 1000);
    }

    //获取项目列表数据
    loadProjectList = () => {
        InterfaceFn.getProjectList({
            offset: 0,
            limit: 1000,
        }, (resData) => {
            this.setState({
                projectList: resData,
            });
        });
    }

    //获取App列表数据
    loadAppList = () => {
        InterfaceFn.getAppList({
            // where: `project_id = ${this.state.projectId}`,
            offset: 0,
            limit: 1000,
        }, (resData) => {
            this.setState({
                appList: resData,
            });
        });
    }
    //获取环境列表数据
    loadCluster = () => {
        InterfaceFn.getCluster({
            offset: 0,
            limit: 1000,
        }, (resData) => {
            //先判断环境列表的长度大于零,再改变当前环境
            if (resData.length > 0) {
                this.setState({
                    clusterList: resData,
                    currCluster: resData[0].name,
                });
            };
            // this.getAppNotExtends();
        });
    }
    // 选择项目
    selectProject = (index) => {
        let currProject = this.state.projectList[index];
        this.setState({
            currProject: null,
            currAppList: this.state.appList.filter(app => app.projectId == currProject.id),
            visibleFlag: [],
            extendsFlag: [],
        });
        this.getAppNotExtends();
        setTimeout(() => {
            this.setState({
                currProject: currProject,
            });
        }), 1;
    }

    //显示App机器列表的Modal
    handleAppModal = (appId) => {
        this.setState({
            appModalClose: !this.state.appModalClose,
            currAppId: appId,
        })
    }
    //拉取非继承的applist
    getAppNotExtends = () =>{
        InterfaceFn.getAppNotExtendsList({
            where: `k8s_cluster = "${this.state.currCluster}"`,
            offset:0,
            limit:100,
        }, (resData) => {
            let extendsFlag = [];
            let visibleFlag = [];
            this.state.currAppList.forEach((app) => {
                let eFlag = true;
                let vFlag = false;
                resData.forEach((item) => {
                    if (item.appid == app.id){
                        eFlag = false;
                        vFlag = false;
                    };
                });
                extendsFlag.push(eFlag);
                visibleFlag.push(vFlag);
            })
            this.setState({
                    appNotExtendsArr: resData,
                    extendsFlag: extendsFlag,
                    visibleFlag: visibleFlag,
            })
        })
    }
    //继承机器
    addAppExtends = (app) => {
        let id = 0;
        this.state.appNotExtendsArr.forEach((item) => {
            if (item.appid == app.id) {
                id = item.id 
            }
        })
        let reqData = {
            id: id,
        };
        InterfaceFn.cancelAppNotExtends(reqData, (resData) => {
           this.getAppNotExtends();
        })
    }
    //取消继承机器
    //extendsFlag在这里改变
    cancelAppExtends = (app) => {
        let username = localStorage.getItem('username');
        let reqData = {
            appid: app.id,
            k8sCluster: this.state.currCluster,
            operator: username,
        };
        InterfaceFn.addAppNotExtends(reqData, (resData) => {
            this.getAppNotExtends();
        });
    }
    //确认改变继承
    confirmChangeExtends = (app, index) => {
        if (this.state.changeFlag) {
            this.addAppExtends(app, index);
        } else {
            this.cancelAppExtends(app, index);
        }
    }
    //改变继承按钮
    handleExtendsChange = (e, app, index) => {
        let visibleFlag = [...this.state.visibleFlag];
        let changeFlag = e.target.checked;
        let tipText = e.target.checked ? '确认继承机器?' : '确认取消继承机器?';
        for (var i = 0; i < visibleFlag.length; i++)  {
            visibleFlag[i] = false;
        } ;
        visibleFlag[index] = true;
        this.setState({
            visibleFlag: visibleFlag,
            currAppId: app.id,
            changeFlag: changeFlag,
            tipText: tipText,
        });
    }

    //隐藏气泡对话框
    cancelChosePop = (index) => {
        let visibleFlag = [...this.state.visibleFlag];
        visibleFlag[index] = false;
        this.setState({
            visibleFlag: visibleFlag,
        });
    }



    componentDidMount = () => {
        this.loadProjectList();
        this.loadAppList();
        this.loadCluster();
    }

    render() {
        let currProject = this.state.currProject;
        return (
            <Layout className="m_page_Project g_layout_row">
                <div className="page_left flex_none m_block_white">
                    <div className="list_container">
                    {
                        this.state.projectList.map((p, index) => {
                            return (
                                <div
                                    className={`list_item ${currProject && currProject.id == p.id ? 'selected' : ''}`}
                                    onClick={() => this.selectProject(index)}
                                >
                                    <div class="content">
                                        <p>项目ID: {p.id}</p>
                                        <p>名称: {p.name}</p>
                                        <p>负责人：{p.owner}</p>
                                        <p>创建时间: {p.createTime}</p>
                                    </div>
                                </div>
                            )
                        })
                    }
                    </div>
                </div>
                <Layout className="page_right m_block_white">
                {
                    currProject ? <div>
                        <div className="block">
                            <p className="title">项目</p>
                            <div className="content">
                                <p>项目ID：{currProject.id}</p>
                                <p>名称：{currProject.name}</p>
                                <p>负责人：{currProject.owner}</p>
                                <p>成员：{currProject.owner}</p>
                                <p>创建时间：{currProject.createTime}</p>
                                <p>更新时间：{currProject.updateTime}</p>
                                <p>项目描述：{currProject.description}</p>
                            </div>
                        </div>
                        <div className=" g_layout_row select_cluster">
                            <span className="text">环境：</span>
                            <Select value={this.state.currCluster} style={{ width: 120 }} onChange={this.handleChangeCluster}>
                                {
                                    this.state.clusterList.map(item => {
                                        return <Option key={item.name} value={item.name}>{item.name}</Option>;
                                    })
                                }
                            </Select>
                        </div>
                        <div className="block">
                            <p className="title">机器列表</p>
                            <div className="content">
                            {
                                this.state.currCluster ?
                                    <ProjectMachine
                                        currCluster={this.state.currCluster}
                                        projectId={currProject.id}
                                    /> : null
                            }
                            </div>
                        </div>
                        <div className="block">
                            <p className="title">APP列表</p>
                            <div className="content list_container g_layout_row">
                            {
                                this.state.currAppList.map((app, index )=> {
                                    return (
                                        <div className="list_item">
                                            <div class="content">
                                                <p>应用ID：{app.id}</p>
                                                <p>名称：{app.name}</p>
                                                <p>负责人：{app.owner}</p>
                                                <p>创建时间：{app.createTime}</p>
                                            </div>
                                            <div class="control">
                                                <Popconfirm 
                                                    placement="topLeft"
                                                    title={this.state.tipText}
                                                    visible={this.state.visibleFlag[index]}
                                                    onConfirm={() => {this.confirmChangeExtends(app)}}
                                                    onCancel={() => {this.cancelChosePop(index)}}
                                                    okText="Yes"
                                                    cancelText="No"
                                                >  
                                                    <Checkbox checked={this.state.extendsFlag[index]} onChange={(e) => {this.handleExtendsChange(e, app, index)}}>继承机器</Checkbox>
                                                </Popconfirm>
                                                {!this.state.extendsFlag[index] ? <Button size="small" style={{float: 'right'}} onClick={()=>{this.handleAppModal(app.id)}}>机器列表</Button> : null}
                                            </div>   
                                        </div>
                                    )
                                })
                            }
                            {
                                this.state.appModalClose ? 
                                    <Modal
                                        title="机器列表"
                                        visible={true}
                                        onOk={this.handleAppModal}
                                        onCancel={this.handleAppModal}
                                        width={800}
                                        maskStyle={{backgroundColor:'rgba(0,0,0,0.2)'}}
                                        >
                                        <div className="block">
                                            <div className="content">
                                                <AppMachine
                                                    // clusterList={this.state.clusterList}
                                                    projectId={currProject.id}
                                                    appId={this.state.currAppId}
                                                />
                                            </div>
                                        </div>
                                    </Modal> : null
                            }
                            </div>
                        </div>
                    </div> : null
                }
                </Layout>
            </Layout>
        );
    }
}

Project.contextTypes = {
    router: PropTypes.object
}

export default Project;