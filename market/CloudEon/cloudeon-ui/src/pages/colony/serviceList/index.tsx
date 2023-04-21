// 集群管理页面
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Space, Button, Avatar, Card, Spin, Popconfirm, message,Popover,Tooltip } from 'antd';
import { FormattedMessage, useIntl, history } from 'umi';
import { PlayCircleOutlined, ReloadOutlined, PoweroffOutlined, DeleteOutlined, AlertFilled } from '@ant-design/icons';
import { serviceListAPI, deleteServiceAPI, restartServiceAPI, stopServiceAPI, startServiceAPI } from '@/services/ant-design-pro/colony';
import { useState, useEffect } from 'react';
import { dealResult } from '../../../utils/resultUtil'
import styles from './index.less'
import { statusColor,serviceStatusColor } from '@/utils/colonyColor'

const { Meta } = Card;

const serviceList: React.FC = () => {
  const intl = useIntl();
  const [serviceList, setServiceList] = useState<any[]>();
  const [loading, setLoading] = useState(false);
  const [currentId, setCurrentId] = useState(0);
  const [btnLoading, setBtnLoading] = useState(false);
  const colonyData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')

  const getServiceListData = async (showLoading:boolean) => {
    const params = {
      clusterId: colonyData.clusterId
    }
    showLoading && setLoading(true)
    const result = await serviceListAPI(params)
    showLoading && setLoading(false)
    if(result?.success){
      setServiceList(result?.data)
    }
  }
  const handleDeleteService = async (params:any) => {
    setLoading(true)
    const result = await deleteServiceAPI(params)
    setLoading(false)
    if(result?.success){
      getServiceListData(true)
      message.success('操作成功！', 3)
    }
  }

  const handleACT = async (key: string, serviceId:number) => {
    if(!serviceId) return
    setCurrentId(serviceId)
    let result: API.normalResult
    setBtnLoading(true)
    const params = {serviceInstanceId: serviceId}
    switch(key){
      case 'start': 
        result = await startServiceAPI(params);
        dealResult(result, key)
        break;
      case 'stop': 
        result = await stopServiceAPI(params);
        dealResult(result, key)
        break;
      case 'restart': 
        result = await restartServiceAPI(params);
        dealResult(result, key)
        break;
      case 'delete':
        result = await deleteServiceAPI(params)
        dealResult(result, key);
        break;
      case 'update':;break;
      default:;
    }    
    setBtnLoading(false)
  }

  const btnLoadingStatus = (id: number) => {
    return currentId == id && btnLoading
  }

  useEffect(()=>{
    getServiceListData(true)
    const intervalId = setInterval(()=>{
      getServiceListData(false)
    },2000)
    return () => clearInterval(intervalId);
  },[])


  return (
    <PageContainer
      header={{
        extra: [
          <Button
            key="addservice"
            type="primary"
            onClick={() => {
              history.push('/colony/serviceList/addService');
            }}
          >
            新增服务
          </Button>,
        ],
      }}
    >
      <Spin tip="Loading" size="small" spinning={loading}>
      <Space className={styles.serviceList}>
        {
          (serviceList && serviceList.length > 0) ? (
            serviceList.map(sItem=>{
              return (
                <Spin tip="Loading" key={sItem.serviceName+'spin'} size="small" spinning={btnLoadingStatus(sItem.id)}>
                  <Card
                    hoverable
                    key={sItem.serviceName}
                    style={{ width: 300 }}
                    actions={[                      
                      <Popconfirm
                            key='startPop'
                            title="确定要启动吗?"
                            onConfirm={()=>{ handleACT('start',sItem.id) }}
                            okText="确定"
                            cancelText="取消"
                      >
                        <Popover content="启动" title="">
                          <PlayCircleOutlined/>
                        </Popover>
                      </Popconfirm>,
                      <Popconfirm
                            key='stopPop'
                            title="确定要停止吗?"
                            onConfirm={()=>{ handleACT('stop',sItem.id) }}
                            okText="确定"
                            cancelText="取消"
                      >
                      <Popover content="停止" title="">
                        <PoweroffOutlined />
                      </Popover>
                      </Popconfirm>,
                      <Popconfirm
                            key='restartPop'
                            title="确定要重启吗?"
                            onConfirm={()=>{ handleACT('restart',sItem.id) }}
                            okText="确定"
                            cancelText="取消"
                      >
                      <Popover content="重启" title="">
                        <ReloadOutlined />
                      </Popover>
                      </Popconfirm>,
                      <Popover content="删除" title="">
                        <Popconfirm
                          title="确定删除该服务吗？"
                          onConfirm={()=>{
                            handleDeleteService({serviceInstanceId: sItem.id })
                          }}
                          onCancel={()=>{}}
                          okText="确定"
                          cancelText="取消"
                        >
                          <DeleteOutlined key="setting"/>
                        </Popconfirm>
                      </Popover>,
                      // <SettingOutlined />,
                      // <EditOutlined key="edit" />,
                      // <EllipsisOutlined key="ellipsis" />,
                    ]}
                  >
                    {
                      sItem.alertMsgCnt ? 
                      <div className={styles.alertWrap}>
                        <Tooltip 
                          placement="top" 
                          color="#fff"
                          title={
                            <div className={styles.alertText}>
                              {`该服务当前有${sItem.alertMsgCnt}个告警：`}
                              {sItem.alertMsgName.map((msg:any,index:any)=>{
                                return <div key={index}>{`${index+1}. ${msg}`}</div>
                              })}
                            </div>
                          }
                        >
                          <AlertFilled style={{fontSize:'18px'}} />
                        </Tooltip>                      
                      </div> :''
                    }
                    <div 
                      style={{cursor:'pointer'}}
                      onClick={() => {
                        history.push(`/colony/serviceList/detail?serviceName=${sItem.serviceName}&id=${sItem.id}`);
                      }}>
                        <Meta
                          avatar={<Avatar style={{width:'40px', height:'40px'}} src={'data:image/jpeg;base64,'+sItem.icon} />}
                          title={<div style={{paddingLeft:'2px'}}>{sItem.serviceName}</div>}
                          description={<div style={{paddingLeft:'2px'}}>
                          <span style={{backgroundColor: serviceStatusColor[sItem.serviceStateValue || 0]}} 
                                className={`${styles.statusCircel} ${[1,2,3,4,5,8].includes(sItem.serviceStateValue) && styles.statusProcessing}`}>
                            </span>
                          {sItem.serviceState}
                          </div>}
                        />
                    </div>
                  </Card>
                </Spin>
                
              )

            })

          ):(
            <div>
              暂无数据，请新增服务
            </div>
            // <Card
            //   style={{ width: 300 }}
            //   actions={[
            //     <SettingOutlined key="setting" />,
            //     <EditOutlined key="edit" />,
            //     <EllipsisOutlined key="ellipsis" />,
            //   ]}
            // >
            //   <div 
            //   style={{cursor:'pointer'}}
            //   onClick={() => {
            //     history.push('/colony/serviceList/detail');
            //   }}>
            //       <Meta
            //         avatar={<Avatar src="https://gw.alipayobjects.com/zos/rmsportal/JiqGstEfoWAOHiTxclqi.png" />}
            //         title="测试 title"
            //         description="This is the description"
            //       />
            //   </div>
            // </Card>
          )
        }
      </Space>
      </Spin>
    </PageContainer>
  );
};

export default serviceList;
