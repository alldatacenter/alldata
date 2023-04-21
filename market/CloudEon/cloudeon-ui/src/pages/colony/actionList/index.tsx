import { PageContainer, ProCard, ProTable } from '@ant-design/pro-components';
import { Space, Card, Table, Tag, Button, Modal, Form, Progress, message, Spin } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { FormInstance } from 'antd/es/form';
import { FormattedMessage, useIntl, history } from 'umi';
import { getCommandListAPI } from '@/services/ant-design-pro/colony';
import { formatDate } from '@/utils/common'
import styles from './index.less'
import { RightOutlined } from '@ant-design/icons';
import { statusColor } from '../../../utils/colonyColor'

const actionList: React.FC = () => {
  const intl = useIntl();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [actionListData, setNodeListData] = useState<any[]>();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  // formRef = React.createRef<FormInstance>();
  // const form = useRef();

  const getTableData = async (params: any) => {
    setLoading(true)
    const result: API.normalResult =  await getCommandListAPI(params);
    setLoading(false)
    setNodeListData(result?.data)
  };

  const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')


  useEffect(() => {
    // getTableData({ clusterId: getData.clusterId });
   const timer =  setInterval(async()=>{
      const result: API.normalResult =  await getCommandListAPI({ clusterId: getData.clusterId });
      setNodeListData(result?.data)
    },2000)
    return () =>{
      clearInterval(timer)
    }
  }, []);

  interface Item {
      id: number;
      name: string;
      startTime: string;
      endTime: string;
      submitTime: string;
      currentProgress: number;
      commandState: string;
  }

  // const statusColor = {
  //   'ERROR': '#f5222d',
  //   'RUNNING': '#1890ff',
  //   'SUCCESS': '#52c41a',
  // }


  const columns = [
    {
      title: '指令名称',
      dataIndex: 'name',
      key: 'name',
      // width: 120,
      render: (_: any, record: Item) => {
        return (
          <a onClick={()=>{
            history.push(`/colony/actionList/detail?commandId=${record.id}`);
          }}>{record.name}</a>
        )
      },
    },
    {
      title: '状态',
      dataIndex: 'commandState',
      key: 'status',
      // render: (_: any, record: Item) => {
      //   return (
      //     <div style={{color:statusColor[record.commandState], whiteSpace: 'nowrap'}}> <span style={{backgroundColor: statusColor[record.commandState]}} className={styles.statusCircel}></span>{record.commandState}</div>
      //   )
      // },
      valueEnum: {
        0: {
          text: '未知',
          status: 'Default',
        },
        'RUNNING': {
          text: '运行中',
          status: 'Processing',
        },
        'SUCCESS': {
          text: '完成',
          status: 'Success',
        },
        'ERROR': {
          text: '出错',
          status: 'Error',
        },
      },
    },
    {
      title: '提交时间',
      dataIndex: 'submitTime',
      key: 'submitTime',
      render: (_: any, record: Item) => {
        return (
          <div>{formatDate(record.submitTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (_: any, record: Item) => {
        return (
          <div>{formatDate(record.startTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (_: any, record: Item) => {
        return (
          <div>{formatDate(record.endTime, 'yyyy-MM-dd hh:mm:ss')}</div>
        )
      },
    },
    {
      title: '进度',
      dataIndex: 'currentProgress',
      key: 'currentProgress',
      // width: 150,
      render: (_: any, record: Item) => {
        return (
          <Progress 
            percent={record.currentProgress} 
            strokeWidth={8} 
            strokeColor={statusColor[record.commandState]} 
            size="small" 
            status={record.commandState=='RUNNING'?"active":'normal'} 
            style={{minWidth:'150px'}}
          />
        )
      }
    },
    // {
    //   title: '资源',
    //   dataIndex: 'serviceRoleNum',
    //   key: 'serviceRoleNum',
    // },
    // {
    //   title: '用户名',
    //   dataIndex: 'operateUserId',
    //   key: 'operateUserId',
    // },
    // {
    //   title: '用户IP',
    //   dataIndex: 'totalMem',
    //   key: 'totalMem',
    // }
  ]
  

  return (
    <PageContainer>
      {/* <div>
        <div className={styles.carBar}>
          <div>
            <Tag color="#f50">#f50</Tag>
            <a 
            onClick={()=>{
              history.push(`/colony/actionList/detail`);
            }}>名称 <RightOutlined /></a>
          </div>
          <div>
            <Progress percent={50} strokeWidth={20} strokeColor="#aaa" size="small" status="active" />
          </div>
        </div>
      </div> */}
      <ProTable 
        search={false} 
        rowKey="id" 
        columns={columns} 
        dataSource={actionListData}
        request={async (params = {}, sort, filter) => {
          // console.log(sort, filter);
          return getCommandListAPI({ clusterId: getData.clusterId });;
        }}
      />
    </PageContainer>
  );
};

export default actionList;
