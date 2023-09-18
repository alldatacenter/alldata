import { arrayMoveImmutable, PageContainer, ProCard, ProTable } from '@ant-design/pro-components';
import { Space, Card, Table, Tag, Button, Modal, Form, Input, message, Select, Spin } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { FormInstance } from 'antd/es/form';
import { FormattedMessage, useIntl, history } from 'umi';
import { getActiveAlertAPI, getHistoryAlertAPI, getRulesAlertAPI, saveRulesAlertAPI,getStackServiceRolesAPI } from '@/services/ant-design-pro/colony';
import { formatDate } from '@/utils/common'
import styles from './index.less'
// import { RightOutlined } from '@ant-design/icons';
// import { statusColor } from '../../../utils/colonyColor'

const { TextArea } = Input;

const alertList: React.FC = () => {
  const intl = useIntl();
  const [alertLevelOptions, setAlertLevelOptions] = useState<any[]>();
  const [serviceOptions, setServiceOptions] = useState<any[]>();
  const [roleOptions, setRoleOptions] = useState<any[]>();
  const [hostnameOptions, setHostnameOptions] = useState<any[]>();
  const [activeListData, setActiveListData] = useState<any[]>();
  const [historyListData, setHistoryListData] = useState<any[]>();
  const [rulesListData, setRulesListData] = useState<any[]>();
  const [loading, setLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [stackServiceRoles, setStackServiceRoles] = useState<any>();
  const [serviceList, setServiceList] = useState<any>();  
  const [roleList, setRoleList] = useState<any[]>();  
  const [currentRow, setCurrentRow] = useState<API.alertRulesItem>();
  const [roleName, setRoleName] = useState<any>();  
  const [serviceName, setServiceName] = useState<any>();  
  
  
  const [ruleForm] = Form.useForm();  
  const [currentTab, setCurrentTab] = useState('activeAlert');
  const [form] = Form.useForm();
  
  // formRef = React.createRef<FormInstance>();
  // const form = useRef();
  const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')

  const getActiveData = async () => {
    setLoading(true)
    const result: API.alertListResult =  await getActiveAlertAPI({clusterId:getData.clusterId});
    if(result?.data){
      let levelOptions:any[] = []
      let serviceOptions:any[] = []
      let roleOptions:any[] = []
      let hostnameOptions:any[] = []
      // 处理表头过滤问题
      for(let i = 0; i < result.data.length; i++){
        let item = result.data[i]
        const { alertLevelMsg, serviceInstanceName, serviceRoleLabel, hostname } = item
        if(levelOptions.indexOf(alertLevelMsg) == -1) { levelOptions.push(alertLevelMsg||'') }
        if(serviceOptions.indexOf(serviceInstanceName) == -1) { serviceOptions.push(serviceInstanceName||'') }
        if(roleOptions.indexOf(serviceRoleLabel) == -1) { roleOptions.push(serviceRoleLabel||'') }
        if(hostnameOptions.indexOf(hostname) == -1) { hostnameOptions.push(hostname||'') }
      }
      setAlertLevelOptions(levelOptions.map(item=>{return {text:item, value: item}}))
      setServiceOptions(serviceOptions.map(item=>{return {text:item, value: item}}))
      setRoleOptions(roleOptions.map(item=>{return {text:item, value: item}}))
      setHostnameOptions(hostnameOptions.map(item=>{return {text:item, value: item}}))
    }
    setLoading(false)
    setActiveListData(result?.data)
  };

  const getHistoryData = async () => {
    setLoading(true)
    const result: API.alertListResult =  await getHistoryAlertAPI({clusterId:getData.clusterId});
    // if(result?.data){
    //   let levelOptions:any[] = []
    //   let serviceOptions:any[] = []
    //   let roleOptions:any[] = []
    //   let hostnameOptions:any[] = []
    //   // 处理表头过滤问题
    //   for(let i = 0; i < result.data.length; i++){
    //     let item = result.data[i]
    //     const { alertLevelMsg, serviceInstanceName, serviceRoleLabel, hostname } = item
    //     if(levelOptions.indexOf(alertLevelMsg) == -1) { levelOptions.push(alertLevelMsg||'') }
    //     if(serviceOptions.indexOf(serviceInstanceName) == -1) { serviceOptions.push(serviceInstanceName||'') }
    //     if(roleOptions.indexOf(serviceRoleLabel) == -1) { roleOptions.push(serviceRoleLabel||'') }
    //     if(hostnameOptions.indexOf(hostname) == -1) { hostnameOptions.push(hostname||'') }
    //   }
    //   setAlertLevelOptions(levelOptions.map(item=>{return {text:item, value: item}}))
    //   setServiceOptions(serviceOptions.map(item=>{return {text:item, value: item}}))
    //   setRoleOptions(roleOptions.map(item=>{return {text:item, value: item}}))
    //   setHostnameOptions(hostnameOptions.map(item=>{return {text:item, value: item}}))
    // }
    setLoading(false)
    setHistoryListData(result?.data)
  };

  const getRulesData = async () => {
    setLoading(true)
    const result: API.alertListResult =  await getRulesAlertAPI({clusterId:getData.clusterId});
    setLoading(false)
    setRulesListData(result?.data)
  }

  const getStackService = async () => {
    setLoading(true)
    const result: API.normalResult =  await getStackServiceRolesAPI({stackId: getData.stackId})
    setLoading(false)
    setStackServiceRoles(result?.data)
    let services = []
    for(let key in result?.data){
      services.push(key)
    }
    setServiceList(services) 
    if(result?.data && services){
      setRoleList(result.data[services[0]])
      setRoleName(result.data[services[0]][0])
      ruleForm.setFieldValue('stackRoleName', result.data[services[0]][0])
    }
  }



  useEffect(() => {
    getActiveData()
    getStackService()
  }, []);




  const activeColumns = [
    {
      title: '告警名称',
      dataIndex: 'alertName',
      key: 'alertName',
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertName}</div>
        )
      },
    },
    {
      title: '告警级别',
      dataIndex: 'alertLevelMsg',
      key: 'alertLevelMsg',
      filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertLevelMsg}</div>
        )
      }
    },
    {
      title: '开始时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (_: any, record:API.alertItem) => {
        return (
          <div>{formatDate(record.createTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '服务名',
      dataIndex: 'serviceInstanceName',
      key: 'serviceInstanceName',
      filters: serviceOptions,
      onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.serviceInstanceName}</div>
        )
      },
    },
    {
      title: '角色名',
      dataIndex: 'serviceRoleLabel',
      key: 'serviceRoleLabel',
      filters: roleOptions,
      onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.serviceRoleLabel}</div>
        )
      },
    },
    {
      title: '节点',
      dataIndex: 'hostname',
      key: 'hostname',
      filters: hostnameOptions,
      onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.hostname}</div>
        )
      },
    },
    {
      title: '告警内容',
      dataIndex: 'info',
      key: 'info',
    },
    {
      title: '建议',
      dataIndex: 'advice',
      key: 'advice',
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{maxWidth:'500px',display:'flex',flexWrap:'wrap'}}>{record.advice}</div>
        )
      },
    }
  ]


  

  const historyColumns = [
    {
      title: '告警名称',
      dataIndex: 'alertName',
      key: 'alertName',
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertName}</div>
        )
      },
    },
    {
      title: '告警级别',
      dataIndex: 'alertLevelMsg',
      key: 'alertLevelMsg',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertLevelMsg}</div>
        )
      }
    },
    {
      title: '开始时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (_: any, record:API.alertItem) => {
        return (
          <div>{formatDate(record.createTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '结束时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (_: any, record:API.alertItem) => {
        return (
          <div>{formatDate(record.createTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '服务名',
      dataIndex: 'serviceInstanceName',
      key: 'serviceInstanceName',
      // filters: serviceOptions,
      // onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.serviceInstanceName}</div>
        )
      },
    },
    {
      title: '角色名',
      dataIndex: 'serviceRoleLabel',
      key: 'serviceRoleLabel',
      // filters: roleOptions,
      // onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.serviceRoleLabel}</div>
        )
      },
    },
    {
      title: '节点',
      dataIndex: 'hostname',
      key: 'hostname',
      // filters: hostnameOptions,
      // onFilter: true,
      render: (_: any, record:API.alertItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.hostname}</div>
        )
      },
    }
  ]


  const showModal = (row: any | null, type: string) => {
    ruleForm.resetFields()
    if(type=='edit' && row){
      setIsEdit(true)
      setCurrentRow(row)
      ruleForm.setFieldsValue(row)
      setRoleList(stackServiceRoles[row.stackServiceName])
    }else{
      setIsEdit(false)
      setCurrentRow({})
      setRoleList(stackServiceRoles[serviceList[0]])
      setRoleName(stackServiceRoles[serviceList[0]][0])
      ruleForm.setFieldsValue({alertLevel:'告警级别', stackRoleName: stackServiceRoles[serviceList[0]][0]})
    }
    setIsModalOpen(true)
    
  }

  const handleOk = () => {
    ruleForm
      .validateFields()
      .then(async (values) => {
        console.log('values: ', values);  
        let params = {
          ...values
        }
        isEdit && (params.id = currentRow?.id)
        setSaveLoading(true)
        const result: API.normalResult = await saveRulesAlertAPI({...params, clusterId: getData.clusterId})
        if(result && result.success){
          message.success(isEdit?'修改成功':'新增成功');
          getRulesData();
          setIsModalOpen(false);
          ruleForm.resetFields()
        }else{
          message.error(result.message);
        }
        setSaveLoading(false)
      })
      .catch((err) => {
        console.log('err: ', err);
      });
  }

  const rulesColumns = [
    {
      title: '规则名称',
      dataIndex: 'ruleName',
      key: 'ruleName',
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div >{record.ruleName}</div>
        )
      },
    },
    {
      title: '告警级别',
      dataIndex: 'alertLevel',
      key: 'alertLevel',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div style={{minWidth:'70px'}}>{record.alertLevel}</div>
        )
      }
    },
    {
      title: 'promql',
      dataIndex: 'promql',
      key: 'promql',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div style={{minWidth:'100px',maxWidth:'300px'}}>{record.promql}</div>
        )
      }
    },
    {
      title: '告警信息',
      dataIndex: 'alertInfo',
      key: 'alertInfo',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertInfo}</div>
        )
      }
    },
    {
      title: '告警建议',
      dataIndex: 'alertAdvice',
      key: 'alertAdvice',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.alertAdvice }</div>
        )
      }
    },
    {
      title: '框架服务名',
      dataIndex: 'stackServiceName',
      key: 'stackServiceName',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div style={{minWidth:'100px'}}>{record.stackServiceName }</div>
        )
      }
    },
    {
      title: '框架角色名',
      dataIndex: 'stackRoleName',
      key: 'stackRoleName',
      // filters: alertLevelOptions,
      onFilter: true,
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div>{record.stackRoleName }</div>
        )
      }
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div>{formatDate(record.updateTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <div>{formatDate(record.createTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '修改',
      dataIndex: 'update',
      key: 'update',
      render: (_: any, record:API.alertRulesItem) => {
        return (
          <Button type="link" onClick={()=>{
            showModal(record,'edit')
          }}>
            修改
          </Button>
        )
      }
    },
  ]

  const onChange = (key: string) => {
    console.log(key);
    setCurrentTab(key)
    // const params = {serviceInstanceId: serviceId}
    switch(key){
      case 'activeAlert':  getActiveData(); break;
      case 'historyAlert': getHistoryData();break;
      case 'rulesAlert': getRulesData();break;
      default:break;
    }
  };

  const tabs = [
    {
      label:'活跃告警',
      key:'activeAlert',
      children:(
        <ProTable 
          search={false} 
          rowKey="alertId" 
          columns={activeColumns} 
          dataSource={activeListData}
          request={async (params = {}, sort, filter) => {
            return getActiveAlertAPI({ });;
          }}
        />
      )
    },
    {
      label:'历史告警',
      key:'historyAlert',
      children:(
        <ProTable 
          search={false} 
          rowKey="alertId" 
          columns={historyColumns} 
          dataSource={historyListData}
          request={async (params = {}, sort, filter) => {
            return getHistoryAlertAPI({ });;
          }}
        />
      )
    },
    {
      label:'告警规则',
      key:'rulesAlert',
      children:(
      <>
          <Button type="primary" onClick={()=>{
            showModal(null,'add')
          }}>
            新增告警规则
          </Button>
          <ProTable 
            search={false} 
            rowKey="id" 
            columns={rulesColumns} 
            dataSource={rulesListData}
            request={async (params = {}, sort, filter) => {
              return getRulesAlertAPI({ });;
            }}
          />
      </>
      )
    }
  ]

  const handleServiceChange = (value: any) => {
    setServiceName(value);
    setRoleName(stackServiceRoles[value][0]);
    setRoleList(stackServiceRoles[value])
    ruleForm.setFieldValue('stackRoleName', stackServiceRoles[value][0])
  }
  const handleRoleChange = (value: any) => {
    setRoleName(value);
  }
  
  

  return (
    <PageContainer>
      <div className={styles.tabsBar}>
          {
            tabs.map(item=>{
              return (
                <div className={`${currentTab == item.key? styles.actived : ''}`} key={item.key} onClick={(e)=>onChange(item.key)}>
                  {item.label}
                </div>
              )
            })
          }
        </div>
        <div className={styles.tabContent}>
          { tabs.filter(item=>{return item.key == currentTab})[0].children}
        </div>
        <Modal
            width={800}
            title={`${isEdit ? '修改告警规则' : '新增告警规则'}`}
            forceRender={true}
            destroyOnClose={true}
            open={isModalOpen}
            onOk={handleOk}
            confirmLoading={saveLoading}
            onCancel={()=>{
              setIsModalOpen(false)
            }}
        >

            <Form
              form={ruleForm}
              name={isEdit ? '修改告警规则' : '新增告警规则'}
              preserve={true}
              labelCol={{ span: 6 }}
              wrapperCol={{ span: 15 }}
              autoComplete="off"
            >
              <Form.Item
                label="规则名称"
                name="ruleName"
                rules={[{ required: true, message: '请输入规则名称!' }]}
              >
                <Input className={styles.inputWrap}/>
              </Form.Item>
              <Form.Item
                label="告警级别"
                name="alertLevel"
                rules={[{ required: true, message: '请选择告警级别!' }]}
              >
                <Select
                  className={styles.inputWrap}
                  options={[
                    { value: '告警级别', label: '告警级别' },
                    { value: '异常级别', label: '异常级别' },
                  ]}
                />
              </Form.Item>
              <Form.Item
                label="promql"
                name="promql"
                rules={[{ required: true, message: '请输入promql!' }]}
              >
                <TextArea rows={4} className={styles.inputWrap}/>
              </Form.Item>
              <Form.Item
                label="告警信息"
                name="alertInfo"
                rules={[{ required: true, message: '请输入告警信息!' }]}
              >
                <Input className={styles.inputWrap}/>
              </Form.Item>
              <Form.Item
                label="告警建议"
                name="alertAdvice"
                rules={[{ required: true, message: '请输入告警建议!' }]}
              >
                <Input className={styles.inputWrap}/>
              </Form.Item>
              <Form.Item
                label="框架服务名"
                name="stackServiceName"
                rules={[{ required: true, message: '请输入框架服务名!' }]}
              >
                {
                  serviceList ?(
                    <Select
                      defaultValue={serviceList[0]}
                      onChange={handleServiceChange}
                      options={serviceList?.map((item:any) => ({ label: item, value: item }))}
                    />
                  ):''
                }
                
              </Form.Item>
              <Form.Item
                label="框架角色名"
                name="stackRoleName"
                rules={[{ required: true, message: '请输入框架角色名!' }]}
              >
                {
                  
                    <Select
                      value={roleName}
                      onChange={handleRoleChange}
                      options={roleList?.map((item:any) => ({ label: item, value: item }))}
                    />
                  
                }                
              </Form.Item>
            </Form>
        </Modal>
      {/* <ProTable 
        search={false} 
        rowKey="alertId" 
        columns={columns} 
        dataSource={activeListData}
        request={async (params = {}, sort, filter) => {
          return getActiveAlertAPI({ });;
        }}
      /> */}
    </PageContainer>
  );
};

export default alertList;
