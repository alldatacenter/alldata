import React from 'react';
import PropTypes from 'prop-types';
import {Layout} from 'antd';
import { Select, Button } from 'antd';
const {Content} = Layout;
const Option = Select.Option;
import InterfaceFn from 'js/interface.js';

// 项目--机器列表
class AppMachine extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            currCluster: '', // 当前环境
            clusterList:[],
            projectId: this.props.projectId,
            appId: this.props.appId,
            operatorFlag: true,
            totalMachineArr: [],
            projectMachineArr:[],
            projectApplyMachineArr:[],
            appMachineArr: [],
            appApplyMachinArr: [],
        };
    }
    // 获取App appcluster
    getAppClusterList = () => {
        let reqData = {
            where: `appid = ${this.state.appId}`,
            offset: 0,
            limit: 1000,
        };
        InterfaceFn.getAppcluster(reqData, (resData) => {
            if (resData.length > 0) {
                this.state.currCluster = resData[0].clusterName;
                this.setState({
                    clusterList: resData,
                    currCluster: resData[0].clusterName,
                })
                this.getAllMachine();
                this.getProjectMachine();
                this.getAppMachine();
            }
        });
    }
    // 切换 currCluster
    handleChangeCluster = (value) => {
        this.state.currCluster = value;
        this.setState({
            currCluster: value,
        });
        // 重新拉取机器列表
        this.getAllMachine();
        this.getProjectMachine();
        this.getAppMachine();
    }

    //是否显示可添加列表
    handleChangeoperatorFlag = () => {
        this.setState({
            operatorFlag: !this.state.operatorFlag,
        })
    }

    //移除项目机器
    handleDeleteProjectMachine = (id) => {
        let arr = this.state.projectMachineArr.filter(item => item.machine_id == id);
        let reqData = {
            id: arr[0].id,
        };
        InterfaceFn.deleteProjectMachine(reqData, (resData) => {
            this.getProjectMachine();
        })
    }
    //添加项目机器
    handleAddProjectMachine = (machine) => {
        let username = localStorage.getItem('username');
        let reqData = {
            projectId: this.state.projectId,
            k8sCluster: this.state.currCluster,
            machine_id: `${machine.id}`,
            operator: username,
        };
        InterfaceFn.addProjectMachine(reqData, (resData) => {
            this.getProjectMachine();
        })    
    }
    
    //获取项目机器列表
    getProjectMachine = () => {
       InterfaceFn.getProjectMachine({
            where: `k8s_cluster="${this.state.currCluster}" and project_id=${this.state.projectId}`,
            offset: 0,
            limit: 1000,
        }, (resData) => {
            this.setState({
                projectMachineArr: resData,

            })
        })
    }

    //拉取全部机器列表
    getAllMachine = () => {
        InterfaceFn.getAllMachine({
            where: `k8s_cluster="${this.state.currCluster}"`,
            offset: 0,
            limit: 1000,
       }, (resData) => {
           this.setState({
               totalMachineArr: resData,
           })
       });
    }

    //拉取APP机器列表
    getAppMachine = () => {
        InterfaceFn.getAppMachine({
                where: `k8s_cluster="${this.state.currCluster}" and appId=${this.state.appId}`,
                offset: 0,
                limit: 1000,
            }, (resData) => {
                let projectApplyMachineArr = this.state.totalMachineArr.filter((item) => {
                    let flag = false;
                    resData.forEach((pm) => {
                        if (pm.machine_id == item.id) {
                            flag = true;
                        }
                    });
                    return flag ;
                });
                this.setState({
                    appMachineArr: resData,
                    projectApplyMachineArr: projectApplyMachineArr,
                })
            })
    }
    //添加APP机器列表
    handleAddAppMachine = (machine) => {
        let username = localStorage.getItem('username');
        let reqData = {
            appid: this.state.appId,
            k8sCluster: this.state.currCluster,
            machine_id: `${machine.id}`,
            operator: machine.operator,
            operator: username, 
        };
        InterfaceFn.addAppMachine(reqData, (resData) => {
            this.getAppMachine();
        })    
    }

    //移除App机器
    handleDeleteAppMachine = (id) => {
        let arr = this.state.apptMachineArr.filter(item => item.machine_id == id);
        let reqData = {
            id: arr[0].id,
        };
        InterfaceFn.deleteAppMachine(reqData, (resData) => {
            this.getAppMachine();
        })
    }

    componentDidMount = () => {
       // 拉取机器列表
       this.getAppClusterList();
    }

    render() {
        let projectMachineArr = this.state.projectMachineArr;
        let appMachineArr = this.state.appMachineArr;
        let projectApplyMachineArr = this.state.totalMachineArr.filter((item) => {
            let flag1 = false;
            projectMachineArr.forEach((pm) => {
                if (pm.machine_id == item.id) {
                    flag1 = true;
                }
            });
            return flag1 ;
        });
        let appApplyMachineArr = projectApplyMachineArr.filter((item) => {
            let flag2 = false;
            appMachineArr.forEach((am) => {
                if (am.machine_id == item.id) {
                    flag2 = true;
                }
            });
            return flag2 ;
        });
        return (
            <div>
                <div className=" g_layout_row select_cluster">
                    <span className="text">环境：</span>
                    <Select value={this.state.currCluster} style={{ width: 120 }} onChange={this.handleChangeCluster}>
                        {
                            this.state.clusterList.map(item => {
                                return <Option key={item.clusterName} value={item.clusterName}>{item.clusterName}</Option>;
                            })
                        }
                    </Select>
                </div>
                {/* 已经配置的机器列表 */}
                {
                    this.state.currCluster ? 
                    <div className="table_block">
                        <div className="table_row">
                            <span className="form_item form_item_title">机器名称</span>
                            <span className="form_item form_item_title">机器信息</span>
                            <span className="form_item form_item_title">状态</span>
                            {
                                this.state.operatorFlag ? <span className="form_item form_item_title" style={{width:'80px'}}>操作</span> : null
                            }
                        </div>
                            {   
                                    appApplyMachineArr.map(item =>{
                                    return (
                                        <div className="table_row">
                                            <span className="form_item">{item.name}</span>
                                            {
                                                item.info == '' ? <span className="form_item">无</span> : <span className="form_item">{item.info}</span>
                                            }
                                            <span className="form_item">{item.status}</span>
                                            <span className="form_item" style={{width:'80px'}}>
                                                <Button type="danger" size="small" onClick={() => {this.handleDeleteProjectMachine(item.id)}}>移除</Button>
                                            </span>    
                                        </div>
                                    )
                                })
                            }
                    </div> : null
                }
                {/* 可添加的机器列表 */}
                {
                    this.state.operatorFlag ? 
                        <div>
                            <p>可添加的机器列表</p>
                            <div className="table_block">
                                <div className="table_row">
                                    <span className="form_item form_item_title">机器名称</span>
                                    <span className="form_item form_item_title">机器信息</span>
                                    <span className="form_item form_item_title">状态</span>
                                    {
                                        this.state.operatorFlag ? <span className="form_item form_item_title" style={{width:'80px'}}>操作</span> : null
                                    }
                                </div>
                                    { 
                                        projectApplyMachineArr.filter((item) => appMachineArr.every(am => am.machine_id != item.id)).map((item => {
                                            return (
                                                <div className="table_row">
                                                    <span className="form_item">{item.name}</span>
                                                    {
                                                        item.info == '' ? <span className="form_item">无</span> : <span className="form_item">{item.info}</span>
                                                    }
                                                    <span className="form_item">{item.status}</span>
                                                    <span className="form_item" style={{width:'80px'}}>
                                                        <Button type="primary" size="small" onClick={() => {this.handleAddAppMachine(item)}}>添加</Button>
                                                    </span>   
                                                </div>
                                            )
                                        }))
                                    }
                            </div>
                        </div> : null
                }
                
                

            </div>
        );
    }
}

export default AppMachine;