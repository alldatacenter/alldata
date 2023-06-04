import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Space, Steps, Button, Spin, message, notification, Alert } from 'antd';
import { BorderOuterOutlined } from '@ant-design/icons';
import { FormattedMessage, useIntl, history } from 'umi';
import { useState, useEffect, SetStateAction } from 'react';
import styles from './index.less';
import { getServiceListAPI, checkServiceAPI, getServiceConfAPI, initServiceAPI, serviceListAPI, getRolesAllocationAPI } from '@/services/ant-design-pro/colony';
import ChooseService from './components/ChooseService'
import ConfigSecurity from'./components/ConfigSecurity'
import AssignRoles from'./components/AssignRoles'
import ConfigService from'./components/ConfigService'
import {cloneDeep} from 'lodash'
// import ConfigSecurity from'./components/ConfigSecurity'


const serviceAdd: React.FC = () => {
  const intl = useIntl();
  const colonyData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')
  const [current, setCurrent] = useState(0);
  const [serviceListData, setServiceListData] = useState<any[]>();
  const [loading, setLoading] = useState(false);
  const [checkServiceLoading, setCheckServiceLoading] = useState(false);
  const [allParams, setSubmitallParams] = useState<API.SubmitServicesParams>(); // 安装提交的params
  const [serviceInfos, setServiceInfos] = useState<API.ServiceInfosItem[]>()
  const [rolesAllNodeIds, setRolesAllNodeIds] = useState<API.rolesValidResult>() //获取所有角色的推荐节点
  const [checkRoleParNext, setCheckRoleParNext] = useState(true); // 判断分配角色的按钮是否disabled
  const [checkCofParNext, setCheckCofParNext] = useState(true);
  // const [warnInfo, setWarnInfo] = useState<any[]>([]); // 校验角色配置选择
  const [errInfo, setErrInfo] = useState<any[]>([]); // 校验角色配置选择

  const checkService = async (params: any) => {
    try {
      setLoading(true)
      const result: API.normalResult =  await checkServiceAPI(params);
      setLoading(false)
      if(!result.success){
        // message.error(result.message);
        return false
      }
      return true
    } catch (error) {
      // message.error(error);
      return false
    }
  }; 

  const getServiceData = async (params: any) => {
    setLoading(true)
    const result: API.ServiceList =  await getServiceListAPI(params);
    setLoading(false)
    const statusData = result?.data?.map(item=>{
      return {
        ...item,
        selected:false
      }
    })
    setServiceListData(statusData)
  };

  const getRolesAll = async (params: any) => {
    setLoading(true)
    const result: API.rolesValidResult =  await getRolesAllocationAPI(params);
    setLoading(false)
    setRolesAllNodeIds(result?.data)
    return result?.data
  };

  const getSelectedService = ()=>{
    const selectList = serviceListData?.filter(item=>{ return item.selected})
    return selectList
  }

  const changeStatus = (ids:number[]) => {
    console.log('ids: ', ids);
    
    const statusData = serviceListData && serviceListData.map(item=>{
      return {
        ...item,
        selected: ids.includes(item.id) ? true : false 
      }
    })
    setServiceListData(statusData)

    // 选择服务的数据放到总数据
    const serList = getSelectedService()
    let params = {...allParams}
    serList?.map(sItem=>{
      const sData = {
        stackServiceId: sItem.id,
        stackServiceName: sItem.name,
        stackServiceLabel: sItem.label,
        roles: sItem.roles.map((role: any)=>{
          return {
            stackRoleName: role,
            nodeIds:[]
          }
        }),
        presetConfList:[],
        customConfList:[]
      }
      const pIndex = params?.serviceInfos?.findIndex(pItem=>{ return pItem.stackServiceId && ids.includes(pItem.stackServiceId) })
      if(pIndex && pIndex == -1){
        params?.serviceInfos?.push(sData)
      }
    })
    
    setSubmitallParams(params)
  }

  // 已选择服务的id集合
  const selectListIds = getSelectedService()?.map(stem=> { return stem.id })

  // 配置安全的数据放到总数据
  const setKerberosToParams = (value: boolean) => {
    let params = {...allParams}
    if(value){
      params.enableKerberos = true
    }
    setSubmitallParams(params)
  }

  const setServiceInfosToParams = (value: API.ServiceInfosItem[]) => {
    let params = {...allParams}
    if(value){
      params.serviceInfos = value
    }
    setSubmitallParams(params)
  }

  interface anyKey{
    [key:number]:any,
  }
  
  const setPresetConfListToParams = async() => {
    const allConfData = JSON.parse(sessionStorage.getItem('allConfData') || '{}')
    let presetConfData:anyKey = {}
    let customConfData:anyKey = {}
    for(let key in allConfData){
      presetConfData[key] = []
      customConfData[key] = []
      allConfData[key].forEach((item: { isCustomConf: any; name: any; value: any; confFile: any; sourceValue: any; recommendExpression: any; })=>{
        if(item.isCustomConf){
          customConfData[key].push({
            name: item.name,
            value: item.value,
            confFile: item.confFile
          })
        }else{
          presetConfData[key].push({
            name: item.name,
            recommendedValue: item.sourceValue,
            value: item.value
          })
        }
      })
      // allConfData[key] = allConfData[key].map((item: { name: any; sourceValue: any; recommendExpression: any; })=>{
      //   return {
      //     name: item.name,
      //     recommendedValue: item.sourceValue,
      //     value: item.recommendExpression
      //   }
      // })
    }
    let params = cloneDeep(allParams)
    if(params?.serviceInfos?.length){
      for(let i=0; i < params.serviceInfos.length; i++){
        let psItem = params.serviceInfos[i]
        if(psItem.stackServiceId){
          if(allConfData[psItem.stackServiceId]){
            psItem.presetConfList = presetConfData[psItem.stackServiceId] //allConfData[psItem.stackServiceId]
            psItem.customConfList = customConfData[psItem.stackServiceId]
          } else {
              setLoading(true)
              const params = {
                  serviceId: psItem.stackServiceId,
                  inWizard: true
              }
              const result =  await getServiceConfAPI(params);
              const confsList = result?.data?.confs || []
              const confsdata = confsList.map((cItem)=>{
                return {
                  name: cItem.name,
                  recommendedValue: cItem.recommendExpression,
                  value: cItem.recommendExpression
                }
              })
              setLoading(false)
              psItem.presetConfList = confsdata
              psItem.customConfList = []
  
          }
        }
      }
    }
    setSubmitallParams(params)
    return params
  }

  // const checkRoleNext = (value:boolean) =>{
  //   setCheckRoleParNext(value)
  // }

  const checkNext = () => {
    switch(current){
      case 0:
        const selectList = getSelectedService()
        return (selectList && selectList.length > 0 ? true : false)
      ;break;
      case 1:
        return checkRoleParNext
      ;break;
      case 2:
        return checkCofParNext;
      ;break;
      case 3:
        return true
      ;break;
      case 4:
        return true
      ;break;
      case 5:
        return true
      ;break;
    }
  }

  


  let stepList: any[] = [
    { title: '选择服务', status: '' },
    // { title: '配置安全', status: '' },
    { title: '分配角色', status: '' },
    { title: '配置服务', status: '' },
    // { title: '服务总览', status: '' },
    { title: '安装', status: '' },
  ];
  const onChange = (value: number) => {
    // setCurrent(value);
  };

  const initServiceInfos = (values: any) =>{    
    console.log("initServiceInfos: ", values);
    
    const nodesIds = values || rolesAllNodeIds
    const serArr = getSelectedService()?.map(item=>{
        return {
            stackServiceId: item.id,
            stackServiceName: item.name,
            stackServiceLabel: item.label,
            roles: item.roles.map((role: any)=>{
              let roles = nodesIds && nodesIds[item.id].filter((roleItem: { stackRoleName: any; })=>{ return roleItem.stackRoleName == role}) 
              let nodeIds = roles && roles.length>0 && roles[0].nodeIds || []
              let validRule = roles[0]
                return {
                    stackRoleName: role,
                    nodeIds: nodeIds,//[]
                    validRule: validRule, // 角色选择节点的校验规则
                }
            }),
            presetConfList:[],
            customConfList:[]
        }
    })    
    setServiceInfos(serArr)
  }

  const installService = async( initParams: API.SubmitServicesParams) =>{
    setLoading(true)
    const result = await initServiceAPI(initParams)
    setLoading(false)
    if(result?.success){
      message.success('安装成功！', 3)
      setTimeout(()=>{
        history.replace('/colony/serviceList');
      },3)
    }
  }

  // 弹框角色分配校验结果
  const [api, contextHolder] = notification.useNotification();
  const openNotificationWithIcon = (errList:string[]) =>{
      notification.destroy()
      if(!errList || errList.length == 0) return
      const errDom = errList.map(item=>{
          return (<Alert type="error" key={item} message={item} banner />)
      })
      notification.open({
          message: '温馨提示',
          description:<>{errDom}</>,
          duration:null,
          style: {
              width: 500
          }
        });
  }

  // 角色分配校验：检验是否符合节点选择规则
  const checkAllRolesRules = (serviceInfos:API.ServiceInfosItem[]) => {
    if(!serviceInfos || serviceInfos.length == 0) return true
    setErrInfo([])
    let errStr = ''
    let errList:any[] = []
    serviceInfos.forEach(item=>{
      item.roles?.forEach(roleItem=>{
        let { nodeIds } = roleItem
        let {fixedNum, minNum, needOdd, stackRoleName} = roleItem.validRule
        if(roleItem.validRule && stackRoleName){
          if(minNum && (!nodeIds || nodeIds.length < minNum)){
            errStr = `${stackRoleName} 要求至少选择${minNum}个节点数`
            errList.push(errStr)
          }
          if(fixedNum && (!nodeIds || nodeIds.length != fixedNum)){
            errStr = `${stackRoleName} 要求选择${fixedNum}个节点数`
            errList.push(errStr)
          }
          if(needOdd && (!nodeIds || nodeIds.length%2 == 0)){
            errStr = `${stackRoleName} 要求选择奇数个节点数`
            errList.push(errStr)
          }
        }        
      })
    })
    setCheckRoleParNext(errList && errList.length > 0 ? false : true)
    setErrInfo(errList)
    openNotificationWithIcon(errList)
    return errList && errList.length > 0 ? false : true
  }

  const checkConfNext = (val:any) => {
    setCheckCofParNext(val)
  }

  useEffect(() => {
    getServiceData({ clusterId: colonyData.clusterId });
  }, []);

  return (
    <PageContainer header={{ title: '' }}>
      <Spin tip="Loading" size="small" spinning={loading}>
        <div className={styles.stepsLayout}>
          <div className={styles.stepsWrap}>
            <Steps
              direction="vertical"
              size="small"
              current={current}
              items={stepList}
              onChange={onChange}
            ></Steps>
          </div>
          <div className={styles.stepsContent}>
            {(current == 0 && serviceListData ) && <ChooseService serviceList={serviceListData} changeStatus={changeStatus} />}
            {/* { current == 1 && <ConfigSecurity selectListIds={selectListIds || []} setKerberosToParams={setKerberosToParams} />} */}
            { current == 1 && <AssignRoles serviceList={ getSelectedService() || [] } sourceServiceInfos={serviceInfos || []} setServiceInfosToParams={setServiceInfosToParams} checkAllRolesRules={checkAllRolesRules} parentLoading={loading} /> }
            { current == 2 && <ConfigService checkConfNext={checkConfNext} />}
            <div className={styles.stepBottomBtns}>
              <Button style={{ marginRight: '5px' }}               
                onClick={()=>{
                  if(current==0){
                    history.replace('/colony/serviceList');
                  }else{
                    setCurrent(current - 1);
                  }
                }}
              >
                {current == 0 ? '取消' : '上一步'}
              </Button>
              {contextHolder}
              <Button type="primary" 
                disabled={!checkNext()} 
                loading={loading || checkServiceLoading}
                onClick={async ()=>{
                  if(current == 0){
                    const params = {
                      "clusterId":colonyData.clusterId,
                      "stackId":colonyData.stackId,
                      "installStackServiceIds": selectListIds
                    }
                    setCheckServiceLoading(true)
                    checkService(params).then(async checkResult=>{
                      if(checkResult) {
                        sessionStorage.setItem('colonyData',JSON.stringify({...colonyData , selectedServiceList: getSelectedService()}) )
                        
                        const ids = serviceListData?.map(sItem=>{return sItem.id})
                        const nodesIds = await getRolesAll({ clusterId: colonyData.clusterId, stackServiceIds: ids})
                        initServiceInfos(nodesIds)
                        setCurrent(current + 1);
                      }
                    }).finally(()=>{
                      setCheckServiceLoading(false)
                    })
                  } 
                  // else if(current == 1){ // 配置安全下一步
                  //   setCurrent(current + 1);
                  //   initServiceInfos()

                  // } 
                  else if(current == 1){ // 分配角色下一步
                    // initServiceInfos()
                    // 校验节点
                    serviceInfos && checkAllRolesRules(serviceInfos)
                    setCurrent(current + 1);
                    
                  } else if(current == 2){ // 配置服务
                    const resultParams = await setPresetConfListToParams()
                    // console.log('---resultParams: ', resultParams);
                    
                    let initParams = {
                      ...resultParams, 
                      stackId: colonyData?.stackId,
                      clusterId: colonyData?.clusterId,
                    }
                    if(initParams.serviceInfos){
                      initParams.serviceInfos = resultParams?.serviceInfos?.map(sItem=>{                        
                        let roles = sItem.roles?.map(roleItem=>{
                          return {
                            nodeIds: roleItem.nodeIds,
                            stackRoleName: roleItem.stackRoleName
                          }
                        })
                        // console.log('--sItem.presetConfList:', sItem.presetConfList);
                        
                        return {
                          ...sItem,
                          presetConfList: sItem.presetConfList,
                          roles
                        }
                      })
                    }
                    // console.log('--initParams: ', initParams);
                    installService(initParams)

                  }else{
                    setCurrent(current + 1);
                  }
                }}
              >{ current == 2 ? '安装':'下一步'}</Button>
            </div>
          </div>
        </div>
      </Spin>
    </PageContainer>
  );
};

export default serviceAdd;
