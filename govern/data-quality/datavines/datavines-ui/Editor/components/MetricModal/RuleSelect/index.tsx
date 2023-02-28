import React, { useState, useImperativeHandle } from 'react';
import {
    Row, Col, Form, Input, FormInstance, Button,
} from 'antd';
import './index.less';
import { useIntl } from 'react-intl';
import TextArea from 'antd/lib/input/TextArea';
import { MinusCircleOutlined } from '@ant-design/icons';
import { layoutItem, layoutOneLineItem, layoutNoneItem } from '../helper';
import useRequest from '../../../hooks/useRequest';
import useRequiredRule from '../../../hooks/useRequiredRule';
import { TDetail, TMetricParameter } from '../type';
import { useEditorContextState } from '../../../store/editor';
import {
    CustomSelect, useMount, usePersistFn, IF,
} from '../../../common';
import Title from '../Title';
import store, { RootReducer } from '@/store';

type InnerProps = {
    form: FormInstance,
    id: any,
    detail: TDetail,
    setMetricTypeParent: React.Dispatch<React.SetStateAction<string>>
}

type dynamicConfigItem = {
    label: string,
    key: string,
}

const Index = ({
    form, id, detail, setMetricTypeParent,
}: InnerProps) => {
    const intl = useIntl();
    const { $http } = useRequest();
    const [metricList, setMetricList] = useState([]);
    const [metricType, setMetricType] = useState('');
    const [dataSoucre, setDataSoucre] = useState([]);
    const [databases1, setDatabases1] = useState([]);
    const [databases2, setDatabases2] = useState([]);
    const [table1, setTable1] = useState([]);
    const [table2, setTable2] = useState([]);
    const [column1, setCloumn1] = useState([]);
    const [column2, setCloumn2] = useState([]);
    useMount(async () => {
        try {
            getDatabases(id, 1, true);
            if (!detail || !detail.id) {
                form.setFieldsValue({
                    mappingColumns: [{}],
                    dataSourceId: parseInt(id),
                });
            } else {
                form.setFieldsValue({
                    dataSourceId: detail.dataSourceId,
                    dataSourceId2: detail?.dataSourceId2,
                    metricType: detail?.metricType,
                    metricParameter: detail?.parameterItem?.metricParameter,
                    metricParameter2: detail?.parameterItem?.metricParameter2,
                    mappingColumns: detail?.parameterItem?.mappingColumns,
                });
                // console.log('detail', detail)
                setMetricTypeParent(detail?.metricType);
                setMetricType(detail?.metricType);
                getDatabases(detail?.dataSourceId2, 2, true);
                getTable(detail?.parameterItem?.metricParameter?.database, detail?.dataSourceId2, 1, true);
                getTable(detail?.parameterItem?.metricParameter2?.database2, detail?.dataSourceId2, 2, true);
                getCloumn(detail?.parameterItem?.metricParameter?.table, id, detail?.parameterItem?.metricParameter?.database, 1, false);
                getCloumn(detail?.parameterItem?.metricParameter2?.table2, detail?.dataSourceId2, detail?.parameterItem?.metricParameter2?.database2, 2, false);
            }

            const $metricList = await $http.get('metric/list/DATA_RECONCILIATION');
            setMetricList($metricList || []);
            const state: RootReducer = store.getState();
            const { workspaceId } = state.workSpaceReducer;
            const $dataSoucrce = await $http.get('/datasource/page', {
                workSpaceId: workspaceId,
                pageNumber: 1,
                pageSize: 9999,
            });
            // console.log('执行');
            setDataSoucre($dataSoucrce.records || []);
        } catch (error) {
            console.log('erro', error);
        }
    });
    const changeRule = (val: string) => {
        setMetricTypeParent(val);
        setMetricType(val);
    };
    const getDatabases = async (id: string | undefined, index: number, isInit: boolean | undefined) => {
        if (!id) return;
        const $databases = await $http.get(`datasource/${id}/databases`);
        if (index === 1) {
            setDatabases1($databases);
            if (isInit) return;
            setTable1([]);
            setCloumn1([]);
        } else {
            setDatabases2($databases);
            if (isInit) return;
            setTable2([]);
            setCloumn2([]);
        }
    };
    const getTable = async (database: string | undefined, id: string | undefined, index: number, isInit: boolean | undefined) => {
        if (!id || !database) return;
        const $table = await $http.get(`datasource/${id}/${database}/tables`);
        if (index === 1) {
            setTable1($table);
            isInit && setCloumn1([]);
        } else {
            setTable2($table);
            isInit && setCloumn2([]);
        }
    };
    const getCloumn = async (table: string | undefined, id: string | undefined, database: string | undefined, index: number, isInit: boolean | undefined) => {
        if (!table || !id || !database) return;
        const $column = await $http.get(`datasource/${id}/${database}/${table}/columns`);
        index === 1 ? setCloumn1($column.columns || []) : setCloumn2($column.columns || []);
    };
    return (
        <div style={{ padding: '0 10px' }}>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dev_metric_rule' })}`}
                        name="metricType"
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_rule' }) }]}
                    >
                        <CustomSelect
                            source={metricList}
                            sourceValueMap="key"
                            onChange={changeRule}
                        />
                    </Form.Item>
                </Col>
            </Row>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dev_metric_datasource' })}1`}
                        name="dataSourceId"
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_datasource' }) }]}
                    >
                        <CustomSelect
                            source={dataSoucre}
                            sourceLabelMap="name"
                            sourceValueMap="id"
                            disabled
                        />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dev_metric_datasource' })}2`}
                        name="dataSourceId2"
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_datasource' }) }]}
                    >
                        <CustomSelect
                            source={dataSoucre}
                            sourceLabelMap="name"
                            sourceValueMap="id"
                            onChange={(e) => getDatabases(e, 2, false)}
                        />
                    </Form.Item>
                </Col>
            </Row>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dv_metric_database' })}1`}
                        name={['metricParameter', 'database']}
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_databases' }) }]}
                    >
                        <CustomSelect
                            source={databases1}
                            sourceValueMap="name"
                            onChange={(e) => getTable(e, form.getFieldValue('dataSourceId'), 1, false)}
                        />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dv_metric_database' })}2`}
                        name={['metricParameter2', 'database2']}
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_databases' }) }]}
                    >
                        <CustomSelect
                            source={databases2}
                            sourceValueMap="name"
                            onChange={(e) => getTable(e, form.getFieldValue('dataSourceId2'), 2, false)}
                        />
                    </Form.Item>
                </Col>
            </Row>
            <Row gutter={30}>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dv_metric_table' })}1`}
                        name={['metricParameter', 'table']}
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_table' }) }]}
                    >
                        <CustomSelect
                            source={table1}
                            sourceValueMap="name"
                            onChange={(e) => getCloumn(e, form.getFieldValue('dataSourceId'), form.getFieldValue(['metricParameter', 'database']), 1, false)}
                        />
                    </Form.Item>
                </Col>
                <Col span={12}>
                    <Form.Item
                        {...layoutItem}
                        label={`${intl.formatMessage({ id: 'dv_metric_table' })}2`}
                        name={['metricParameter2', 'table2']}
                        rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_table' }) }]}
                    >
                        <CustomSelect
                            source={table2}
                            sourceValueMap="name"
                            onChange={(e) => getCloumn(e, form.getFieldValue('dataSourceId2'), form.getFieldValue(['metricParameter2', 'database2']), 2, false)}
                        />
                    </Form.Item>
                </Col>
            </Row>
            <IF visible={metricType === 'multi_table_accuracy'}>
                <Row gutter={30}>
                    <Col span={12}>
                        <Form.Item
                            {...layoutItem}
                            label={`${intl.formatMessage({ id: 'dv_metric_condition' })}1`}
                            name={['metricParameter', 'filter']}
                        >
                            <TextArea />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            {...layoutItem}
                            label={`${intl.formatMessage({ id: 'dv_metric_condition' })}2`}
                            name={['metricParameter2', 'filter2']}
                        >
                            <TextArea />
                        </Form.Item>
                    </Col>
                </Row>
                <Form.List name="mappingColumns">
                    {(fields, { add, remove }, { errors }) => (
                        <>
                            <Row gutter={30}>
                                <Col span={21} push={3} className="dv-editor-title_flex">
                                    <span>
                                        {intl.formatMessage({ id: 'dv_metric_check_column' })}
                                        {' '}
                                    </span>
                                    <span style={{ cursor: 'pointer' }} onClick={() => add()}>
                                        {intl.formatMessage({ id: 'dv_metric_add' })}
                                        {' '}
                                    </span>
                                </Col>
                            </Row>
                            {
                                fields.map((field, index) => (
                                    <Form.Item
                                        required={false}
                                        key={field.key}
                                        style={{ marginBottom: 0 }}
                                    >
                                        <Row gutter={30}>
                                            <Col span={9} push={3}>
                                                <Form.Item
                                                    {...layoutNoneItem}
                                                    {...field}
                                                    label=""
                                                    name={[field.name, 'column']}
                                                    rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_column' }) }]}
                                                >
                                                    <CustomSelect
                                                        source={column1}
                                                        sourceValueMap="name"
                                                    />
                                                </Form.Item>
                                            </Col>
                                            <Col span={3} push={3} style={{ textAlign: 'center' }}>=</Col>
                                            <Col span={9} push={3}>

                                                <Form.Item
                                                    {...layoutNoneItem}
                                                    {...field}
                                                    name={[field.name, 'column2']}
                                                    label=""
                                                    rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_column' }) }]}
                                                >
                                                    <CustomSelect
                                                        source={column2}
                                                        sourceValueMap="name"
                                                    />
                                                </Form.Item>
                                                <IF visible={fields.length > 1}>

                                                    <MinusCircleOutlined
                                                        className="dv-delete-button"
                                                        onClick={() => remove(field.name)}
                                                    />
                                                </IF>
                                            </Col>
                                            {/* <Col span={2}>
                                                <IF visible={fields.length > 1}>
                                                    <MinusCircleOutlined onClick={() => remove(field.name)} />
                                                </IF>
                                            </Col> */}
                                        </Row>
                                    </Form.Item>
                                ))
                            }
                        </>
                    )}

                </Form.List>
            </IF>
            <IF visible={metricType === 'multi_table_value_comparison'}>
                <Row gutter={30}>
                    <Col span={12}>
                        <Form.Item
                            {...layoutItem}
                            label={intl.formatMessage({ id: 'dev_metric_actual_value_execution' })}
                            name={['metricParameter', 'actual_execute_sql']}
                            rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_input_actual_value_execution' }) }]}
                        >
                            <TextArea />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            {...layoutItem}
                            label={intl.formatMessage({ id: 'dev_metric_expected_value_execution' })}
                            name={['metricParameter2', 'expected_execute_sql']}
                            rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_input_expected_value_execution' }) }]}
                        >
                            <TextArea />
                        </Form.Item>
                    </Col>
                </Row>
            </IF>
        </div>
    );
};

export default Index;
