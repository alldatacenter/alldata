import React, { useRef, useState } from 'react';
import {
    Button, Form, FormInstance, message, Tabs, Divider,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    useModal, useImmutable, usePersistFn, useMount, IF, useWatch,
} from '@/common';
import RuleSelect from './RuleSelect';
import MetricSelect from './MetricSelect';
import ExpectedValue from './ExpectedValue';
import VerifyConfigure from './VerifyConfigure';
import store, { RootReducer } from '@/store';
import { TDetail } from './type';
import { guid, pickProps } from './helper';
import { useMetricName } from './MetricNameModal';

const { TabPane } = Tabs;

type TargetKey = React.MouseEvent | React.KeyboardEvent | string;

type TMetricItem = {
    id: any,
    detail: TDetail,
    uuid?: string,
    setAction?: (...args: any) => any;
}

const MetricItem = (props: TMetricItem) => {
    const { detail, id, setAction } = props;
    const { datasourceReducer } = store.getState() as RootReducer;
    const [metricType, setMetricTypeParent] = useState('');
    const metricSelectRef = useRef<any>();
    const [form] = Form.useForm();

    const validSave = async () => {
        try {
            const values = await form.validateFields();
            const parameter: any = {
                ...(pickProps(values, ['metricType', 'expectedType', 'resultFormula', 'operator', 'threshold'])),
                metricParameter: {
                    ...(pickProps(values, ['database', 'table', 'column', 'filter'])),
                },
            };
            if (values.expectedType === 'fix_value') {
                parameter.expectedParameter = {
                    expected_value: values.expected_value,
                };
            }
            if (datasourceReducer.modeType === 'comparison') {
                parameter.dataSourceId2 = values.dataSourceId2;
                Object.assign(parameter, pickProps(values, ['metricParameter', 'metricParameter2']));
                if (values.metricType === 'multi_table_accuracy') {
                    values.mappingColumns.forEach((item: any) => {
                        item.operator = '=';
                    });
                    parameter.mappingColumns = values.mappingColumns;
                }
            } else if (datasourceReducer.modeType === 'quality') {
                parameter.metricParameter = {
                    ...(pickProps(values, ['database', 'table', 'column', 'filter'])),
                    ...metricSelectRef.current.getDynamicValues(),
                };
            }
            return parameter;
        } catch (error) {
            return null;
        }
    };

    useMount(() => {
        setAction?.({
            validSave,
        });
    });

    return (
        <Form form={form} name={props.uuid}>
            <IF visible={datasourceReducer.modeType === 'comparison'}>
                <RuleSelect detail={detail} id={id} form={form} setMetricTypeParent={setMetricTypeParent} />
            </IF>
            <IF visible={datasourceReducer.modeType === 'quality'}>
                <MetricSelect detail={detail} id={id} form={form} metricSelectRef={metricSelectRef} />
            </IF>
            <IF visible={datasourceReducer.modeType === 'quality' || metricType === 'multi_table_accuracy'}>
                <ExpectedValue detail={detail} form={form} />
            </IF>
            <VerifyConfigure detail={detail} form={form} />
        </Form>
    );
};

// metric tabs

type TmetricTabsProps = {
    id: any,
    detail: TDetail,
    parameter?: TDetail[];
    metricRef?: any;
}
const MetricTabs = (props: TmetricTabsProps) => {
    const [activeKey, setActiveKey] = useState('');
    const [items, setItems] = useState<TDetail[]>([]);
    const formActionsRef = useRef<Record<string, FormInstance >>({});
    const { Render, show } = useMetricName({});
    const intl = useIntl();
    const {
        id, detail, metricRef,
    } = props;
    metricRef.current = {
        // eslint-disable-next-line no-async-promise-executor
        getValues: () => new Promise(async (resolve, reject) => {
            try {
                const parameterArray: any[] = [];
                // eslint-disable-next-line no-restricted-syntax
                for (const item of items) {
                    const data: any = formActionsRef.current[item?.uuid as any as string];
                    // eslint-disable-next-line no-await-in-loop
                    const parameter = await data.validSave();
                    if (!parameter) {
                        reject();
                        return;
                    }
                    parameterArray.push({
                        ...parameter,
                        name: item?.$name,
                        uuid: item?.uuid,
                    });
                }
                resolve(parameterArray);
            } catch (error) {
                console.log(error);
                reject(error);
            }
        }),
    };

    useWatch(detail, () => {
        const $items: any[] = [];
        ((detail?.parameter || []) as any).forEach((item: any) => {
            $items.push({
                ...detail,
                uuid: item.uuid || guid(),
                $name: item?.name || intl.formatMessage({ id: 'dv_config_text' }),
                parameterItem: {
                    ...item,
                },
            });
        });
        if ($items.length < 1) {
            $items.push({
                ...detail,
                $name: intl.formatMessage({ id: 'dv_config_text' }),
                uuid: guid(),
                parameterItem: {},
            });
        }
        setActiveKey($items[0]?.uuid);
        setItems($items);
    }, { immediate: true });

    const add = () => {
        show({
            name: '',
            ok(values) {
                const newPanes = [...items];
                const item: TDetail = {
                    // @ts-ignore
                    $name: values.name, uuid: guid(), ...detail, parameterItem: {},
                };
                if (detail?.parameterItem && !detail?.id) {
                    // @ts-ignore
                    item.parameterItem = { ...(detail?.parameterItem || {}) };
                }
                newPanes.push(item);
                setItems(newPanes);
            },
        });
    };

    const remove = (targetKey: TargetKey) => {
        if (items.length === 1) {
            message.error(intl.formatMessage({ id: 'dv_metric_job_error_message' }));
            return;
        }

        let newActiveKey = activeKey;
        let lastIndex = -1;
        items.forEach((item: any, i) => {
            if (item.uuid === targetKey) {
                lastIndex = i - 1;
            }
        });
        const newPanes: any = items.filter((item: any) => item.uuid !== targetKey);
        if (newPanes.length && newActiveKey === targetKey) {
            if (lastIndex >= 0) {
                newActiveKey = newPanes[lastIndex].key;
            } else {
                newActiveKey = newPanes[0].key;
            }
        }
        setItems(newPanes);
        setActiveKey(newActiveKey);
    };

    const onEdit = (
        targetKey: TargetKey,
        action: 'add' | 'remove',
    ) => {
        if (action === 'add') {
            add();
        } else {
            remove(targetKey);
        }
    };

    return (
        <div>
            <Tabs
                type="editable-card"
                onEdit={onEdit}
                size="small"
                activeKey={activeKey}
                onChange={(key) => (setActiveKey(key))}
            >
                {
                    (items || []).map((item) => (
                        <TabPane tab={item?.$name} key={item?.uuid} forceRender>
                            <React.Fragment key={item?.uuid}>
                                <MetricItem
                                    id={id}
                                    detail={item}
                                    uuid={item?.uuid}
                                    setAction={(data) => {
                                        formActionsRef.current[item?.uuid as any] = data;
                                    }}
                                />
                            </React.Fragment>
                        </TabPane>
                    ))
                }
            </Tabs>
            <Render />
            <Divider />
        </div>
    );
};

export default MetricTabs;
