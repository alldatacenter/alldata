/* eslint-disable max-len */
import React, { useState, useImperativeHandle } from 'react';
import {
    Row, Col, Form, Input, FormInstance,
} from 'antd';
import { useIntl } from 'react-intl';
import './index.less';
import { useLocation } from 'react-router-dom';
import { layoutItem } from '../helper';
import useRequest from '../../../hooks/useRequest';
import useRequiredRule from '../../../hooks/useRequiredRule';
import { TDetail, TMetricParameter } from '../type';
import { useEditorContextState } from '../../../store/editor';
import {
    CustomSelect, useMount, usePersistFn, IF,
} from '../../../common';
import { useSelector } from '@/store';
import Title from '../Title';

type InnerProps = {
    form: FormInstance,
    metricSelectRef: any,
    id: any,
    detail: TDetail
}

type dynamicConfigItem = {
    label: string,
    key: string,
}

const Index = ({
    form, metricSelectRef, id, detail,
}: InnerProps) => {
    const isJobsPage = useLocation().pathname.includes('jobs') || useLocation().pathname.includes('SLAs');
    const intl = useIntl();
    const { $http } = useRequest();
    const [context] = useEditorContextState();
    const [metricList, setMetricList] = useState([]);
    const requiredRules = useRequiredRule();
    const [databases, setDataBases] = useState([]);
    const [tables, setTables] = useState([]);
    const [columns, setColumns] = useState([]);
    const [configsName, setConfigsName] = useState<dynamicConfigItem[]>([]);
    const [configsMap, setConfigsMap] = useState<Record<string, dynamicConfigItem>>({});
    const { entityUuid, dsiabledEdit } = useSelector((r) => r.datasourceReducer);
    useImperativeHandle(metricSelectRef, () => ({
        getDynamicValues() {
            const values = form.getFieldsValue();
            return configsName.reduce((prev: Record<string, any>, cur) => {
                prev[cur.key] = values[cur.key];
                return prev;
            }, {});
        },
    }));
    const getTable = async (databaseName: string) => {
        try {
            const res = await $http.get(`/datasource/${id}/${databaseName}/tables`);
            setTables(res || []);
        } catch (error) {
        }
    };
    const getCloumn = async (databaseName: string, tableName:string) => {
        try {
            const res = await $http.get(`/datasource/${id}/${databaseName}/${tableName}/columns`);
            setColumns(res || []);
        } catch (error) {
        }
    };
    useMount(async () => {
        try {
            const $metricList = await $http.get('metric/list/DATA_QUALITY');
            const $databases = await $http.get(`/datasource/${id}/databases`);
            const {
                database, table, column, filter, ...rest
            } = detail?.parameterItem?.metricParameter || {} as TMetricParameter;
            if (database) {
                await getTable(database);
            }
            if (database && table) {
                await getCloumn(database, table);
            }
            const metricType = detail?.parameterItem?.metricType;
            if (metricType) {
                await getConfigsName(metricType);
            }
            let $resmetricList = $metricList ? JSON.parse(JSON.stringify($metricList)) : [];
            if (entityUuid || dsiabledEdit) {
                const isTable = dsiabledEdit?.isTable;
                $resmetricList = $resmetricList?.filter((item:any) => (isTable ? item.level === 'table' : item.level === 'column'));
            }
            setDataBases($databases || []);
            setMetricList($resmetricList || []);
            const options: Record<string, any> = {
                database,
                table,
                column,
                metricType,
                filter,
            };
            Object.keys(rest).forEach((item) => {
                options[item] = rest[item];
            });
            form.setFieldsValue(options);
        } catch (error) {
            console.log('error', error);
        }
    });

    const getConfigsName = usePersistFn(async (val) => {
        try {
            const res = await $http.get<dynamicConfigItem[]>(`metric/configs/${val}`);
            const $configMaps: Record<string, dynamicConfigItem> = {};
            const $res: dynamicConfigItem[] = [];
            (res || []).forEach((item) => {
                if (['table', 'column', 'filter'].includes(item.key)) {
                    $configMaps[item.key] = item;
                    return;
                }
                $res.push(item);
            });
            setConfigsMap($configMaps);
            setConfigsName($res);
        } catch (error) {
        }
    });
    const databasesChange = (val: string) => {
        form.setFieldsValue({
            table: undefined,
        });
        getTable(val);
    };
    const tableChange = () => {
        form.setFieldsValue({
            column: undefined,
        });
        const values = form.getFieldsValue();
        getCloumn(values.database, values.table);
    };
    const renderColumn = () => {
        const {
            column,
        } = detail?.parameterItem?.metricParameter || {} as TMetricParameter;
        return (
            <Form.Item
                {...layoutItem}
                label={intl.formatMessage({ id: 'dv_metric_column' })}
                name="column"
                rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_column' }) }]}
            >
                <CustomSelect defaultValue={column} disabled={!isJobsPage && (!!entityUuid || !dsiabledEdit?.isTable) && !!detail?.parameterItem?.metricParameter?.column} allowClear source={columns} sourceValueMap="name" />
            </Form.Item>
        );
    };

    const dynamicRender = (item: dynamicConfigItem) => (
        <Form.Item
            {...layoutItem}
            label={item.label}
            name={item.key}
            rules={[...requiredRules]}
        >
            <Input style={{ width: '100%' }} autoComplete="off" />
        </Form.Item>
    );
    return (
        <Title title={intl.formatMessage({ id: 'dv_metric_config' })}>
            <div>
                <Row gutter={30}>
                    <Col span={12}>
                        <Form.Item
                            {...layoutItem}
                            label="Metric"
                            name="metricType"
                            rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_placeholder' }) }]}
                        >
                            <CustomSelect
                                source={metricList}
                                sourceValueMap="key"
                                onChange={getConfigsName}
                            />
                        </Form.Item>
                        <Form.Item
                            {...layoutItem}
                            label={intl.formatMessage({ id: 'dv_metric_database' })}
                            name="database"
                            rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_databases' }) }]}
                        >
                            <CustomSelect disabled={!isJobsPage && (!!entityUuid || !!dsiabledEdit)} onChange={databasesChange} allowClear source={databases} sourceValueMap="name" />
                        </Form.Item>
                        <IF visible={!!configsMap.table}>
                            <Form.Item
                                {...layoutItem}
                                label={configsMap.table?.label || intl.formatMessage({ id: 'dv_metric_table' })}
                                name="table"
                                rules={[{ required: true, message: intl.formatMessage({ id: 'editor_dv_metric_select_table' }) }]}
                            >
                                <CustomSelect
                                    disabled={!isJobsPage && (!!entityUuid || !!dsiabledEdit) && !!detail?.parameterItem?.metricParameter?.table}
                                    onChange={tableChange}
                                    allowClear
                                    source={tables}
                                    sourceValueMap="name"
                                />
                            </Form.Item>
                        </IF>
                        <Form.Item noStyle dependencies={['table', 'metricType']}>
                            {() => {
                                const value = form.getFieldValue('table');
                                if (!value || !configsMap.column) {
                                    return null;
                                }
                                return renderColumn();
                            }}
                        </Form.Item>
                        {
                            configsName.map((item) => <React.Fragment key={item.key}>{dynamicRender(item)}</React.Fragment>)
                        }
                    </Col>
                    <Col span={12}>
                        <IF visible={!!configsMap.filter}>
                            <div style={{ paddingTop: 20 }} />
                            <Form.Item
                                className="dv-editor__condition"
                                colon={false}
                                label={(
                                    <div>
                                        {intl.formatMessage({ id: 'dv_metric_condition' })}
                                        :
                                    </div>
                                )}
                                name="filter"
                            >
                                <Input.TextArea autoComplete="off" style={{ marginLeft: context.locale === 'en_US' ? -100 : -62 }} rows={4} />
                            </Form.Item>
                        </IF>
                    </Col>
                </Row>
            </div>
        </Title>
    );
};

export default Index;
