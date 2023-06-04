
import styles from './index.less'
import React, { useState, useEffect, useRef } from 'react';
import { Menu, Form, Table, Button, Typography, Popconfirm, InputNumber, Input, Tooltip, Modal, Select, Slider, Switch, message } from 'antd';
import { getServiceConfAPI, getListConfsAPI, saveServiceConfAPI } from '@/services/ant-design-pro/colony';
import { QuestionCircleFilled } from '@ant-design/icons';
const { TextArea } = Input;
import {cloneDeep} from 'lodash'

const ConfigService:React.FC<{serviceId: number}> = ( {serviceId} )=>{

    const [form] = Form.useForm()
    const [initConfList, setInitConfList] = useState<any[]>(); // 保存编辑前的数据，取消的时候可以恢复编辑前数据
    const [currentConfList, setCurrentConfList] = useState<any[]>();
    const [loading, setLoading] = useState(false);
    // const [serviceId, setServiceId] = useState(null);
    // const [editingKey, setEditingKey] = useState('');
    const [isEditMode, setIsEditMode] = useState(false); // 是否是编辑模式
    // const [confData, setConfData] = useState({});
    const [addConfigForm] = Form.useForm()
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [customFileNames, setCustomFileNamesData] = useState<any>(); // 当前服务的配置文件
    const [currentNames, setCurrentNames] = useState<any[]>([]); // 当前服务的所有配置项名称，用来校验新增自定义配置的时候配置项名称重复
    const [fileGroupMap, setFileGroupMap] = useState<any>(); // 配置文件名和tag的json数据
    const [fileNameList, setFileList] = useState<any[]>(); // 配置文件名数组
    const [currentFile, setCurrentFile] = useState<any>('全部'); // 当前选中的配置文件tab
    const [currentTag, setCurrentTag] = useState<any>(); // 当前选中的标签tag
    const [filterTableList, setFilterTableList] = useState<any[]>(); // 过滤后的table数据
    

    useEffect(()=>{
        getConfListData()
    },[])
    

    const getConfListData = async () => {
        const params = {serviceInstanceId: serviceId}
        setLoading(true)
        const result: API.ConfList =  await getListConfsAPI(params);
        setLoading(false)
        const confs = result?.data?.confs?.map((item,index)=>{
            // console.log('item: ', item, 'index:', index);
                return {
                    sourceValue: item.valueType == "Switch" ? eval(item.recommendExpression||'false') : item.recommendExpression,
                    ...item,
                    value: item.valueType == "Switch" ? eval(item.value||'false') : item.value,
                    recommendExpression: item.valueType == "Switch" ? eval(item.recommendExpression||'false') : item.recommendExpression,
                }
            })
        setInitConfList(cloneDeep(confs))
        setCurrentConfList(cloneDeep(confs))
        setFilterTableList(handleFiflterTableData(currentTag,currentFile,cloneDeep(confs)))
        setCurrentNames(confs?.map((item2)=>{return item2.name}) || [])
        const customFileNames = result?.data?.customFileNames
        setCustomFileNamesData(customFileNames)
        const fileMap = result?.data?.fileGroupMap
        let files = []
        for(let key in fileMap){
            files.push(key)
        }
        setFileGroupMap(fileMap)
        setFileList(files)
        setCurrentFile(files[0])
    };

    const handleDelete = (record: Item) => {
        // 处理总配置数据的删除
        const newData = cloneDeep(currentConfList || []);
        const index = newData.findIndex(item => record.name === item.name);
        if(index > -1){
            newData.splice(index,1)
            console.log('--newData: ', newData);
            setCurrentConfList(newData);
        }
        // 处理当前过滤数据的删除
        const filterData = cloneDeep(filterTableList || []);
        const filterIndex = filterData.findIndex(item => record.name === item.name);
        if(filterIndex > -1){
            filterData.splice(filterIndex,1)
            setFilterTableList(filterData)
        }
    }

    const resetSource = (record: Item, index:number) => {
        // console.log('---record: ', record);
        record.value = record.recommendExpression
        setCurrentConfList(cloneDeep(currentConfList));
        // console.log('---currentConfList: ', currentConfList);
        form.setFieldValue(`${record.name}-value`, record.recommendExpression)
    }

    const submitEdit = async() => {
        const curConfList = saveCurrentTable() || currentConfList; // 获取最新修改后的数据
        const presetConfList = curConfList?.filter(f=> !f.isCustomConf).map(item=>{
            return {
                name: item.name,
                value: item.value,
                id: item.id,
            }
        })
        const customConfList = curConfList?.filter(f=> f.isCustomConf).map(item=>{
            return {
                name: item.name,
                value: item.value,
                confFile: item.confFile,
                id: item.id,
            }
        })

        const params = {
            serviceInstanceId: serviceId,
            presetConfList,
            customConfList
        }
        console.log('---params:', params);
        
        const result = await saveServiceConfAPI(params)
        if(result && result.success){
            message.success('保存成功')
            setIsEditMode(false)
            getConfListData()
        }
    }
    interface Item {
        id?: number;
        value?: any;
        options: any;
        unit: string;
        min: number;
        max: number;
        valueType: any;
        name: string;
        recommendExpression: string;
        sourceValue: string;
        isCustomConf: boolean;
        confFile:string;
        description: string
    }

    const actionOnChange = (e:any, record: Item) => {
        // console.log('--record: ', e.target.value);        
        record.value = e
        let cdata = cloneDeep(filterTableList || [])
        cdata && setFilterTableList(cdata)
    }

    const configColumns = [
        {
            title: '配置项',
            dataIndex: 'name',
            editable: false,
            // width: 300,
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
            dataIndex: 'value',
            editable: true,
            render: (_: any, record: Item, index: any) => {
                // <span>{record.value}&nbsp;{record.unit?record.unit:''}</span>
                let inputNode = <Input onChange={(e)=>actionOnChange(e.target.value,record)} addonAfter={record?.unit || ''} />
                // const inputNode = inputType === 'number' ? <InputNumber /> : <Input />;
                switch(record.valueType){
                    case 'InputNumber':
                        inputNode = <InputNumber onChange={(e)=>actionOnChange(e,record)} max={record.max || Number.MAX_SAFE_INTEGER} min={record.min || Number.MIN_SAFE_INTEGER} addonAfter={record.unit || ''} />
                        ;break;
                    case 'InputString':;break;
                    case 'Slider':
                        inputNode = <Slider onChange={(e)=>actionOnChange(e,record)} max={record.max || 100} min={record.min || 0} />
                        ;break;
                    case 'Switch':
                        inputNode = <Switch checked={record.value} onChange={(e)=>actionOnChange(e,record)} />
                        ;break;
                    case 'Select':
                        inputNode = 
                                <Select onChange={(e)=>actionOnChange(e,record)}
                                        style={{ width: 120 }}
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
                  ):(<> <span>{record.valueType == "Switch" ?record.value.toString() : record.value}&nbsp;{record.unit?record.unit:''}</span> </>)
            },
        },
        {
            title: '配置文件',
            dataIndex: 'confFile',
            // width: 200,
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
            width: 180,
            dataIndex: 'operation',
            render: (_: any, record: Item, index:any) => {
                let resultDom1 = <></>
                let resultDom2 = <></>
                const formData = form.getFieldsValue(true)
                if(isEditMode){
                    if(formData[`${record.name}-value`] != record.recommendExpression && !record.isCustomConf){
                        resultDom1 = (<div style={{marginRight: '5px'}}>
                            <Popconfirm title="确定恢复到初始值吗?" onConfirm={()=>resetSource(record,index)}>
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
            // dealItemData(index,newData,row)
            newData.push(row)
            setCurrentConfList(newData);
            if(values.confFile == currentFile){
                let filterData = cloneDeep(filterTableList||[])
                filterData?.push(row)
                setFilterTableList(filterData)
            }
            setIsModalOpen(false)
          })
          .catch((err) => {
            console.log('err: ', err);
          });
    };
    
    const handleCancel = () => {
        addConfigForm.resetFields()
        setIsModalOpen(false);
    };

    const getFormInitData = (listdata: any[]) => {
        let formList:{ [key: string]: any } = {}
        for(let i =0;i<listdata.length;i++){
            formList[`${listdata[i].name}-value`] = listdata[i].value
        }
        return formList
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

    const saveCurrentTable = () => {
        if(filterTableList && currentConfList){
            let newData = cloneDeep(currentConfList)
            for(let i = 0; i < filterTableList?.length; i++){
                let filterItem = filterTableList[i]
                let index = currentConfList.findIndex(item=>{ return item.name == filterItem.name})
                if(index > -1){
                    newData[index] = filterItem
                }else{
                    newData.push(filterItem)
                }
            }
            setCurrentConfList(newData);
            return newData
        }
        return ''
    }

    return (
        <div className={styles.ConfigTabLayout}>
            <div className={styles.CSRight}>
                <div className={styles.btnsBar}>
                    {
                        !isEditMode?(
                        <>
                            <Button 
                                key="editBtn"
                                type="primary"
                                disabled={!serviceId || loading}
                                onClick={() => {
                                    setIsEditMode(true)
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
                                    console.log('--currentConfList: ', currentConfList, form.getFieldsValue(true));
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
                                    setIsEditMode(false)
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
                                        saveCurrentTable()
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
                                                saveCurrentTable()
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
                                    scroll={{
                                        y:600
                                    }}
                                    rowKey="name" // 以为新增的自定义配置项没有id，所以不能使用id
                                    pagination={false}
                                    bordered
                                    loading={loading}
                                    dataSource={filterTableList}
                                    columns={mergedColumns}
                                    rowClassName="editable-row"
                                />
                            </Form>
                            </div>
                        </div>
                    </div>
                </div>
                {/* <div>
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
                        bordered
                        loading={loading}
                        dataSource={currentConfList}
                        columns={mergedColumns}
                        rowClassName="editable-row"
                    />
                </Form>
                </div> */}
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
                                    console.log(err);
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