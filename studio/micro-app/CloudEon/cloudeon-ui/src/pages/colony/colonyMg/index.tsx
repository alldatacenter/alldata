// 集群管理页面
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { SlackOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { Space, Popconfirm, Button, Modal, Form, Input, message, Spin, Select, Popover } from 'antd';
import { FormattedMessage, useIntl, history, useModel } from 'umi';
import { useState, useEffect, useRef } from 'react';
import styles from './index.less';
import { getClusterListAPI, getStackListAPI, createClusterAPI, deleteClusterAPI } from '@/services/ant-design-pro/colony';

import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-xcode";
import "ace-builds/src-noconflict/ext-language_tools";


const { Option } = Select;

const Colony: React.FC = () => {
  // const intl = useIntl();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [stackLoading, setStackLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [clusterList, setClusterList] = useState<API.ColonyItem[]>();
  const [stackList, setStackList] = useState<API.StackItem[]>();
  const [kubeConfig, setKubeConfig] = useState('');
  const [colonyForm] = Form.useForm();
  const [editColony, setEditColony] = useState<API.ColonyItem | null>(null);

  const kubeConfigRef = useRef(kubeConfig)
  kubeConfigRef.current = kubeConfig
  

  const getClusterData = async (params: any) => {
    setLoading(true)
    const result: API.ColonyList =  await getClusterListAPI(params);
    setLoading(false)
    setClusterList(result?.data)
  };

  const getStackData = async (params: any) => {
    setStackLoading(true)
    const result: API.StackList =  await getStackListAPI(params);
    setStackLoading(false)
    setStackList(result?.data)
  };

  useEffect(() => {
    getClusterData({});
  }, []);

  const showModal = () => {
    getStackData({})
    setIsModalOpen(true);
  };

  const handleOk = () => {
    colonyForm
      .validateFields()
      .then(async (values) => {
        // console.log('values',values);
        setConfirmLoading(true)
        let params = values
        if(editColony){
          params={...params,id: editColony.id}
        }
        const result: API.normalResult = await createClusterAPI(params)
        if(result?.success){
          message.success(editColony ? '修改成功' : '新增成功');
          getClusterData({});
          colonyForm.resetFields()
          setConfirmLoading(false)
        }
        setIsModalOpen(false);
      })
      .catch((err) => {
        console.log('err: ', err);
      });
    // setIsModalOpen(false);
  };

  const handleCancel = () => {
    setIsModalOpen(false);
  };

  const confirm = async(e: any, id:any) => {
    e.stopPropagation();
    console.log(e);
    setLoading(true)
    const result: API.normalResult = await deleteClusterAPI({id})
    if(result?.success){
      getClusterData({});
      message.success('已删除');
    }else{
      message.error('删除失败：'+result.message);
    }
    setLoading(false)
  };

  const cancel = (e: any) => {
    e.stopPropagation();
    console.log(e);
    message.error('已取消');
  };

  const onFinish = async (values: any) => {
    const result: API.normalResult = await createClusterAPI(values)
    if(result && result.success){
      message.success('新增成功');
      getClusterData({});
      setIsModalOpen(false);
    }
  };

  const onFinishFailed = (errorInfo: any) => {
    console.log('Failed:', errorInfo);
  };

  const onSelectChange = (value: string) => {
    console.log('onSelectChange:', value);
  };

  const onEditorChange = (value: any) => {
    console.log(value);
    
  }
  
  // const { colonyData, setClusterId } = useModel('colonyModel', model => ({ colonyData: model.colonyData, setClusterId: model.setClusterId }));

  // const gotoColony = (value: any) => {
  //   const { colonyData, setClusterId } = useModel('colonyModel', model => ({ colonyData: model.colonyData, setClusterId: model.setClusterId }));
  //   setClusterId(value)
  //   history.push('/colony/nodeList');
  // }

  // type formItem = {
  //   clusterName?: string,
  //   stackId?: number,
  //   kubeConfig?: string | null
  // }

  const handleUpdate = (item: API.ColonyItem) => {
    setEditColony(item)
    colonyForm.setFieldsValue({
      clusterName: item.clusterName,
      stackId: item.stackId,
      kubeConfig: item.kubeConfig      
    })
    showModal()
  }

  return (
    <PageContainer
      header={{
        extra: [
          <Button
            type="primary"
            key="colonyadd"
            onClick={() => {
              setEditColony(null)
              colonyForm.setFieldsValue({})
              showModal()
            }}
          >
            新增集群
          </Button>,
        ],
      }}
    >
      <Spin tip="Loading" size="small" spinning={loading}>
      <div className={styles.clusterLayout}>
        {clusterList&&clusterList.map(cItem=>{
          return(
              <ProCard
                hoverable
                key={cItem.id}
                className={styles.clusterBox}
                bodyStyle={{ padding: 0 }}
                actions={[
                  <Popconfirm
                    title="确定删除该集群吗?"
                    onConfirm={(e)=>confirm(e, cItem.id)}
                    onCancel={cancel}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Popover content="删除" title="">
                      <DeleteOutlined
                        key="setting"
                        onClick={(e) => {
                          e.stopPropagation();
                          console.log('SettingOutlined');
                        }}
                      />
                    </Popover>
                  </Popconfirm>,
                  <Popover content="修改" title="">
                    <EditOutlined onClick={(e)=>{ e.stopPropagation(); handleUpdate(cItem) }}/>
                  </Popover>
                ]}
                onClick={() => {
                  // setClusterId(cItem.id || 0)
                  const sdata = {
                    clusterId: cItem.id,
                    clusterName: cItem.clusterName,
                    stackId: cItem.stackId
                  }
                  const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')
                  sessionStorage.setItem('colonyData',JSON.stringify({...getData ,...sdata}) )
                  history.push('/colony/nodeList?clusterId='+cItem.id);
                }}
              >
                <div className={styles.stackWrap}>
                  <div className={styles.titleWrap}>
                    <SlackOutlined className={styles.cardIcon} />
                    <div className={styles.stackTitle}>{cItem.clusterName}</div>
                  </div>
                  <div className={styles.stackInfo}>
                    <div className={styles.stackDesc}>服务数：{cItem.serviceCnt}</div>
                    <div className={styles.stackDesc}>节点数：{cItem.nodeCnt}</div>
                  </div>
                </div>
              </ProCard>
          )
        })}
        
      </div>
      </Spin>
      <Modal
        title={editColony ? "修改"+editColony.clusterName+"集群" : "新增集群"}
        forceRender={false}
        destroyOnClose={true}
        open={isModalOpen}
        onOk={handleOk}
        width={'80%'}
        onCancel={handleCancel}
        confirmLoading={confirmLoading}
        // footer={null}
      >
        <div>
          <Form
            form={colonyForm}
            name={editColony ? "修改"+editColony.clusterName+"集群" : "新增集群"}
            preserve={false}
            labelCol={{ span: 6 }}
            wrapperCol={{ span: 15 }}
            onFinish={onFinish}
            onFinishFailed={onFinishFailed}
            autoComplete="off"
          >
            <Form.Item
              label="集群名称"
              name="clusterName"
              rules={[{ required: true, message: '请输入集群名称!' }]}
            >
            <Input className={styles.inputWrap}/>
            </Form.Item>
            <Form.Item
              label="框架"
              name="stackId"              
              rules={[{ required: true, message: '请选择框架!' }]}
            >                
                <Select
                  disabled={editColony ? true : false}
                  placeholder="请选择框架"
                  onChange={onSelectChange}
                  loading={stackLoading}
                  className={styles.inputWrap}
                  allowClear
                >
                  {
                    stackList && stackList.map(sItem=>{
                      return (
                        <Option key={sItem.stackCode} value={sItem.id}>{sItem.stackCode}</Option>
                      )
                    })
                  }
                </Select>
            </Form.Item>
            <Form.Item
              label="kubeConfig"
              name="kubeConfig"
              rules={[{ required: true, message: '请输入kubeConfig!' }]}
            >
              {/* <Input /> */}
              <AceEditor
                className={styles.codeEditorWrap}
                mode="java"
                theme="xcode"
                value={kubeConfig}
                onChange={onEditorChange}
                name="UNIQUE_ID_OF_DIV"
                editorProps={{ $blockScrolling: true }}
              />

            </Form.Item>

            {/* <Form.Item wrapperCol={{ offset: 8, span: 16 }}>
              <Button type="primary" htmlType="submit">
                确定
              </Button>
            </Form.Item> */}
          </Form>
        </div>
      </Modal>
    </PageContainer>
  );
};

export default Colony;
