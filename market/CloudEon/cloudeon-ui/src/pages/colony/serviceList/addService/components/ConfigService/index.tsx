
import styles from './index.less'
import React, { useState, useEffect, useRef } from 'react';
import { Menu, Form, Table, Button, Typography, Popconfirm, InputNumber, Input, Tooltip, Modal, Select, Slider, Switch, notification } from 'antd';
import { QuestionCircleFilled } from '@ant-design/icons';
import { getServiceConfAPI } from '@/services/ant-design-pro/colony';
import {cloneDeep} from 'lodash'
const { TextArea } = Input;

const ConfigService:React.FC<{checkConfNext: any}> = ( {checkConfNext} )=>{

    const [form] = Form.useForm()
    const [currentConfList, setCurrentConfList] = useState<any[]>();
    const [loading, setLoading] = useState(false);
    const [serviceId, setServiceId] = useState(null);
    const [editingKey, setEditingKey] = useState('');
    const [confData, setConfData] = useState({});
    const [addConfigForm] = Form.useForm()
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [customFileNames, setCustomFileNamesData] = useState<any>(); // 当前服务的配置文件
    const [currentNames, setCurrentNames] = useState<any[]>([]); // 当前服务的所有配置项名称，用来校验新增自定义配置的时候配置项名称重复
    const [fileGroupMap, setFileGroupMap] = useState<any>(); // 配置文件名和tag的json数据
    const [fileNameList, setFileList] = useState<any[]>(); // 配置文件名数组
    const [currentFile, setCurrentFile] = useState<any>(); // 当前选中的配置文件tab
    const [currentTag, setCurrentTag] = useState<any>(); // 当前选中的标签tag
    const [filterTableList, setFilterTableList] = useState<any[]>(); // 过滤后的table数据

    const [isEditMode, setIsEditMode] = useState(false); // 是否是编辑模式
    const [initConfList, setInitConfList] = useState<any[]>(); // 保存编辑前的数据，取消的时候可以恢复编辑前数据

    
    const getData = JSON.parse(sessionStorage.getItem('colonyData') || '{}')

    const serviceMenu = getData.selectedServiceList.map((sItem: { label: any; id: any; })=>{
        return {
            label: sItem.label,
            key: sItem.id + '',
        }
    })|| []

    const updateConfig = (value:any) => {
        // console.log('---updateConfig: ', value);
        sessionStorage.setItem('allConfData', JSON.stringify(cloneDeep(value)))
        setConfData(value)
    }


    useEffect(()=>{        
        if(serviceMenu && serviceMenu.length>0){
            setServiceId(serviceMenu[0].key)
            const params = {
                serviceId: serviceMenu[0].key,
                inWizard: true
            }
            getConfListData(params)
        }
    },[])
    

    const getConfListData = async (params: any) => {
        if(confData && confData[params.serviceId]){
            setInitConfList(cloneDeep(confData[params.serviceId]))
            setCurrentConfList(confData[params.serviceId])
            setFilterTableList(confData[params.serviceId])
            setCurrentNames(confData[params.serviceId].map((item: { name: any; })=>{return item.name}))
            const customFileNamesObj = JSON.parse(sessionStorage.getItem('customFileNamesObj') || '{}') 
            setCustomFileNamesData(customFileNamesObj[params.serviceId] || [])
            return
        }else{
            setLoading(true)
            const result: API.ConfList =  await getServiceConfAPI(params);
            setLoading(false)
            const confs = result?.data?.confs?.map(item=>{
                    return {
                        sourceValue: item.recommendExpression,
                        ...item,
                        value: item.valueType == "Switch" ? eval(item.recommendExpression||'false') : item.recommendExpression,
                        recommendExpression: item.valueType == "Switch" ? eval(item.recommendExpression||'false') : item.recommendExpression,  
                    }
                })
            const fileMap = result?.data?.fileGroupMap
            let files = []
            for(let key in fileMap){
                files.push(key)
            }
            // console.log('---files: ', fileMap, files);            
            setFileGroupMap(fileMap)
            setFileList(files)
            setCurrentFile(files[0])
            setInitConfList(confs)
            setCurrentConfList(confs)
            setFilterTableList(confs)
            setCurrentNames(confs?.map((item2)=>{return item2.name}) || [])
            const customFileNamesObj = JSON.parse(sessionStorage.getItem('customFileNamesObj') || '{}') 
            customFileNamesObj[params.serviceId] = result?.data?.customFileNames
            sessionStorage.setItem('customFileNamesObj', JSON.stringify(customFileNamesObj))
            setCustomFileNamesData(result?.data?.customFileNames || [])
            let confResult = {...(confData||{}),[params.serviceId]:confs}
            updateConfig(confResult)
        }
      };
    
    const handleMenuClick = () => {
        if(isEditMode){
            notification.warn({
                message: '温馨提示',
                description:<>请先处理当前编辑配置项</>,
                duration:3,
                style: {
                    width: 500
                }
              });
            return
        }
    }

    const onSelectService = function(value: any){
        // if(serviceId){// 保存切换前的数据
        //     let confResult = {...confData,[serviceId]:currentConfList}
        //     updateConfig(confResult)
        // }
        setServiceId(value.key)
        const params = {
            serviceId: value.key,
            inWizard: true
        }
        getConfListData(params)
    }

    // const isEditing = (record: Item) => record.name === editingKey;

    // const edit = (record: Item) => {
    //     form.setFieldsValue({ ...record });
    //     setEditingKey(record.name || '');
    // };
    
    // const cancel = () => {
    //     setEditingKey('');
    // };

    const handleDelete = (record: Item) => {
        const newData = cloneDeep(currentConfList||[]);
        const index = newData.findIndex(item => record.name === item.name);
        if(index > -1){
            newData.splice(index,1)
            setCurrentConfList(newData);
            deleteFilterData(record) // 处理过滤表格的数据
        }
    }

    const resetSource = (record: Item) => {
        record.value = record.recommendExpression
        setCurrentConfList(cloneDeep(currentConfList));
        form.setFieldValue(`${record.name}-value`, record.recommendExpression)
        // const row = {...record, recommendExpression: record.sourceValue}
        // const newData = cloneDeep(currentConfList||[]);
        // const index = newData.findIndex(item => row.name === item.name);
        // dealItemData(index,newData,row)// 处理保存编辑的逻辑
        // dealFilterData(record.name,row) // 处理过滤表格的数据
    }
    const save = async (key: string) => {        
        try {
            const row = (await form.validateFields()) as Item;
            const newData = cloneDeep(currentConfList||[])
            const index = newData.findIndex(item => key === item.name); // name是唯一值，所以用name做主键
            dealItemData(index,newData,row) // 处理保存编辑的逻辑
            dealFilterData(key,row) // 处理过滤表格的数据
        } catch (errInfo) {
            console.log('Validate Failed:', errInfo);
        }
    }
    // 处理总数据的编辑逻辑
    const dealItemData = (index:number,newData:any[],row:any) =>{
        if (index > -1) {
            const item = newData[index];
            newData.splice(index, 1, {
                ...item,
                ...row,
            });
            setCurrentConfList(newData);
            setEditingKey('');
        }else{
            newData.push(row);
            setCurrentConfList(newData);
            setEditingKey('');
        }
        let confResult = serviceId ? {...confData,[serviceId]:newData} : {...confData}
        updateConfig(confResult)
    }

    // 处理过滤表格数据（过滤文件配置、标签）的编辑逻辑
    const dealFilterData = (key:string, row:any) =>{
        if(filterTableList && filterTableList.length>0){
            const newData = cloneDeep(filterTableList)
            const index = newData?.findIndex(item => key === item.name);
            if (index > -1) {
                const item = newData[index];
                newData.splice(index, 1, {
                    ...item,
                    ...row,
                });
                setFilterTableList(newData)
            }else if(row.fileName == currentFile || row.confFile == currentFile || currentFile=="全部"){
                newData.push(row);
                setFilterTableList(newData)
            }
        }
    }
    // 处理过滤数据的删除逻辑
    const deleteFilterData = (record:Item) =>{
        const newData = cloneDeep(filterTableList||[]);
        const index = newData.findIndex(item => record.name === item.name);
        if(index > -1){
            newData.splice(index,1)
            setFilterTableList(newData);
        }
    }

    // 处理过滤数据怎么得到的，从总数据过滤得到
    const handleFiflterTableData = (tag: string, fileName: string, confListData?:any[])=>{
        const confList = confListData || currentConfList
        if(fileName == '全部' && !tag){
            return confList
        }else if(fileName == '全部' && tag){
            let arr = confList?.filter(item=>{
                return item.tag == tag
            })
            return arr
        }else if(fileName != "全部" && !tag){
            let arr = confList?.filter(item=>{
                return item.confFile == fileName
            })
            return arr
        }else{
            let arr = confList?.filter(item=>{
                return item.confFile == fileName && item.tag == tag
            })
            return arr
        }
    }

    // 修改某个值
    const actionOnChange = (e:any, record: Item) => {
        // console.log('--e: ', e);        
        record.value = e
        let cdata = cloneDeep(filterTableList || [])
        // console.log('--cdata: ', cdata); 
        cdata && setFilterTableList(cdata)
        const newData = cloneDeep(currentConfList||[]);
        const index = newData.findIndex(item => record.name === item.name); // name是唯一值，所以用name做主键
        dealItemData(index,newData,record) // 处理保存编辑的逻辑
    }

    interface Item {
        name: string;
        value?: any;
        options: any;
        unit: string;
        min: number;
        max: number;
        valueType: any;
        recommendExpression: string;
        sourceValue: string;
        isCustomConf: boolean;
        confFile:string;
        description: string
    }

    interface EditableCellProps extends React.HTMLAttributes<HTMLElement> {
        editing: boolean;
        dataIndex: string;
        title: any;
        inputType: 'InputNumber' | 'InputString' | 'Slider' | 'Switch' | 'Select';
        record: Item;
        index: number;
        children: React.ReactNode;
      }

    // const EditableCell: React.FC<EditableCellProps> = ({
    //     editing,
    //     dataIndex,
    //     title,
    //     inputType,
    //     record,
    //     index,
    //     children,
    //     ...restProps
    //   }) => {
    //     let inputNode = <Input addonAfter={record?.unit || ''} />
    //     // const inputNode = inputType === 'number' ? <InputNumber /> : <Input />;
    //     switch(inputType){
    //         case 'InputNumber':
    //             inputNode = <InputNumber max={record.max || Number.MAX_SAFE_INTEGER} min={record.min || Number.MIN_SAFE_INTEGER} addonAfter={record.unit || ''} />
    //             ;break;
    //         case 'InputString':;break;
    //         case 'Slider':
    //             inputNode = <Slider max={record.max || 100} min={record.min || 0} />
    //             ;break;
    //         case 'Switch':
    //             inputNode = <Switch />
    //             ;break;
    //         case 'Select':
    //             inputNode = 
    //                     <Select
    //                             // style={{ width: 120 }}
    //                             options={
    //                                 record.options.map((opItem: any)=>{
    //                                     return { value: opItem, label: opItem }
    //                                 })}
    //                         />
                        
    //             ;break;
    //     }
      
    //     return (
    //       <td {...restProps} className={(record?.sourceValue != record?.recommendExpression && !editing && !record.isCustomConf) ? styles.hasEdited:''}>
    //         {editing ? (
    //           <Form.Item
    //             name={dataIndex}
    //             style={{ margin: 0 }}
    //             rules={[
    //               {
    //                 required: true,
    //                 message: `请输入 ${title}!`,
    //               },
    //             ]}
    //           >
    //             {inputNode}
    //           </Form.Item>
    //         ) : (
    //           children
    //         )}
    //       </td>
    //     );
    //   };

    const configColumns = [
        {
            title: '配置项',
            dataIndex: 'name',
            editable: false,
            render: (_: any, record: Item)=>{
                return (
                    <span>
                        {record.name} 
                        {record.description? <Tooltip title={record.description}><QuestionCircleFilled style={{marginLeft:'5px'}} /></Tooltip>:''}
                    </span>
                )
            }
        },
        // {
        //     title: '配置类型',
        //     dataIndex: 'isCustomConf',
        //     width: 110,
        //     editable: false,
        //     render: (_: any, record: Item)=>{
        //         return (record.isCustomConf?'自定义配置':'预设配置')
        //     }
        // },
        {
            title: '值',
            dataIndex: 'recommendExpression',
            editable: true,
            render: (_: any, record: Item, index: any) => {
                // <span>{record.value}&nbsp;{record.unit?record.unit:''}</span>
                let inputNode = <Input style={{ width: '100%' }} onChange={(e)=>actionOnChange(e.target.value,record)} addonAfter={record?.unit || ''} />
                // const inputNode = inputType === 'number' ? <InputNumber /> : <Input />;
                switch(record.valueType){
                    case 'InputNumber':
                        inputNode = <InputNumber style={{ width: '100%' }} onChange={(e)=>actionOnChange(e,record)} max={record.max || Number.MAX_SAFE_INTEGER} min={record.min || Number.MIN_SAFE_INTEGER} addonAfter={record.unit || ''} />
                        ;break;
                    case 'InputString':;break;
                    case 'Slider':
                        inputNode = <Slider style={{ width: '100%' }} onChange={(e)=>actionOnChange(e,record)} max={record.max || 100} min={record.min || 0} />
                        ;break;
                    case 'Switch':
                        inputNode = <Switch checked={record.value} onChange={(e)=>actionOnChange(e,record)} />
                        ;break;
                    case 'Select':
                        inputNode = 
                                <Select onChange={(e)=>actionOnChange(e,record)}
                                        style={{ width: '100%' }}
                                        options={
                                            record.options.map((opItem: any)=>{
                                                return { value: opItem, label: opItem }
                                            })}
                                    />
                                
                        ;break;
                }
                  return  isEditMode?(
                    <Form.Item
                        name={`${record.name}-value`}
                        initialValue={record.value}
                        // key={record.name}
                        style={{ margin: 0 }}
                        rules={[
                        {
                            required: true,
                            message: `请输入值/选择值!`,
                        },
                        ]}
                    >
                        {inputNode}
                    </Form.Item>                    
                  ):(<> <span className={(record?.value != record?.recommendExpression && !record.isCustomConf) ? styles.hasEdited:''}>{record.valueType == "Switch" ?record.value.toString() : record.value}&nbsp;{record.unit?record.unit:''}</span> </>)
            },
        },
        {
            title: '配置文件',
            dataIndex: 'confFile',
            width: 200,
            editable: false,
        },
        // {
        //     title: '描述',
        //     dataIndex: 'description',
        //     editable: false,
        //     render: (_: any, record: Item)=>{
        //         return (record.description||'-')
        //     }
        // },
        {
            title: '操作',
            width: 150,
            dataIndex: 'operation',
            render: (_: any, record: Item, index:any) => {
                let resultDom1 = <></>
                let resultDom2 = <></>
                const formData = form.getFieldsValue(true)
                if(isEditMode){
                    if(formData[`${record.name}-value`] != record.recommendExpression && !record.isCustomConf){
                        resultDom1 = (<div style={{marginRight: '5px'}}>
                            <Popconfirm title="确定恢复到初始值吗?" onConfirm={()=>resetSource(record)}>
                                <a >恢复初始值</a>
                            </Popconfirm>
                        </div>)
                    }
                    if(record.isCustomConf){
                        resultDom2 = (
                            <div>
                                <Popconfirm title="确定删除吗?" onConfirm={()=>handleDelete(record)}>
                                    <a>删除</a>
                                </Popconfirm>
                            </div>
                            
                        )
                    }
                }
                return <>{resultDom1}{resultDom2}</>
            },
          },
    ]

    const mergedColumns = configColumns.map(col => {
        if (!col.editable) {
          return col;
        }
        return {
          ...col,
        };
    });

    // 点击"添加自定义配置"的确认按钮
      const handleOk = () => {
        // console.log(form.getFieldsValue());
        addConfigForm
          .validateFields()
          .then(async (values) => {
            const row = {...values, isCustomConf: true, recommendExpression: values.value};
            const newData = cloneDeep(currentConfList||[]);
            const index = newData.findIndex(item => values.name === item.name);
            if(index == -1){
                const params = [...currentNames, values.name]
                setCurrentNames(params)
            }
            dealItemData(index,newData,row)
            dealFilterData(values.name,row)
            setIsModalOpen(false)
            // const result: API.normalResult = await createNodeAPI({...values, clusterId: getData.clusterId})
            // if(result && result.success){
            //   message.success('新增成功');
            //   getNodeData({ clusterId: getData.clusterId });
            //   setIsModalOpen(false);
            //   form.resetFields()
            // }
          })
          .catch((err) => {
            console.log('err: ', err);
          });
    };

    const handleCancel = () => {
        addConfigForm.resetFields()
        setIsModalOpen(false);
    };

    // 初始化form数据
    const getFormInitData = (listdata: any[]) => {
        let formList:{ [key: string]: any } = {}
        for(let i =0;i<listdata.length;i++){
            formList[`${listdata[i].name}-value`] = listdata[i].value
        }
        return formList
    }

    // 批量修改保存按钮
    const submitEdit = async() => {
        // console.log('--currentConfList: ', currentConfList);
        setIsEditMode(false)
        checkConfNext(true)
        setInitConfList(cloneDeep(currentConfList))
        if(serviceId){
            let confResult = {...(confData||{}),[serviceId]:cloneDeep(currentConfList)}
            // console.log('---confResult:', confResult);
            updateConfig(confResult)
        }
    }

    return (
        <div className={styles.CSLayout}>
            <div className={styles.CSLeft}>
                {serviceMenu && serviceMenu[0] && serviceMenu[0].key && (
                    <Menu 
                        mode="inline" 
                        items={serviceMenu} 
                        defaultSelectedKeys={[serviceMenu[0].key]}
                        className={styles.CSLeftMenu} 
                        selectable={!isEditMode}
                        onClick={handleMenuClick}
                        onSelect={onSelectService} 
                    />
                )}
            </div>
            <div className={styles.CSRight}>
                <div className={styles.CSBtnWrap}>
                    {
                        !isEditMode?(
                        <>
                            <Button 
                                key="editBtn"
                                type="primary"
                                disabled={!serviceId || loading}
                                onClick={() => {
                                    setIsEditMode(true)
                                    checkConfNext(false)
                                }}
                            >
                                编辑配置项
                            </Button>
                        </>):(
                        <>
                            <Button 
                                key="saveBtn"
                                type="primary"
                                disabled={!serviceId || loading}
                                onClick={() => {
                                    // console.log('--currentConfList: ', currentConfList, form.getFieldsValue(true));
                                    submitEdit()
                                }}
                            >
                                保存
                            </Button>
                            <Button 
                                key="cancelBtn"
                                type="primary"
                                disabled={!serviceId || loading}
                                onClick={() => {
                                    setCurrentConfList(cloneDeep(initConfList))
                                    setFilterTableList(handleFiflterTableData(currentTag,currentFile,cloneDeep(initConfList)))
                                    // console.log('--initConfList:', initConfList, getFormInitData(initConfList||[]));
                                    form.setFieldsValue(getFormInitData(initConfList||[]))
                                    // form.resetFields();
                                    setIsEditMode(false)
                                    checkConfNext(true)
                                }}
                            >
                                取消
                            </Button>
                           
                            <Button 
                                key="addconfig"
                                type="primary"
                                disabled={!serviceId || loading}
                                onClick={() => {
                                    setIsModalOpen(true)
                                    addConfigForm.resetFields()
                                }}
                            >
                                添加自定义配置
                            </Button>
                        </>)
                    }
                    {/* <Button 
                        key="addconfig"
                        type="primary"
                        disabled={!serviceId}
                        onClick={() => {
                            setIsModalOpen(true)
                            addConfigForm.resetFields()
                        }}
                    >
                        添加自定义配置
                    </Button> */}
                </div>
                <div className={styles.fileTabLayout}>
                    <div className={styles.fileTabBar}>
                        {fileNameList?.map(item => {
                            return (
                                <div 
                                    key={item} 
                                    className={`${styles.fileTabItem} ${currentFile == item ? styles.active:''}`} 
                                    onClick={()=>{
                                        setCurrentTag('')
                                        setCurrentFile(item)
                                        setFilterTableList(handleFiflterTableData('',item))
                                    }}
                                >
                                    {item}
                                </div>
                            )
                        })}
                    </div>
                    <div className={styles.fileTabContent}>
                        <div className={styles.tagListWrap}>
                            {
                                fileGroupMap && fileGroupMap[currentFile] && fileGroupMap[currentFile].map((item:any)=>{
                                    return (
                                        <div 
                                            key={item} 
                                            className={`${styles.tagItem} ${currentTag == item ? styles.activeTag:''}`}
                                            onClick={()=>{
                                                setCurrentTag(item)
                                                setFilterTableList(handleFiflterTableData(item,currentFile))
                                            }}
                                        >
                                            {item}
                                        </div>
                                    )
                                })
                            }
                        </div>
                        <div className={styles.fileTabTable}>
                            <div>
                                <Form form={form} component={false}>
                                    <Table
                                        // components={{
                                        //     body: {
                                        //         cell: EditableCell,
                                        //     },
                                        // }}
                                        scroll={{
                                            y:600
                                        }}
                                        rowKey="name"
                                        pagination={false}
                                        // bordered
                                        loading={loading}
                                        dataSource={filterTableList} //{currentConfList}
                                        columns={mergedColumns}
                                        rowClassName="editable-row"
                                    />
                                </Form>
                            </div>
                        </div>
                    </div>
                </div>
                
            </div>
            <Modal
                key="addconfigmodal"
                title="添加自定义配置"
                forceRender={true}
                destroyOnClose={true}
                open={isModalOpen}
                onOk={handleOk}
                onCancel={handleCancel}
                // footer={null}
            >
                <div>
                <Form
                    form={addConfigForm}
                    key="addConfigForm"
                    name="添加自定义配置"
                    preserve={false}
                    labelCol={{ span: 6 }}
                    wrapperCol={{ span: 16 }}
                    initialValues={{ remember: true }}
                    // onFinish={onFinish}
                    autoComplete="off"
                >
                    <Form.Item
                        label="配置项"
                        name="name"
                        rules={[
                            { required: true, message: '请输入配置项!' },
                            { validator:(rule, value, callback)=>{
                                try {
                                    if(currentNames && currentNames.includes(value)){
                                        // throw new Error('该配置项已存在，请直接修改!');
                                        return Promise.reject(new Error('该配置项已存在，请直接修改!'));
                                    }
                                    return Promise.resolve();
                                  } catch (err) {
                                    callback(err);
                                  }
                            } }
                        ]}
                    >
                    <Input />
                    </Form.Item>

                    <Form.Item
                        label="值"
                        name="value"
                        rules={[{ required: true, message: '请输入值!' }]}
                    >
                    <Input />
                    </Form.Item>

                    <Form.Item
                        label="配置文件"
                        name="confFile"
                        rules={[{ required: true, message: '请选择配置文件!' }]}
                    >
                        <Select>
                            { customFileNames && customFileNames.map((fileNameItem: any) => {
                                return (<Select.Option key={fileNameItem} value={fileNameItem}>{fileNameItem}</Select.Option>)
                            }) }
                        </Select>
                    </Form.Item>

                    {/* <Form.Item
                        label="描述"
                        name="description"
                        rules={[]}
                    >
                    <TextArea rows={4} placeholder="请输入描述" />
                    </Form.Item> */}
                </Form>
                </div>
            </Modal>
        </div>
    )
}

export default ConfigService;