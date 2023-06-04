import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Progress, Modal, Spin } from 'antd';
import styles from './index.less'
import { /*commandInfos,*/ statusColor,trailColor } from '../../../../utils/colonyColor'
import { formatDate } from '@/utils/common'
import { useState, useEffect, ReactChild, ReactFragment, ReactPortal } from 'react';
import { getCommandDetailAPI, getTaskLogAPI } from '@/services/ant-design-pro/colony';
import { FormattedMessage, history, SelectLang, useIntl, useModel } from 'umi';

import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrow } from 'react-syntax-highlighter/dist/esm/styles/hljs';

const actionDetail: React.FC = () => {

  const [selectedTask, setSelectedTask] = useState('ZOOKEEPER1');
  const [selectIndex, setSelectIndex] = useState(0)
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [commandInfos, setCommandInfos] = useState<API.commandType>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [logData, setLogData] = useState('');
  

  const getTableData = async (params: any) => {
    setLoading(true)
    const result: API.commandResult =  await getCommandDetailAPI(params);
    setLoading(false)
    if(result?.success){
        setSelectedTask(result?.data?.serviceProgresses && result?.data?.serviceProgresses[0]?.serviceInstanceName ||'')
        setCommandInfos(result?.data || {})
    }
  };

  const getTaskLog = async (params: any) => {
    setDetailLoading(true)
    const result: API.logResult =  await getTaskLogAPI(params);
    setDetailLoading(false)
    if(result?.success){
        setLogData(result?.data || '')
    }else{
        setLogData('')
    }
  }

  const handleOk = () => {
  };

  const handleCancel = () => {
    setIsModalOpen(false);
  };

  const handleTask = (name:any,index:any) => {
    setSelectedTask(name)
    setSelectIndex(index)
  }

  const handleLog = (id: number) => {
    if(!id) return
    // const codemirrorDom = document.getElementsByClassName('CodeMirror')[0]
    // codemirrorDom.setAttribute("style","height: auto")
    setIsModalOpen(true)
    getTaskLog({commandTaskId: id})
  }


  useEffect(() => {
    const { query } = history.location;
    const params = { commandId: query?.commandId }
    getTableData(params);
    const timer =  setInterval(async()=>{
        const result: API.commandResult =  await getCommandDetailAPI(params);
        if(result?.success){
            setCommandInfos(result?.data || {})
        }
    },2000)
    return () =>{
        clearInterval(timer)
    }
  }, []);

    
    return (
        <PageContainer header={{title:''}} >
            <Spin tip="Loading" size="small" spinning={loading} style={{height:'100%',width:'100%'}}>
            {commandInfos?.id && (
            <div className={styles.commandLayout}>
                <div className={styles.titleBar}>{commandInfos.name}</div>
                    <div className={styles.titleWrap}>
                        {/* <div className={styles.titleBar}>{commandInfos.name}</div> */}

                        <div className={styles.allProcessBar}>
                            {/* <div style={{width:'60px'}}>总进度：</div> */}
                            <Progress 
                                percent={commandInfos.currentProgress} 
                                strokeWidth={10} 
                                width={70}
                                type="circle"
                                // format={percent => `总进度：${commandInfos.currentProgress}%`}
                                strokeColor={statusColor[commandInfos.commandState ||'DEFAULT']} 
                                size="small" 
                                // trailColor={trailColor[commandInfos.commandState ||'DEFAULT']}
                                status={commandInfos.commandState=='RUNNING'?"active":'normal'} 
                                style={{minWidth:'120px'}}
                            />
                        </div>
                        <div className={styles.otherInfo}>
                            <div className={styles.otherInfoItem}>
                                状态：
                                <div style={{color: statusColor[commandInfos.commandState ||'DEFAULT'], whiteSpace: 'nowrap',display:'inline-block'}}> 
                                    <span style={{backgroundColor: statusColor[commandInfos.commandState ||'DEFAULT']}} className={styles.statusCircel}>
                                    </span>
                                    {commandInfos.commandState}
                                </div>
                            </div>
                            <div className={styles.otherInfoItem}>开始时间：{formatDate(commandInfos.startTime, 'yyyy-MM-dd hh:mm:ss')}</div>
                            <div className={styles.otherInfoItem}>结束时间：{formatDate(commandInfos.endTime, 'yyyy-MM-dd hh:mm:ss')}</div>
                            <div className={styles.otherInfoItem}>提交时间：{formatDate(commandInfos.submitTime, 'yyyy-MM-dd hh:mm:ss')}</div>
                            <div className={styles.otherInfoItem}>操作人：{commandInfos.operateUserId}</div>
                        </div>
                    </div>
                    <div className={styles.content}>
                        <div className={styles.overallInfo}>
                            <div className={styles.contentLeft}>
                                { commandInfos?.serviceProgresses?.map((taskItem: API.progressItem, _index: any) => {
                                    return (
                                        <div 
                                            key={taskItem.serviceInstanceName}
                                            className={[styles.taskmenu, selectedTask == taskItem.serviceInstanceName ? styles.activetaskmenu :null].join(' ')}
                                            onClick={()=>{
                                                handleTask(taskItem.serviceInstanceName,  _index)
                                            }}
                                        >
                                            {taskItem.serviceInstanceName}
                                            <Progress 
                                                percent={Math.floor(taskItem.successCnt/taskItem.totalCnt * 100)} 
                                                steps={5}
                                                // strokeWidth={10} 
                                                // width={40}
                                                // type="circle"
                                                strokeColor={statusColor[taskItem.currentState ||'DEFAULT']} 
                                                size="small" 
                                                // status={taskItem.currentState=='RUNNING'?"active":'normal'} 
                                                style={{minWidth:'60px', marginLeft:'10px', fontSize:'12px'}}
                                            />
                                        </div>
                                    )
                                })
                                }
                            </div>
                            <div className={styles.contentRight}>
                                {
                                    commandInfos?.serviceProgresses && commandInfos?.serviceProgresses[selectIndex].taskDetails?.map( 
                                        (taskDetail) =>{
                                        return (
                                            <div className={styles.taskDetailItem} key={`${taskDetail.id}${taskDetail.taskName}`}>
                                                <div className={styles.taskDetailCenter}>
                                                    <div className={styles.taskName}>{taskDetail.taskName}</div>
                                                    <div className={styles.taskTimeWrap}>
                                                        <Progress 
                                                            percent={taskDetail.progress} 
                                                            strokeWidth={8} 
                                                            strokeColor={statusColor[taskDetail.commandState||'DEFAULT']} 
                                                            size="small" 
                                                            status={taskDetail.commandState=='RUNNING'?"active":'normal'} 
                                                            style={{width:'450px'}}
                                                        />
                                                    </div>
                                                </div>
                                                <div className={styles.taskDetailRight}>
                                                    <div className={styles.taskTime}>开始：{formatDate(taskDetail.startTime, 'yyyy-MM-dd hh:mm:ss')}</div>
                                                    <div className={styles.taskTime}>结束：{formatDate(taskDetail.endTime, 'yyyy-MM-dd hh:mm:ss')}</div>
                                                </div>
                                                <div className={styles.taskDetailLog}>
                                                    <a onClick={()=>handleLog(taskDetail.id || 0)}> 日志 </a>
                                                </div>
                                            </div>
                                        )
                                    })
                                }

                            </div>
                        </div>
                    </div>
                    
                    
            </div>

            )}
            </Spin>
            <Modal
                    key="commandDetailmodal"
                    title="日志信息"
                    width="80%"
                    style={{height:'80vh'}}
                    forceRender={true}
                    destroyOnClose={false}
                    open={isModalOpen}
                    onOk={handleOk}
                    onCancel={handleCancel}
                    footer={null}
                >
                    {/* <div dangerouslySetInnerHTML={{__html:logData}} /> */}
                    <div>
                        <Spin tip="Loading" size="small" spinning={detailLoading}>
                        <SyntaxHighlighter 
                            language="yaml" 
                            style={tomorrow} 
                            showLineNumbers 
                            customStyle={{height:'60vh',overflow:'auto'}}
                        >
                            {logData}
                        </SyntaxHighlighter>
                        </Spin>
                    </div>
                </Modal>
        </PageContainer>
    )
}

export default actionDetail