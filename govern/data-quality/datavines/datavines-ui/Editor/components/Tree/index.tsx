import React, { useEffect, useState } from 'react';
import {
    Tree, message, Button, Dropdown, Menu, Spin,
} from 'antd';
import {
    DatabaseOutlined, CopyOutlined, DownOutlined, PoweroffOutlined, ReloadOutlined,
} from '@ant-design/icons';
import type { DataNode, EventDataNode, TreeProps } from 'antd/lib/tree';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import { useIntl } from 'react-intl';
import { useMetricModal } from '../MetricModal';
import { setEditorFn, useEditorActions, useEditorContextState } from '../../store/editor';
import { usePersistFn } from '../../common';
import { IDvDataBaseItem } from '../../type';
import useTableCloumn from './useTableCloumn';
import './index.less';
import { TDetail } from '../MetricModal/type';
import store from '@/store';
import useRequest from '../../hooks/useRequest';

type TIndexProps = {
    getDatabases: (...args: any[]) => void;
    onShowModal?: (...args: any[]) => any;
}

const Index = ({
    getDatabases, onShowModal,
}: TIndexProps) => {
    // console.log('onShowModal1', onShowModal);
    const { $http } = useRequest();
    const { Render: RenderModal, show } = useMetricModal();
    const [spinning, setSpinning] = useState(false);
    const [{ databases, id , selectDatabases}] = useEditorContextState();
    const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
    const [defaultSelectId, setDefaultSelectId] = useState<string| number>('');
    const intl = useIntl();
    const fns = useEditorActions({ setEditorFn });
    const $setExpandedKeys = usePersistFn((key: React.Key, isCancel?: boolean) => {
        if (isCancel) {
            setExpandedKeys(expandedKeys.filter((item) => (item !== key)));
            return;
        }
        setExpandedKeys([...expandedKeys, key]);
    });
    const { onRequestTable, onRequestCloumn, onSeletCol } = useTableCloumn({ $setExpandedKeys });

    // const onFieldClick = (database: string, table: string, column?: string) => {
    //     const $record = {
    //         parameterItem: {
    //             metricParameter: {
    //                 database,
    //                 table,
    //                 column: column || '',
    //             },
    //         },
    //     };
    //     if (onShowModal) {
    //         store.dispatch({
    //             type: 'save_datasource_modeType',
    //             payload: 'quality',
    //         });
    //         onShowModal({
    //             parameter: JSON.stringify($record.parameterItem),
    //             parameterItem: $record.parameterItem,
    //         });
    //         return;
    //     }

    //     show(id as string, $record as TDetail);
    // };

    const renderSingle = (item: IDvDataBaseItem) => ({
        title: <span className="dv-editor-tree-title">{item.name}</span>,
        key: `${item.uuid}@@${item.name}`,
        dataName: item.name,
        type: item.type,
        icon: <DatabaseOutlined />,
        uuid: item.uuid,
        children: (item.children || []).map((tableItem) => ({
            title: <span className="dv-editor-tree-title">
                {tableItem.name}
            </span>,
            key: `${item.uuid}@@${item.name}##${tableItem.uuid}@@${tableItem.name}`,
            dataName: tableItem.name,
            parentName: item.name,
            type: tableItem.type,
            uuid: tableItem.uuid,
            icon: (
                <span
                    onClick={(e) => {
                        e.stopPropagation();
                    }}
                >
                    <CopyToClipboard
                        text={`select * from ${item.name}.${tableItem.name}`}
                        onCopy={() => {
                            message.success('Copy success');
                        }}
                    >
                        <CopyOutlined />
                    </CopyToClipboard>
                </span>
            ),
            children: (tableItem.children || []).map((fieldItem) => ({
                title: (
                    <span className="dv-editor-tree-title">
                        {fieldItem.name}
                    </span>
                ),
                className: 'dv-editor-tree-field',
                uuid: fieldItem.uuid,
                key: `${item.uuid}@@${item.name}##${tableItem.uuid}@@${tableItem.name}##${fieldItem.uuid}@@${fieldItem.name}`,
                dataName: fieldItem.name,
                type: fieldItem.type,
            })),
        })),
    });
    const onSelect: TreeProps['onSelect'] = (selectedKeys, e: any) => {
        if (e.node?.selected) return;
        // if (e.node.children?.length >= 1) {
        //     if (e.selected) {
        //         $setExpandedKeys(e.node.key,true);
        //     } else {
        //         $setExpandedKeys(e.node.key, true);
        //     }
        // }
        const allData = e.node.key.split("##");
        const allSelectDatabases =  allData.map((item:any)=>{
            const itemData = item.split("@@");
            return {
                name: itemData[1],
                uuid: itemData[0]
            }
        })
        allSelectDatabases.unshift(selectDatabases[0]);
        // console.log("allSelectDatabases",allSelectDatabases,allData)
        // fns.setEditorFn({ selectDatabases: [...allSelectDatabases],databases });
        // 这里会出现跳数据然后获取下级
        if (e.node.type === 'database') {
            setSpinning(true)
            onRequestTable(e.node.dataName, e.node.uuid, allSelectDatabases,()=>{
                setSpinning(false)
            });
        } else if (e.node.type === 'table') {
            setSpinning(true)
            onRequestCloumn(e.node.parentName, e.node.dataName, e.node.uuid, allSelectDatabases,()=>{
                setSpinning(false)
            });
        } else if (e.node.type === 'column') {
            // console.log(' e.node', e.node);
            onSeletCol(e.node.dataName, e.node.uuid,allSelectDatabases);
        }
    };

    const onExpand = (expandedKeysValue: React.Key[]) => {
        // console.log("expandedKeysValue",expandedKeysValue)
        setExpandedKeys(expandedKeysValue);
    };
    // 右键刷新
    const refresh = async (nodeData: DataNode & {type:string;key:string;dataName:string;parentName?:string}) => {
        await $http.post('catalog/refresh', {
            database: nodeData.parentName || nodeData.dataName,
            datasourceId: id,
            table: nodeData.parentName ? nodeData.dataName : '',
        });
        message.success(intl.formatMessage({ id: 'common_success' }));
    };
    const [treeHeight,setTreeHeight] = useState(0)
    useEffect(() => {
        if (databases.length > 0 && expandedKeys.length === 0) {
            onSelect([], {
                event: 'select',
                selected: false,
                selectedNodes: [],
                node: {
                    type: 'database',
                    dataName: databases[0].name,
                    name: databases[0].name,
                    key: `${databases[0].uuid}@@${databases[0].name}`,
                    uuid: databases[0].uuid,
                } as unknown as unknown as EventDataNode<DataNode>,
                nativeEvent: new MouseEvent('move')
            });
            setDefaultSelectId(`${databases[0].uuid}@@${databases[0].name}`);
            setExpandedKeys([`${databases[0].uuid}@@${databases[0].name}`])
            // setDefaultSelectId(`${databases[0].uuid}@@${databases[0].name}`);
        }
        return () => {
            if (databases.length > 0 && expandedKeys.length === 0) {
               
                setTreeHeight(document.getElementsByClassName("dv-editor-tree_list")[0].scrollHeight)
               
            }
        };
    }, [databases]);

    const openData = (node:any) => {
        const index = expandedKeys.indexOf(node.key)
        if(index > -1){
            expandedKeys.splice(index,1)
            setExpandedKeys([...expandedKeys])
        }else{
            setExpandedKeys([...expandedKeys,node.key])
        }
    }

    const titleRender = (nodeData: any) => (
        nodeData.type !== 'column'
            ? (
                <Dropdown
                    // eslint-disable-next-line react/no-unstable-nested-components
                    overlay={() => (
                        <Menu
                            items={[
                                {
                                    key: 'refresh',
                                    label: <span onClick={() => refresh(nodeData)}> {intl.formatMessage({ id: 'job_log_refresh' })}</span>,
                                },
                            ]}
                        />
                    )}
                    trigger={['contextMenu']}
                >
                    <div onClick={()=>openData(nodeData)} >
                        {nodeData.title as string}
                    </div>
                </Dropdown>
            ) : <div >{nodeData.title as string}</div>
    );
    return (
        <div className="dv-editor-tree" >
            <div
                className="dv-editor-flex-between"
                style={{
                    alignItems: 'center',
                }}
            >
                <span style={{ marginRight: 15 }}>
                    <ReloadOutlined
                        onClick={() => {
                            getDatabases();
                        }}
                    />

                </span>
            </div>
            {
                defaultSelectId ? (
                    <Spin spinning={spinning} size="small"><div className="dv-editor-tree_list">
                        <Tree
                            showIcon
                            switcherIcon={<DownOutlined style={{fontSize:'14px',position:'relative',top:'2px'}} />}
                            onSelect={onSelect}
                            onExpand={onExpand}
                            expandedKeys={expandedKeys}
                            treeData={databases.map((item) => renderSingle(item))}
                            titleRender={titleRender}
                            defaultSelectedKeys={[defaultSelectId]}
                            height={treeHeight}
                        />
                    </div></Spin>
                    
                ) : ''
            }

            <RenderModal />
        </div>
    );
};

export default Index;
