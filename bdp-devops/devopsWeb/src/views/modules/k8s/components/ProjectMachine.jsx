import React from 'react';
import PropTypes from 'prop-types';
import {Layout} from 'antd';
import { Form, Select, Button, Table, Row, Col, Input, Modal, Popconfirm } from 'antd';
const {Content} = Layout;
const Option = Select.Option;
import InterfaceFn from 'js/interface.js';

// 项目--机器列表
class ProjectMachine extends React.Component {
    constructor(props) {
        super(props);
        let columns = [{
            title: '机器名称',
            dataIndex: 'name',
            width: '30%'
        }, {
            title: '状态',
            dataIndex: 'status',
            width: '20%'
        }, {
            title: '机器信息',
            dataIndex: 'info',
            width: '30%'
        }];
        this.projectOperateColumn = {
            title: '操作',
            width: '20%',
            render: (text, record, index) => {
                return (
                    <span>
                        <Popconfirm title="确定移除该机器吗?" onConfirm={() => {this.handleDeleteProjectMachine(record.id)}} okText="确定" cancelText="取消">
                            <Button type="danger" size="small" type="danger" style={{margin: '2px'}}>移除</Button>
                        </Popconfirm>    
                    </span>
                );
            }
        };
        this.machineOperateColumn = [{
            title: '操作',
            width: '20%',
            render: (text, record, index) => {
                return (
                    <span>
                        <Popconfirm title="确定添加该机器吗?" onConfirm={() => {this.handleAddProjectMachine(record)}} okText="确定" cancelText="取消">
                            <Button size="small" type="primary" style={{margin: '2px'}}>添加</Button>
                        </Popconfirm>    
                    </span>
                );
            }
        }];
        let machineColumns = columns;
        this.projectColumns = columns;
        this.machineColumns = machineColumns.concat(this.machineOperateColumn);
        this.state = {
            currCluster: this.props.currCluster, // 当前环境
            projectId: this.props.projectId,
            projectFlag: false,
            totalMachineArr: [],
            projectMachineArr: [],
            btnText: '修改机器配置',
        };
    }

    

    //是否显示可添加列表
    handleChangeProjectFlag = () => {
        let btnText = '';
        if (!this.state.projectFlag) {
            btnText = '退出修改配置';
        } else {
            btnText = '修改机器配置';
        };
        this.setState({
            projectFlag: !this.state.projectFlag,
            btnText: btnText,
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
        InterfaceFn.addProjectmachine(reqData, (resData) => {
            this.getProjectMachine();
        })
        
    }

    getProjectMachine = () => {
        //获取项目机器列表
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

    //拉取机器列表
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

    // 自适应表格高度
    getTableScrollHeight = () => {
        return getComputedStyle(document.getElementById('mainbody')).height.split('px')[0] - 200;
    }

    componentDidMount = () => {
        // 拉取机器列表
        this.getAllMachine();
        this.getProjectMachine();
        this.getTableScrollHeight();
    }

    render() {
        let projectMachineArr = this.state.projectMachineArr;
        let projectApplyMachineArr = this.state.totalMachineArr.filter((item) => {
            let flag = false;
            projectMachineArr.forEach((pm) => {
                if (pm.machine_id == item.id) {
                    flag = true;
                }
            });
            return flag ;
        });
        let addableMachineArr = this.state.totalMachineArr.filter((item) => projectMachineArr.every(pm => pm.machine_id != item.id));
        console.log(projectApplyMachineArr);
        let projectColumns = this.projectColumns;
        if (this.state.projectFlag) {
            projectColumns = projectColumns.concat(this.projectOperateColumn);
        }; 
        return (
            <div>
                <div className=" g_layout_row select_cluster">
                    <Button type="primary" style={{marginLeft: "5px"}} onClick={() => {this.handleChangeProjectFlag()}}>{this.state.btnText}</Button>
                </div>
                {/* 已经配置的机器列表 */}
                <Table
                    size={'small'}
                    bordered={true}
                    pagination={{
                        pageSize: 25
                    }}
                    dataSource={projectApplyMachineArr}
                    columns={projectColumns}
                    scroll={{ x: '800px', y: this.state.tableHeight + 'px' }}
                />
                {/* 可添加的机器列表 */}
                {
                    this.state.projectFlag ? 
                        <div>
                            <p>可添加的机器列表</p>
                            <Table
                                size={'small'}
                                bordered={true}
                                pagination={{
                                    pageSize: 25
                                }}
                                dataSource={addableMachineArr}
                                columns={this.machineColumns}
                                scroll={{ x: '800px', y: this.state.tableHeight + 'px' }}
                            />
                        </div> : null
                }
            </div>
        );
    }
}

export default ProjectMachine;