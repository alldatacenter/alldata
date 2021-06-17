import React from 'react';
import PropTypes from 'prop-types';
import {Layout} from 'antd';
import { Form, Select, Button, Table, Row, Col, Input, Modal, Popconfirm } from 'antd';
const {Content} = Layout;
const Option = Select.Option;
const FormItem = Form.Item;
import DotFrom from '../components/DotForm';
import MachineForm from '../components/MachineForm';
import InterfaceFn from 'js/interface.js';

class MachineManage extends React.Component {
    constructor(props) {
        super(props);
        this.columns = [{
            title: '编号',
            dataIndex: 'id',
            width: '6%'
        }, {
            title: '机器名称',
            dataIndex: 'name',
            width: '16%'
        }, {
            title: '状态',
            dataIndex: 'status',
            width: '16%'
        }, {
            title: '机器组别',
            dataIndex: 'account',
            width: '15%'
        },{
            title: '机器信息',
            dataIndex: 'info',
            width: '15%'
        }, {
            title: '环境',
            dataIndex: 'k8sCluster',
            width: '16%'
        },{
            title: '操作',
            width: '16%',
            render: (text, record, index) => {
                return (
                    <span>
                        {/* <Button size="small" type="primary" style={{margin: '2px'}} onClick={() => {this.toggleModalDotForm()}}>修改</Button> */}
                        <Popconfirm title="确定删除该配置吗?" onConfirm={() => {this.handleDeleteMachine(record.id)}} okText="确定" cancelText="取消">
                            <Button size="small" type="danger" style={{margin: '2px'}}>删除</Button>
                        </Popconfirm>    
                    </span>
                );
            },
        }];
        this.state = {
            data: [], //机器列表数据
            tableHeight: 600,   // 表格初始高度
            machineMatchData: [],//查询机器的匹配数据
            flag: false,
            modalDotClose: true,
            modalMachinFormClose: true,
        };
    }

    //获取machine列表
    //拉取机器列表
    getAllMachine = () => {
        InterfaceFn.getAllMachine({
            where: `k8s_name != ''`,
            order: 'id',
            offset: 0,
            limit: 1000,
       }, (resData) => {
           this.setState({
               data: resData,
           })
       });
    }

    // 查询
    handleCheckSubmit = (e) => {
        e.preventDefault();
        this.props.form.validateFields((err, values) => {
            if (!err) {
                console.log(values);
                let checkInfo = {
                    group: values.group || '',
                    name: values.name || '',
                };
                let nameReg = new RegExp(checkInfo.name);
                let groupReg = new RegExp(checkInfo.group);
                let matchData = this.state.data.filter((item) => {
                    return (nameReg.test(item.name) || checkInfo.name == '') &&
                            (groupReg.test(item.account) || checkInfo.group =='')
                });
                this.setState({
                    machineMatchData: matchData,
                    flag: true,
                });
            }
        });
    }
    //修改机器,是否显示dotForm
    toggleModalDotForm = () => {
        this.setState({
            modalDotClose: !this.state.modalDotClose,
        });
        //过滤出该机器污点列表
    }
    //删除机器
    handleDeleteMachine = (id) => {
        let reqData = {
            id: id,
        };
        InterfaceFn.deleteMachine(reqData, (resData) => {
            this.getAllMachine();
        })

    }
    //添加机器,显示machineForm
    toggleModalMachineForm = () => {
        this.setState({
            modalMachinFormClose: !this.state.modalMachinFormClose,
        });
    }

    // 自适应表格高度
    getTableScrollHeight = () => {
        return getComputedStyle(document.getElementById('mainbody')).height.split('px')[0] - 200;
    }

    componentDidMount() {
        this.getAllMachine();
        this.setState({
            tableHeight: this.getTableScrollHeight(),
        });
    }

    render() {
        const { getFieldDecorator } = this.props.form;
        const columns = this.columns;
        let dataSource = this.state.flag ? this.state.machineMatchData : this.state.data;
        return (
            <Layout className="m_page_MachineManage m_block_white">
                <Form
                    id="contentToolbar"
                    onSubmit={this.handleCheckSubmit}
                >
                    <Row>
                        <Col span={5}>
                            <FormItem
                                wrapperCol={{ span: 24 }}
                                style={{ marginRight: '20px' }}
                            >
                                {getFieldDecorator('group', {
                                    initialValue: '',
                                })(
                                    <Input
                                        addonBefore="机器组"
                                    />
                                )}
                            </FormItem>
                        </Col>
                        <Col span={5}>
                            <FormItem
                                wrapperCol={{ span: 24 }}
                                style={{ marginRight: '20px' }}
                            >
                                {getFieldDecorator('name', {
                                    initialValue: '',
                                })(
                                    <Input
                                        addonBefore="名称"
                                    />
                                )}
                            </FormItem>
                        </Col>
                        <Button
                            htmlType="submit"
                            type="primary"
                            style={{ marginRight: '20px' }}
                        >
                            查询
                        </Button>
                        <Button
                            type="primary"
                            style={{ marginRight: '20px' }}
                            onClick={() => {this.toggleModalMachineForm()}}
                        >
                            添加机器
                        </Button>
                    </Row>
                </Form>
                <Table
                    size={'small'}
                    bordered={true}
                    pagination={{
                        pageSize: 25
                    }}
                    dataSource={dataSource}
                    columns={columns}
                    scroll={{ x: '800px', y: this.state.tableHeight + 'px' }}
                />
                {
                    this.state.modalDotClose ? null : 
                        <Modal
                            title="修改机器"
                            visible={true}
                            // maskCloseable = {false}
                            maskStyle={{backgroundColor:'rgba(0,0,0,0.2)'}}
                            footer = {null}
                            width = {800}
                            onCancel = {this.toggleModalDotForm}
                        >
                            <DotFrom
                                toggleModalDotForm={this.toggleModalDotForm}
                            />
                        </Modal>
                }
                {
                    this.state.modalMachinFormClose ? null : 
                        <Modal
                            title="添加机器"
                            visible={true}
                            // maskCloseable = {false}
                            maskStyle={{backgroundColor:'rgba(0,0,0,0.2)'}}
                            footer = {null}
                            width = {800}
                            onCancel = {this.toggleModalMachineForm}
                            >
                            <MachineForm
                                toggleModalMachineForm={this.toggleModalMachineForm}
                                getAllMachine={this.getAllMachine}
                            />
                        </Modal>    
                }
                
            </Layout>
        );
    }
}

MachineManage = Form.create()(MachineManage);

export default MachineManage;