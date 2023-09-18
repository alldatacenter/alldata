/* eslint-disable no-nested-ternary */
/* eslint-disable no-shadow */
import React, {
    useRef, useState, useEffect,
} from 'react';
import {
    Tabs, Tag, Table, Modal, Form, Button, Select, message, Spin, Pagination, DatePicker, Dropdown, Drawer,
} from 'antd';
import { RightOutlined, PlusOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import './index.less';
import { setEditorFn, useEditorActions, useEditorContextState } from '@Editor/store/editor';
import { IF } from '@Editor/common';
import { useIntl } from 'react-intl';
import dayjs, { Dayjs } from 'dayjs';
import useRequest from '../../hooks/useRequest';
import { useWatch, usePersistFn } from '@/common';
import {
    dataBaseTabs, dataBaseCol, tableTabs, tableCol, colTabs, colCol,
    Tab, Col,
} from './option';
import Detail from './Detail';
import { useMetricModal } from '../MetricModal';
import DashBoard from './Detail/dashBoard';
import ProfileContent from './profileContent';
import Schedule from '@/view/Main/HomeDetail/Jobs/components/Schedule';
import SearchForm from './SearchForm';
import store from '@/store';
import { useLogger } from '@/view/Main/HomeDetail/Jobs/useLogger';

type DIndexProps = {
    onShowModal?: (...args: any[]) => any;
    afterClose?: (...args: any[]) => any;
}
type RangeValue = [Dayjs | null, Dayjs | null] | null;

const Index = ({ onShowModal, afterClose }:DIndexProps) => {
    const [{ selectDatabases, workspaceId, id }] = useEditorContextState();
    const { Render: RenderModal } = useMetricModal();
    const { $http } = useRequest();
    const intl = useIntl();
    const { Render: RenderLoggerModal, show: showLoggerModal } = useLogger({});
    const { RangePicker } = DatePicker;
    const searchForm = Form.useForm()[0];
    useEffect(() => {
        getTagList();
    }, []);
    const [detailName, setName] = useState({
        name: intl.formatMessage({ id: 'datasource' }),
        colName: '',
        colKeys: '',
    });
    const [tableItems, setTableItems] = useState<Tab[]>([]);
    const [activeTableKey, setActivedTabKey] = useState('');
    const [rowKey, setRowKey] = useState('uuid'); // 表格的key
    const [detailData, setDetailData] = useState<{
        name: string;
        uuid: string;
        type: string;
        metrics: string;
        usages: string;
        tags: string;
        updateTime: string;
        tables: string;
        columns?:string;
    }>({
        name: '',
        uuid: '',
        type: '',
        metrics: '',
        usages: '',
        tags: '',
        updateTime: '',
        tables: '',
    });
    const [tagList, setTagList] = useState<{
        uuid:string,
        name?:string
    }[]>([]);
    const getTagList = async () => {
        const res = await $http.get(`/catalog/tag/list-in-workspace/${workspaceId}`);
        setTagList(res);
    };
    const [currentList, setCurrentList] = useState<{name:string;id:string;uuid:string}[]>([]);
    const getCurrentTagList = async (entityUUID:string) => {
        const res = await $http.get(`/catalog/tag/list-in-entity/${entityUUID}`);
        setCurrentList(res || []);
    };
    // 获取表格数据
    const [columns, setColums] = useState<Col[]>([]);
    const [tableData, setTableData] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
    const [isScheduleOpen, setisScheduleOpen] = useState(false);
    const [currentIndex, setCurentIndex] = useState(0);
    const [form] = Form.useForm();
    const handleOk = async () => {
        form.validateFields().then((value) => {
            createTag(value);
        }).catch(() => {

        });
    };

    const handleCancel = () => {
        form.resetFields();
        setIsModalOpen(false);
    };
    const currentColList = useRef<Col[][]>([]);

    const changeTabKey = (key:string) => {
        const len = selectDatabases.length;
        setColums(currentColList.current[+key]);
        const { uuid } = selectDatabases[len - 1];
        setTableData([]);
        switch (tableItems[+key].name) {
            case 'Profile':
                setTimer(null);
                getProfile(uuid);
                getProfileTable(uuid);
                setRowKey('uuid');
                break;
            case 'Tables':
                setPageInfo({
                    pageNumber: 1,
                    pageSize: 10,
                    total: 0,
                });
                setTotal(0);
                setRowKey('uuid');
                break;
            case 'Schema Changes':
                setPageInfo({
                    pageNumber: 1,
                    pageSize: 10,
                    total: 0,
                });
                setTotal(0);
                setRowKey('uuid');
                break;
            case 'Column':
                setPageInfo({
                    pageNumber: 1,
                    pageSize: 10,
                    total: 0,
                });
                setTotal(0);
                setRowKey('uuid');
                break;
            case 'Metrics':
                setPageInfo({
                    pageNumber: 1,
                    pageSize: 10,
                    total: 0,
                });
                setTotal(0);
                setRowKey('id');
                break;
            case 'Issues':
                setPageInfo({
                    pageNumber: 1,
                    pageSize: 10,
                    total: 0,
                });
                setTotal(0);
                setRowKey('id');
                break;
            default:
        }
        setActivedTabKey(key);
    };
    const renderNext = (record:any, currentActiveIndex:number) => {
        let len = currentActiveIndex + 1;
        while (len < selectDatabases.length) {
            selectDatabases.splice(len, 1);
            len += 1;
        }
        selectDatabases[currentActiveIndex + 1] = {
            name: record.name,
            uuid: record.uuid,
        };
        fns.setEditorFn({ selectDatabases: [...selectDatabases] });
    };
    const renderMetrics = (record:any) => {
        store.dispatch({
            type: 'save_datasource_modeType',
            payload: 'quality',
        });
        store.dispatch({
            type: 'save_datasource_dsiabledEdit',
            payload: {
                isTable: currentIndex === 2,
            },
        });
        setMetricid(record.id);
        setMetricName(record.name);
    };
    const onSeeView = (content:string, id:string) => {
        issueId.current = id;
        setIsIssueModalOpen(true);
        setIssuesTableData(JSON.parse(content).map((item:string) => {
            const [key, value] = item.split(':');
            return {
                key, value,
            };
        }));
    };
    // 初始化数据和重新加载数据
    const initData = (index:number) => {
        setActivedTabKey('0');
        setTableData([]);
        // eslint-disable-next-line no-unused-expressions
        index > 0 && getDetail(selectDatabases[index].uuid);
        switch (index) {
            case 1:
                currentColList.current = dataBaseCol;
                setTableItems(dataBaseTabs);
                dataBaseCol[0][0].onCell = (record) => ({
                    onClick: () => renderNext(record, 1),
                });
                setColums(dataBaseCol[0]);
                getTableList(selectDatabases[index].uuid);
                getCurrentTagList(selectDatabases[index].uuid);
                setName({
                    name: intl.formatMessage({ id: 'job_database' }),
                    colName: intl.formatMessage({ id: 'job_table_quantity' }),
                    colKeys: 'tables',
                });
                break;
            case 2:
                setTimer(null);
                getProfile(selectDatabases[index].uuid);
                getProfileTable(selectDatabases[index].uuid);
                currentColList.current = tableCol;
                getCurrentTagList(selectDatabases[index].uuid);
                setTableItems(tableTabs);
                tableCol[1][0].onCell = (record) => ({
                    onClick: () => renderNext(record, 2),
                });
                tableCol[2][0].onCell = (record) => ({
                    onClick: () => renderMetrics(record),
                });
                tableCol[2][4].render = (_: any, { id }: any) => <a onClick={() => onRun(id)}>{intl.formatMessage({ id: 'jobs_run' })}</a>;
                tableCol[4][4].render = (_: any, { content, id }:{content:string, id:string}) => <a onClick={() => onSeeView(content, id)}>{intl.formatMessage({ id: 'common_view' })}</a>;
                setColums(tableCol[0]);
                setName({
                    name: intl.formatMessage({ id: 'job_table' }),
                    colName: `${intl.formatMessage({ id: 'job_column_quantity' })}`,
                    colKeys: 'columns',
                });
                break;
            case 3:
                currentColList.current = colCol;
                setTableItems(colTabs as never);
                setColums(colCol[0] as never[]);
                colCol[0][0].onCell = (record) => ({
                    onClick: () => renderMetrics(record),
                });
                colCol[0][4].render = (_: any, { id }: any) => <a onClick={() => onRun(id)}>{intl.formatMessage({ id: 'jobs_run' })}</a>;
                colCol[1][4].render = (_: any, { content, id }: {content:string, id:string}) => <a onClick={() => onSeeView(content, id)}>{intl.formatMessage({ id: 'common_view' })}</a>;
                getMetric(selectDatabases[index].uuid);
                setName({
                    name: intl.formatMessage({ id: 'job_column' }),
                    colName: `${intl.formatMessage({ id: 'job_alter_quantity' })}`,
                    colKeys: 'tables',
                });
                break;
            default:
        }
    };

    useEffect(() => {
        const len = selectDatabases.length;
        initData(len - 1);
        setMetricid('');
        if (len === 1) return;
        setCurentIndex(len - 1);
    }, [selectDatabases]);

    useEffect(() => {
        initData(currentIndex);
    }, [currentIndex]);

    const [total, setTotal] = useState(0);
    // 获取表格数
    const getTableList = async (uuid:string) => {
        setLoading(true);
        $http.get('/catalog/page/table-with-detail', {
            upstreamUuid: uuid,
            ...pageInfo,
            name: searchForm.getFieldsValue()?.name || '',
        }).then((res) => {
            setTableData(res.records || []);
            setTotal(res.total);
        }).finally(() => {
            setLoading(false);
        });
    };
    // 获取schemachange
    const getSchemaChanges = async (uuid:string) => {
        setLoading(true);
        $http.get('/catalog/page/schema-change', {
            uuid,
            ...pageInfo,
        }).then((res) => {
            setTableData(res.records || []);
            setTotal(res.total);
        }).finally(() => {
            setLoading(false);
        });
    };
    const [pageInfo, setPageInfo] = useState({
        pageSize: 10,
        pageNumber: 1,
        total: 0,
    });
    useWatch([pageInfo], () => {
        const { uuid } = selectDatabases[selectDatabases.length - 1];
        const name = tableItems[+activeTableKey]?.name;
        switch (name) {
            case 'Tables':
                getTableList(uuid);
                break;
            case 'Column':
                getColList(uuid);
                break;
            case 'Schema Changes':
                getSchemaChanges(uuid);
                break;
            case 'Metrics':
                getMetric(uuid);
                break;
            case 'Issues':
                getIssue(uuid);
                break;
            default:
        }
    }, { immediate: true });
    const onChange = async (pageNumber:number, pageSize:number) => {
        setPageInfo({
            pageNumber,
            pageSize,
            total: pageInfo.total,
        });
    };
    // 获取列
    const getColList = async (uuid:string) => {
        await $http.get('/catalog/page/column-with-detail', {
            ...pageInfo,
            upstreamUuid: uuid,
            name: searchForm.getFieldsValue()?.name || '',
        }).then((res) => {
            setTableData(res.records || []);
            setTotal(res.total);
        }).finally(() => {
            setLoading(false);
        });
    };
    // 获取Mertric
    const getMetric = async (uuid:string) => {
        setLoading(true);
        await $http.get('/catalog/page/entity/metric', {
            ...pageInfo,
            uuid,
        }).then((res) => {
            setTableData(res.records || []);
        }).finally(() => {
            setLoading(false);
        });
    };
    // 获取profile
    const getProfile = async (uuid:string) => {
        await $http.get(`/catalog/profile/table/${uuid}`, {
            uuid,
        }).then((res) => {
            setTableData(res.columnProfile || []);
        }).finally(() => {
        });
    };
    const getIssue = async (uuid:string) => {
        setLoading(true);
        await $http.get('/catalog/page/entity/issue', {
            uuid,
            ...pageInfo,
        }).then((res) => {
            setTableData(res.records || []);
        }).finally(() => {
            setLoading(false);
        });
    };
    const [option, setOption] = useState<any>(null);
    const [timer, setTimer] = useState<RangeValue>(null);
    useEffect(() => {
        getProfileTable(selectDatabases[currentIndex]?.uuid);
    }, [timer]);
    const getProfileTable = async (uuid:string) => {
        await $http.get('/catalog/profile/table/records', {
            uuid,
            startTime: timer && timer[0] ? dayjs(timer[0]).format('YYYY-MM-DD') : undefined,
            endTime: timer && timer[1] ? dayjs(timer[1]).format('YYYY-MM-DD') : undefined,
        }).then((res) => setOption({
            title: {
                text: 'Table Records',
                left: 'center',
            },
            color: ['#ffd56a'],
            grid: {
                left: 40,
                top: 50,
                right: 0,
                bottom: 50,
            },
            tooltip: {},
            xAxis: {
                type: 'category',
                data: res.map((item: {
                        datetime: string;
                    }) => item.datetime),
            },
            yAxis: {
                type: 'value',
            },
            series: [
                {
                    data: res.map((item: {
                            value: number;
                        }) => item.value),
                    type: 'line',
                    smooth: true,
                },
            ],
        }));
    };

    const [chooesColList, setChooesColList] = useState([]);
    const [chooesColModalOpen, setChooesColModalOpen] = useState(false);
    const [colCheckList, setColCheckList] = useState<string[]>([]);
    const chooseColumns = [{
        title: intl.formatMessage({ id: 'job_column_Name' }),
        dataIndex: 'name',
        key: 'value',
    }];
    const historyColumns = [{
        title: intl.formatMessage({ id: 'jobs_execution_time' }),
        dataIndex: 'updateTime',
        key: 'updateTime',
    }, {
        title: intl.formatMessage({ id: 'jobs_status' }),
        dataIndex: 'status',
        key: 'status',
    }, {
        title: intl.formatMessage({ id: 'common_action' }),
        dataIndex: 'action',
        key: 'action',
        width: '100px',
        render: (_: any, data: any) => (
            <a
                onClick={() => {
                    showLoggerModal(data);
                }}
                className="text-underline"
            >
                {intl.formatMessage({ id: 'jobs_task_log_btn' })}
            </a>
        ),
    }];
    const runProfileGetCol = async () => {
        const res = await $http.get(`/catalog/list/column-with-detail/${selectDatabases[currentIndex]?.uuid}`);
        setChooesColList(res);
        const resCheck = await $http.get(`/catalog/profile/selected-columns/${selectDatabases[currentIndex]?.uuid}?uuid=${selectDatabases[currentIndex]?.uuid}`);
        setColCheckList(resCheck);
        setChooesColModalOpen(true);
    };
    const runProfile = async () => {
        await $http.post('/catalog/profile/execute-select-columns', {
            selectAll: chooesColList.length === colCheckList.length,
            selectedColumnList: colCheckList,
            uuid: selectDatabases[currentIndex]?.uuid,
        }).then(() => {
            setChooesColModalOpen(false);
            message.success(intl.formatMessage({ id: 'common_success' }));
        });
    };
    const changeIndex = (index:number) => {
        if (!index) return;
        let len = selectDatabases.length - 1;
        while (len > index) {
            selectDatabases.splice(len, 1);
            len -= 1;
        }
        fns.setEditorFn({ selectDatabases: [...selectDatabases] });
        setCurentIndex(index);
    };
    // eslint-disable-next-line react/no-unstable-nested-components
    const Tab = () => (
        <>
            {
                selectDatabases.map((item:any, index:number) => (
                    <React.Fragment key={item.name + item.uuid}>
                        <span className={index === 0 ? 'text-decoration-none' : index === currentIndex ? 'cp ligth' : 'cp'} onClick={() => changeIndex(index)}>{item?.name}</span>
                        {index === selectDatabases.length - 1 ? '' : <RightOutlined /> }
                    </React.Fragment>
                ))
            }
        </>
    );
    const getDetail = async (uuid: string) => {
        const len = selectDatabases.length;
        if (len === 1) return;
        let url = '';
        switch (len) {
            case 2:
                url = '/catalog/detail/database/';
                break;
            case 3:
                url = '/catalog/detail/table/';
                break;
            case 4:
                url = '/catalog/detail/column/';
                break;
            default:
                break;
        }
        const res = await $http.get(`${url}${uuid}`);
        setDetailData(res || {});
    };
    // eslint-disable-next-line no-unused-expressions
    afterClose && afterClose(() => {
        // eslint-disable-next-line no-unused-expressions
        tableItems[+activeTableKey].name === 'Metrics' && getMetric(selectDatabases[currentIndex].uuid);
    });

    const onSearch = usePersistFn(() => {
        setPageInfo({ ...pageInfo, pageNumber: 1 });
    });

    const onFieldClick = (index?:number, name?:string, uuid?:string) => {
        const $record = {
            parameterItem: {
                metricParameter: {
                    database: selectDatabases[1]?.name,
                    table: index === 2 ? name : selectDatabases[2]?.name,
                    column: !index ? (selectDatabases[3]?.name || '') : (index && index > 2) ? (index === 3 ? name : '') : '',
                },
            },
        };
        if (onShowModal) {
            store.dispatch({
                type: 'save_datasource_modeType',
                payload: 'quality',
            });
            store.dispatch({
                type: 'save_datasource_entityUuid',
                payload: uuid || selectDatabases[currentIndex].uuid,
            });
            store.dispatch({
                type: 'save_datasource_dsiabledEdit',
                payload: {
                    isTable: currentIndex === 2,
                },
            });
            onShowModal({
                parameter: [$record.parameterItem],
                parameterItem: $record.parameterItem,
            });
        }
    };
    const createTag = async (data: {tagUUID:string}) => {
        try {
            await $http.post(`catalog/tag/entity-tag?tagUUID=${data.tagUUID}&entityUUID=${selectDatabases[currentIndex]?.uuid}`, {
                ...data,
                entityUUID: selectDatabases[currentIndex]?.uuid,
            });
            message.success(intl.formatMessage({ id: 'common_success' }));
            getCurrentTagList(selectDatabases[currentIndex].uuid);
            handleCancel();
            getDetail(selectDatabases[currentIndex].uuid);
        } catch (error) {
        }
    };
    const onClose = async (e: React.MouseEvent<HTMLElement, MouseEvent>, data:{uuid:string}) => {
        try {
            e.preventDefault();
            await $http.delete(`catalog/tag/entity-tag?tagUUID=${data.uuid}&entityUUID=${selectDatabases[currentIndex]?.uuid}`);
            message.success(intl.formatMessage({ id: 'common_success' }));
            getCurrentTagList(selectDatabases[currentIndex].uuid);
            getDetail(selectDatabases[currentIndex].uuid);
        } catch (error) {
        }
    };
    const [metricsId, setMetricid] = useState('');
    const [metricsName, setMetricName] = useState('');
    const fns = useEditorActions({ setEditorFn });
    const [loading, setLoading] = useState(false);
    const onRun = async (id: string) => {
        try {
            setLoading(true);
            await $http.post(`/job/execute/${id}`);
            message.success('Run Success');
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const onDelete = async (id:string) => {
        try {
            setLoading(true);
            await $http.delete(`/job/${id}`);
            message.success('Delete Success');
            setMetricid('');
            getDetail(selectDatabases[currentIndex].uuid);
            let key = '';
            tableItems.some((item, index) => {
                if (item.name === 'Metrics') {
                    key = `${index}`;
                    return true;
                }
                return false;
            });
            changeTabKey(key);
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };
    const [issuesTableData, setIssuesTableData] = useState([]);
    const issuesColumns = [{
        title: '',
        dataIndex: 'key',
        key: 'key',
    }, {
        title: '',
        dataIndex: 'value',
        key: 'value',
    }];
    const issueId = useRef('');
    const setIssue = async (type:string) => {
        await $http.post('/catalog/issue/update-status', {
            id: issueId.current,
            status: type,
        });
        setIsIssueModalOpen(false);
        message.success(intl.formatMessage({ id: 'common_success' }));
        getIssue(selectDatabases[currentIndex].uuid);
    };
    const onRefresh = async () => {
        await $http.post('catalog/refresh', {
            database: selectDatabases[1]?.name,
            datasourceId: id,
            table: selectDatabases[2] ? selectDatabases[2]?.name : '',
        });
        message.success(intl.formatMessage({ id: 'common_success' }));
    };
    const [detailKey, setDetailTabKey] = useState('1');
    const changeDetailTabKey = (value:string) => {
        setDetailTabKey(value);
    };
    const detailRef = useRef<{
        runsRef:any
    }>();
    const refreshHistoryList = () => {
        detailRef.current?.runsRef?.current.getData();
    };

    const [openDrawer, setOpenDrawer] = useState(false);
    const [historyList, setHistoryList] = useState([]);
    const [historytotal, setHistorytotal] = useState(0);
    const [historyLoading, setHistoryLoading] = useState(false);
    const onPageChange = (page:number, pageSize:number) => {
        OpenDrawer(page, pageSize);
    };
    const OpenDrawer = async (page:number, pageSize:number) => {
        setHistoryList([]);
        setHistoryLoading(true);
        setOpenDrawer(true);
        const res = await $http.get('catalog/profile/execute/history', {
            pageNumber: page,
            pageSize,
            uuid: selectDatabases[selectDatabases.length - 1].uuid,
        });
        setHistoryLoading(false);
        setHistorytotal(res.total || 0);
        setHistoryList(res.records || []);
    };
    const onDrawerClose = () => {
        setOpenDrawer(false);
    };
    return (
        <div className="dv-database">
            <IF visible={currentIndex > 0}>
                <p className="dv-database-title">
                    <Tab />
                </p>
                <IF visible={!metricsId}>
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: '10px',
                    }}
                    >
                        <div>
                            <div className="dv-database-name">
                                <span>{ detailName.name }</span>
                                <span>{selectDatabases[currentIndex]?.name}</span>
                            </div>
                            {/* 标签列表 */}
                            {
                                currentList.map((item) => <Tag onClose={(e) => onClose(e, item)} closable key={item.id} color="#FF2D55">{item.name}</Tag>)
                            }
                            <Tag
                                onClick={() => setIsModalOpen(true)}
                                style={{
                                    background: '#fff',
                                    borderStyle: 'dashed',
                                    cursor: 'pointer',
                                }}
                            >
                                <PlusOutlined />
                                {intl.formatMessage({ id: 'common_add' })}
                            </Tag>
                        </div>
                        <div>
                            <IF visible={currentIndex !== 3}>
                                <Button
                                    onClick={onRefresh}
                                    style={{ marginRight: '10px' }}
                                >
                                    {intl.formatMessage({ id: 'job_log_refresh' })}
                                </Button>
                            </IF>
                            <IF visible={currentIndex > 1}>
                                <Button
                                    onClick={() => onFieldClick()}
                                >
                                    {intl.formatMessage({ id: 'jobs_add_metric' })}
                                </Button>
                            </IF>
                            <IF visible={currentIndex === 2}>
                                <Dropdown.Button
                                    menu={{
                                        items: [{
                                            key: '1',
                                            label: intl.formatMessage({ id: 'job_profile_schedule' }),
                                        }, {
                                            key: '2',
                                            label: intl.formatMessage({ id: 'job_profile_execution_history' }),
                                        }],
                                        onClick: (data) => {
                                            if (data.key === '1') {
                                                setisScheduleOpen(true);
                                            } else {
                                                OpenDrawer(1, 10);
                                            }
                                        },
                                    }}
                                    onClick={() => runProfileGetCol()}
                                    style={{ marginLeft: '10px', display: 'inline' }}
                                >
                                    {intl.formatMessage({ id: 'jobs_run_profile' })}

                                </Dropdown.Button>
                            </IF>
                        </div>

                    </div>
                    <div className="dv-database-label">
                        <div>
                            <p>{intl.formatMessage({ id: 'jobs_last_scan_time' })}</p>
                            <p>
                                <Tag color="success">{detailData.updateTime}</Tag>
                            </p>
                        </div>
                        <div>
                            <p>{detailName.colName}</p>
                            <p>
                                <Tag color="processing">{detailData[detailName.colKeys as 'columns' | 'tables']}</Tag>
                            </p>
                        </div>
                        <div>
                            <p>
                                {intl.formatMessage({ id: 'jobs_number_of_labels' })}
                            </p>
                            <p>
                                <Tag color="default">{detailData.tags}</Tag>
                            </p>
                        </div>
                        <div>
                            <p>
                                {intl.formatMessage({ id: 'jobs_number_of_rules' })}
                            </p>
                            <p>
                                <Tag color="warning">{detailData.metrics}</Tag>
                            </p>
                        </div>
                        <div>
                            <p>
                                {intl.formatMessage({ id: 'jobs_use_heat' })}
                            </p>
                            <p>
                                <Tag color="error">{detailData.usages}</Tag>
                            </p>
                        </div>
                    </div>
                    <Spin spinning={loading}>
                        <div className="dv-database-table">
                            <Tabs
                                size="small"
                                activeKey={activeTableKey}
                                items={tableItems}
                                onChange={changeTabKey}
                            />
                            <div style={{
                                height: 'calc(100vh - 370px)',
                                overflow: 'auto',
                            }}
                            >
                                {
                                    tableItems[+activeTableKey]?.name === 'Profile' ? (
                                        <>
                                            {/* {timer?.toString()} */}
                                            <RangePicker
                                                style={{
                                                    float: 'right',
                                                    position: 'relative',
                                                    zIndex: '1',

                                                }}
                                                onChange={(val) => setTimer(val)}
                                                value={timer}
                                            />
                                            <DashBoard
                                                style={{
                                                    height: '250px',
                                                    width: '100%',
                                                    marginBottom: '20px',
                                                }}
                                                id="ProfileChart"
                                                option={option}
                                            />
                                        </>

                                    ) : ''
                                }
                                <React.Fragment key={tableItems[+activeTableKey]?.name}>
                                    <IF visible={['Tables', 'Column'].includes(tableItems[+activeTableKey]?.name)}>
                                        <SearchForm
                                            form={searchForm}
                                            onSearch={onSearch}
                                            placeholder={intl.formatMessage({ id: tableItems[+activeTableKey]?.name === 'Column' ? 'editor_dv_search_column' : 'editor_dv_search_table' })}
                                        />
                                    </IF>
                                </React.Fragment>
                                <Table
                                    size="small"
                                    scroll={{ y: 'calc(100vh - 454px)' }}
                                    rowKey={rowKey}
                                    dataSource={tableData}
                                    columns={columns}
                                    expandable={{
                                    // eslint-disable-next-line react/no-unstable-nested-components
                                        expandedRowRender: tableItems[+activeTableKey]?.name === 'Profile'
                                            // eslint-disable-next-line react/no-unstable-nested-components
                                            ? (record:{ dataType: string;uuid:string, type:string}) => (
                                                <ProfileContent
                                                    uuid={record.uuid}
                                                    type={record.dataType}
                                                />
                                            ) : undefined,
                                    }}
                                    pagination={
                                        false
                                    }
                                />
                                {
                                    tableItems[+activeTableKey]?.name === 'Column'
                                    || tableItems[+activeTableKey]?.name === 'Schema Changes'
                                     || tableItems[+activeTableKey]?.name === 'Tables'
                                      || tableItems[+activeTableKey]?.name === 'Metrics'
                                       || tableItems[+activeTableKey]?.name === 'Issues' ? (
                                            <Pagination
                                               style={{
                                                    float: 'right',
                                                    marginTop: '20px',
                                                }}
                                               size="small"
                                               current={pageInfo.pageNumber}
                                               onChange={onChange}
                                               total={total}
                                           />
                                        ) : ''
                                }
                            </div>

                        </div>
                    </Spin>
                </IF>
                <IF visible={!!metricsId}>
                    <div>
                        <div className="dv-back-btn" onClick={() => setMetricid('')} style={{ cursor: 'pointer' }}>
                            <ArrowLeftOutlined />
                        </div>
                        <span style={{
                            fontWeight: '600',
                        }}
                        >
                            {metricsName}

                        </span>
                        <div style={{ float: 'right' }}>
                            {detailKey === '3' ? <Button onClick={refreshHistoryList} type="primary" style={{ marginRight: '10px' }}>刷新</Button> : ''}
                            <Button onClick={() => onRun(metricsId)} type="primary">运行</Button>
                            <Button onClick={() => onDelete(metricsId)} style={{ marginLeft: '10px' }}>删除</Button>
                        </div>
                        <Detail ref={detailRef} id={metricsId} selectDatabases={selectDatabases} changeTabKey={changeDetailTabKey} />
                    </div>
                </IF>
            </IF>
            <Modal title={intl.formatMessage({ id: 'label_add' })} open={isModalOpen} onOk={handleOk} onCancel={handleCancel}>
                <Form
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 20 }}
                    initialValues={{ name: '' }}
                    autoComplete="off"
                    form={form}
                >
                    <Form.Item
                        label={intl.formatMessage({ id: 'label_name' })}
                        name="tagUUID"
                        rules={[{ required: true, message: intl.formatMessage({ id: 'header_top_search_msg' }) }]}
                    >
                        <Select
                            fieldNames={
                                {
                                    label: 'name',
                                    value: 'uuid',
                                }
                            }
                            options={tagList}
                        />
                    </Form.Item>
                </Form>
            </Modal>
            <Modal
                width="700px"
                footer={[]}
                title="Profile Schedule"
                onCancel={() => {
                    setisScheduleOpen(false);
                }}
                open={isScheduleOpen}
            >
                <Schedule
                    onSavaEnd={() => {
                        setisScheduleOpen(false);
                    }}
                    width="100%"
                    style={{ height: 'auto' }}
                    api="catalog/profile"
                    jobId={selectDatabases[currentIndex]?.uuid}
                />
            </Modal>
            <Modal
                width="700px"
                open={isIssueModalOpen}
                footer={[
                    <Button key="good" onClick={() => setIssue('good')}>
                        {intl.formatMessage({ id: 'job_good' })}
                    </Button>,
                    <Button key="bad" type="primary" onClick={() => setIssue('bad')}>
                        {intl.formatMessage({ id: 'job_bad' })}
                    </Button>,
                ]}
                onCancel={() => setIsIssueModalOpen(false)}
                title={intl.formatMessage({ id: 'job_issue_detail' })}
            >
                <Table
                    size="small"
                    showHeader={false}
                    dataSource={issuesTableData}
                    columns={issuesColumns}
                    pagination={
                        false
                    }
                />
            </Modal>
            <Modal
                width="400px"
                open={chooesColModalOpen}
                title={intl.formatMessage({ id: 'job_choose_col' })}
                maskClosable={false}
                footer={[
                    <Button key="good" onClick={() => setChooesColModalOpen(false)}>
                        {intl.formatMessage({ id: 'common_Cancel' })}
                    </Button>,
                    <Button key="bad" type="primary" onClick={() => runProfile()}>
                        {intl.formatMessage({ id: 'jobs_run' })}
                    </Button>,
                ]}
                onCancel={() => setChooesColModalOpen(false)}
                destroyOnClose
            >
                <Table
                    style={{ marginTop: 20 }}
                    rowSelection={{
                        type: 'checkbox',
                        onChange: (val:any[]) => setColCheckList(val),
                        defaultSelectedRowKeys: colCheckList,
                    }}
                    size="small"
                    dataSource={chooesColList}
                    columns={chooseColumns}
                    pagination={
                        false
                    }
                    rowKey="name"
                    scroll={{
                        y: 300,
                    }}
                />
            </Modal>
            <RenderModal />
            <Drawer width="50%" title={intl.formatMessage({ id: 'job_profile_execution_history' })} placement="right" onClose={onDrawerClose} open={openDrawer}>

                <Table
                    loading={historyLoading}
                    columns={historyColumns}
                    dataSource={historyList}
                    pagination={
                        {
                            total: historytotal,
                            onChange: onPageChange,
                        }
                    }
                />
            </Drawer>
            <RenderLoggerModal />
        </div>
    );
};

export default Index;
