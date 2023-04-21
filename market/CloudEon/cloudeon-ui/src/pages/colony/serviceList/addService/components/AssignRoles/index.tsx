
import { ProCard } from '@ant-design/pro-components';
import { Tree, Table, notification, Alert, Modal, Spin } from 'antd';
import type { DataNode, TreeProps } from 'antd/es/tree';
import styles from './index.less'
import { useState, useEffect, useRef } from 'react';
import { getNodeListAPI } from '@/services/ant-design-pro/colony';
import {cloneDeep} from 'lodash'

const AssignRoles : React.FC<{
    serviceList: any[]; // 原本选中的服务的树结构
    sourceServiceInfos: API.ServiceInfosItem[], // 原本选中的服务的树结构+主机名(包括上一次选中的)
    setServiceInfosToParams: any,
    checkAllRolesRules:any,
    parentLoading: boolean
}> = ({serviceList, sourceServiceInfos, setServiceInfosToParams, checkAllRolesRules, parentLoading})=>{

    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]); // 选中的主机名
    const [nodeListData, setNodeListData] = useState<any[]>();
    const [nodeJsonData, setNodeJsonData] = useState({}); // JSON格式的{id:hostname}
    const [currentSelectRoles, setCurrentSelectRoles] = useState<selectRoleItem>();
    const [loading, setLoading] = useState(false);
    const [serviceInfos, setServiceInfos] = useState<API.ServiceInfosItem[]>();
    const [isModalOpen, setIsModalOpen] = useState(false);
    
    const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')

    type NotificationType = 'success' | 'info' | 'warning' | 'error';

    type selectRoleItem = {
        validRule: any;
        stackServiceId: number,
        stackRoleName: any,
        nodeIds: any[]
    }

    // type nodeJsonItem = {}

    const getNodeData = async (params: any) => {
        setLoading(true)
        const result: API.NodeList =  await getNodeListAPI(params);
        setLoading(false)
        if(result?.data){
            setNodeListData(result?.data)
            let nodeJson = {}
            for(let i = 0; i < result.data.length; i++){
                let item = result.data[i]
                item.id && (nodeJson[item.id] = item.hostname)
            }
            console.log('--nodeJson: ',nodeJson);
            
            setNodeJsonData(nodeJson)
        }
      };

    const initServiceInfos = () => {
        if(!sourceServiceInfos) return
        setServiceInfos(sourceServiceInfos)
        setServiceInfosToParams(sourceServiceInfos)
        // const roleData = {
        //     stackServiceId: sourceServiceInfos[0].stackServiceId || 0,
        //     stackRoleName: sourceServiceInfos[0] && sourceServiceInfos[0].roles && sourceServiceInfos[0].roles[0].stackRoleName || '',
        //     nodeIds: sourceServiceInfos[0] && sourceServiceInfos[0].roles && sourceServiceInfos[0].roles[0].nodeIds || [],
        //     validRule: sourceServiceInfos[0] && sourceServiceInfos[0].roles && sourceServiceInfos[0].roles[0].validRule || {},
        // }
        // setCurrentSelectRoles(roleData)
        // const newSelectedRowKeys = getNodeIds(sourceServiceInfos[0].stackServiceId, serviceList[0].roles[0])
        // setSelectedRowKeys(newSelectedRowKeys);
    }

    // const treeData = serviceList.map(item=>{
    //     let itemData = {
    //         title: item.label,
    //         key: item.id,
    //         selectable: false,
    //         children: item.roles? (item.roles.map((r: any)=>{
    //             return {
    //                 title: r,
    //                 key: r,
    //                 stackServiceId: item.id
    //             }
    //         })):[]
    //     }
    //     return itemData
    // })

    const getNodeIds:any = (stackServiceId:number, stackRoleName:any) => {
        for (let sItem of (serviceInfos||sourceServiceInfos)) {
            if (sItem.stackServiceId == stackServiceId) {
                for (let role of sItem.roles||[]) {
                    if(role.stackRoleName == stackRoleName)
                    return role.nodeIds||[]
                }
            }
        }      
    }

    const getRoleRule:any = (stackServiceId:number, stackRoleName:any) => {
        for (let sItem of (serviceInfos||sourceServiceInfos)) {
            if (sItem.stackServiceId == stackServiceId) {
                for (let role of sItem.roles||[]) {
                    if(role.stackRoleName == stackRoleName)
                    return role.validRule||{}
                }
            }
        }      
    }

    // const onSelectTree: TreeProps['onSelect'] = (selectedKeys, info) => {
    //     console.log('selected', selectedKeys, info);
    //     const newSelectedRowKeys = getNodeIds(info.selectedNodes[0].stackServiceId, selectedKeys[0])
    //     setSelectedRowKeys(newSelectedRowKeys);
    //     const roleData = {
    //         stackServiceId: info.selectedNodes[0].stackServiceId,
    //         stackRoleName: selectedKeys[0],
    //         nodeIds: newSelectedRowKeys,
    //         validRule: getRoleRule(info.selectedNodes[0].stackServiceId, selectedKeys[0]),
    //     }
    //     setCurrentSelectRoles(roleData)
    //     // console.log('--currentSelectRoles:', currentSelectRoles);
    // };

    const columns = [
        {
            title: '主机名',
            dataIndex: 'hostname',
            key: 'hostname',
        },
        {
            title: 'ip地址',
            dataIndex: 'ip',
            key: 'ip',
        },
        {
            title: '总cpu',
            dataIndex: 'coreNum',
            key: 'coreNum',
        },
        {
            title: '总内存',
            dataIndex: 'totalMem',
            key: 'totalMem',
        },
        {
            title: '总硬盘',
            dataIndex: 'totalDisk',
            key: 'totalDisk',
        },
    ];

    const [api, contextHolder] = notification.useNotification();

    // const openNotificationWithIcon = (errList:string[]) =>{
    //     if(!errList || errList.length == 0) return
    //     const errDom = errList.map(item=>{
    //         return (<Alert type="error" message={item} banner />)
    //     })
    //     notification.open({
    //         message: '温馨提示',
    //         description:<>{errDom}</>,
    //         duration:null,
    //         style: {
    //             width: 500
    //         }
    //         // onClick: () => {
    //         //   console.log('Notification Clicked!');
    //         // },
    //       });
    // }

    

    const onSelectChange = (newSelectedRowKeys: number[]) => { // 主机名勾选触发的函数
        console.log('---onSelectChange:', newSelectedRowKeys, currentSelectRoles);
        setSelectedRowKeys(newSelectedRowKeys);
        // let roles = {...currentSelectRoles}
        // const newServiceInfos = [...(serviceInfos||[])]
        // roles.nodeIds = newSelectedRowKeys
        // for (let sItem of newServiceInfos||[]) {
        //     if (sItem.stackServiceId == roles.stackServiceId) {
        //         for (let role of sItem.roles||[]) {
        //             if(role.stackRoleName == roles.stackRoleName)
        //             role.nodeIds = newSelectedRowKeys
        //         }
        //     }
        // } 
        // setServiceInfos(newServiceInfos)
        // checkAllRolesRules(newServiceInfos)
        // setServiceInfosToParams(newServiceInfos)
    };

    const rowSelection = {
        selectedRowKeys,
        onChange: onSelectChange,
        getCheckboxProps: (record: { name: string; }) => ({
            disabled: !(currentSelectRoles && currentSelectRoles.stackRoleName), 
          }),
      };

    useEffect(() => {
        initServiceInfos()
        getNodeData({ clusterId: getData.clusterId })
        console.log('serviceList: ', serviceList);
        console.log('sourceServiceInfos: ', sourceServiceInfos);        
    }, []);

    const handleRoleBox = (stackServiceId: any, stackRoleName: any) => {
        const newSelectedRowKeys = getNodeIds(stackServiceId,stackRoleName)
        setSelectedRowKeys(newSelectedRowKeys);
        const roleData = {
            stackServiceId: stackServiceId,
            stackRoleName: stackRoleName,
            nodeIds: newSelectedRowKeys,
            validRule: getRoleRule(stackServiceId, stackRoleName),
        }
        setCurrentSelectRoles(roleData)
        setIsModalOpen(true)
    }

    const handleOk = () => {
        const newServiceInfos = cloneDeep(serviceInfos||[])
        let roles = cloneDeep(currentSelectRoles)
        if(roles){
            roles.nodeIds = cloneDeep(selectedRowKeys)
            for (let sItem of newServiceInfos||[]) {
                if (sItem.stackServiceId == roles.stackServiceId) {
                    for (let role of sItem.roles||[]) {
                        if(role.stackRoleName == roles.stackRoleName)
                        role.nodeIds = cloneDeep(selectedRowKeys)
                    }
                }
            } 
        }
        if(checkAllRolesRules(newServiceInfos)){
            setServiceInfos(newServiceInfos)
            setServiceInfosToParams(newServiceInfos)
            setIsModalOpen(false);
        }
        
    }
    const handleCancel = () => {
        checkAllRolesRules(serviceInfos)
        setIsModalOpen(false);
    };

    return (
        
        
        <div className={styles.assignRolesLayout}>
            <Spin tip="Loading" size="small" spinning={loading || parentLoading}>
            <div style={{padding:'10px 0'}}>请点击角色选择节点。</div>
            <div >
                {
                    serviceInfos?.map(serItem=>{
                        return (
                            <div className={styles.serviceRow} key={serItem.stackServiceId}>
                                <div className={styles.serviceName}>{serItem.stackServiceName}</div>
                                <div className={styles.roleWrap}>
                                    
                                    {
                                        serItem?.roles?.map(roleItem=>{
                                            return (
                                                <div className={styles.roleBox} key={roleItem.stackRoleName} onClick={()=>handleRoleBox(serItem.stackServiceId,roleItem.stackRoleName)}>
                                                    <div className={styles.roleName}>{roleItem.stackRoleName}</div>
                                                    <div className={styles.roleNodeListBox}>
                                                    {roleItem.nodeIds?.map(nodeItem=>{
                                                        return (
                                                            <div className={styles.nodeItemBox} key={nodeItem}>{nodeJsonData[nodeItem]}</div>
                                                        )
                                                    })}
                                                    </div>

                                                </div>
                                            )
                                        })
                                    }
                                </div>
                            </div>
                        )
                    })
                }

            </div>
            <Modal
                key="selectNodes"
                width={800}
                title={`请选择${currentSelectRoles?.stackRoleName}的节点`}
                forceRender={true}
                destroyOnClose={true}
                open={isModalOpen}
                onOk={handleOk}
                confirmLoading={loading}
                onCancel={handleCancel}
                // footer={null}
            >
                <Table 
                    rowKey="id"
                    pagination={false}
                    loading={loading}
                    rowSelection={rowSelection} 
                    columns={columns} 
                    dataSource={nodeListData} 
                />
            </Modal>
            </Spin>
            {/* <ProCard split="vertical" bordered>
                <ProCard title="服务" colSpan="30%" headerBordered className={styles.rolesLeft}>
                <Tree
                    defaultExpandAll
                    blockNode
                    defaultSelectedKeys={[serviceList[0].roles[0]]}
                    onSelect={onSelectTree}
                    treeData={treeData}
                    style={{background: '#fbfbfe'}}
                />
                </ProCard>
                <ProCard title="主机名分配" headerBordered className={styles.nodeWrap}>
                {contextHolder}
                    <Table 
                        rowKey="id"
                        loading={loading}
                        rowSelection={rowSelection} 
                        columns={columns} 
                        dataSource={nodeListData} 
                    />
                </ProCard>
            </ProCard> */}
        </div>
    )
}

export default AssignRoles