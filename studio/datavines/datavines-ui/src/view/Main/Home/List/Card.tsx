import React, { useState } from 'react';
import {
    Pagination, Dropdown, Menu, Popconfirm, Row, Col, Tag, Modal,
} from 'antd';
import {
    EditOutlined, DeleteOutlined, EllipsisOutlined, FundOutlined,
} from '@ant-design/icons';
import { useIntl } from 'react-intl';
import { IDataSourceListItem, IDataSourceList } from '@/type/dataSource';
import Schedule from '@/view/Main/HomeDetail/Jobs/components/Schedule';

type IndexProps = {
    tableData: IDataSourceList,
    onPageChange: any,
    pageParams: { pageNumber: number, pageSize: number }
    goDetail: (record: IDataSourceListItem) => void;
    onEdit: (record: IDataSourceListItem) => void,
    onDelete: (record: IDataSourceListItem) => void,
}

const Index: React.FC<IndexProps> = ({
    tableData, pageParams, onPageChange, goDetail, onEdit, onDelete,
}) => {
    const intl = useIntl();
    const onChange = (current: number, pageSize: number) => {
        onPageChange({
            current,
            pageSize,
        });
    };
    const [isScheduleOpen, setisScheduleOpen] = useState(false);
    const [uuid, setUuid] = useState<string | number>('');
    const renderItem = (item: IDataSourceListItem) => (
        <React.Fragment key={item.id}>
            <Col span={6}>
                <div style={{
                    background: '#FFF4D8',
                    borderRadius: '8px',
                    padding: '20px',
                    marginBottom: '20px',
                }}
                >
                    <p style={{
                        margin: '0px 0px 20px 0px',
                        color: '#111',
                        fontWeight: '600',
                    }}
                    >
                        <span
                            style={{
                                cursor: 'pointer',
                            }}
                            onClick={() => goDetail(item)}
                        >
                            {item.name}
                        </span>

                        <Tag
                            color="#32D74B"
                            style={{
                                marginLeft: '10px',
                            }}
                        >
                            {/* {intl.formatMessage({ id: 'datasource_modal_source_type' })} */}
                            {item.type}
                        </Tag>
                        <Dropdown
                            overlay={(
                                <Menu
                                    items={[
                                        {
                                            label: (
                                                <div style={{ width: '100%', height: '100%' }} onClick={() => (onEdit(item))}>
                                                    <EditOutlined style={{ marginRight: '4px' }} />
                                                    {intl.formatMessage({ id: 'common_edit' })}
                                                </div>
                                            ),
                                            key: 'edit',
                                        },
                                        {
                                            label: (
                                                <Popconfirm
                                                    title={intl.formatMessage({ id: 'common_delete_tip' })}
                                                    onConfirm={() => { onDelete(item); }}
                                                    okText={intl.formatMessage({ id: 'common_Ok' })}
                                                    cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                                                >
                                                    <a style={{ color: '#f81d22' }}>
                                                        <DeleteOutlined style={{ color: '#f81d22', marginRight: '4px' }} />
                                                        {intl.formatMessage({ id: 'common_delete' })}
                                                    </a>
                                                </Popconfirm>
                                            ),
                                            key: 'delete',
                                        },
                                        {
                                            label: (

                                                <a onClick={() => {
                                                    setisScheduleOpen(true);
                                                    setUuid(item.id);
                                                }}
                                                >
                                                    <FundOutlined style={{ marginRight: '4px' }} />
                                                    {intl.formatMessage({ id: 'home_metadata_fetch' })}
                                                </a>

                                            ),
                                            key: 'push',
                                        },
                                    ]}
                                />
                            )}
                            placement="bottomLeft"
                        >
                            <EllipsisOutlined style={{
                                float: 'right',
                                color: '#000',
                            }}
                            />
                        </Dropdown>

                    </p>
                    <span style={{
                        fontSize: '12px',
                        color: '#111',
                    }}
                    >
                        {intl.formatMessage({ id: 'datasource_updateTime' })}
                        ：
                        {item.updateTime}

                    </span>

                </div>
                {/* <Card
                    title={(
                        <a
                            style={{
                                color: '#ffd56a',
                            }}
                            onClick={() => {
                                goDetail(item);
                            }}
                        >
                            {item.name}
                        </a>
                    )}
                    extra={(
                        <div onClick={(e) => (e.stopPropagation())}>
                            <Dropdown
                                overlay={(
                                    <Menu
                                        items={[
                                            {
                                                label: <span onClick={() => (onEdit(item))}>{intl.formatMessage({ id: 'common_edit' })}</span>,
                                                key: 'edit',
                                                icon: <EyeOutlined />,
                                            },
                                            {
                                                label: (
                                                    <Popconfirm
                                                        title={intl.formatMessage({ id: 'common_delete_tip' })}
                                                        onConfirm={() => { onDelete(item); }}
                                                        okText={intl.formatMessage({ id: 'common_Ok' })}
                                                        cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                                                    >
                                                        <a style={{ color: '#f81d22' }}>{intl.formatMessage({ id: 'common_delete' })}</a>
                                                    </Popconfirm>
                                                ),
                                                icon: <DeleteOutlined />,
                                                key: 'delete',
                                            },
                                        ]}
                                    />
                                )}
                                placement="bottomLeft"
                            >
                                <a style={{
                                    color: '#ffd56a',
                                }}
                                >
                                    {intl.formatMessage({ id: 'common_more' })}

                                </a>
                            </Dropdown>
                        </div>
                    )}
                    style={{ marginBottom: '15px' }}
                >
                    <p>
                        {intl.formatMessage({ id: 'datasource_modal_source_type' })}
                        :
                        {' '}
                        {item.type}
                    </p>
                    <p>
                        {intl.formatMessage({ id: 'datasource_updateTime' })}
                        :
                        {' '}
                        {item.updateTime}
                    </p>
                </Card> */}
            </Col>
        </React.Fragment>
    );
    return (
        <>
            <div
                className="dv-card-warp"
                style={{
                    height: 'calc(100% - 70px)',
                }}
            >
                <Row gutter={16}>
                    {
                        (tableData.list || []).map((item) => (renderItem(item)))
                    }
                </Row>
            </div>
            <div style={{ textAlign: 'right' }}>
                <Pagination
                    showSizeChanger
                    onChange={onChange}
                    current={pageParams.pageNumber}
                    pageSize={pageParams.pageSize}
                    total={tableData.total}
                />
            </div>
            <Modal
                width="700px"
                footer={[]}
                title={intl.formatMessage({ id: 'home_metadata_fetch_schedule' })}
                onCancel={() => {
                    setisScheduleOpen(false);
                }}
                open={isScheduleOpen}
                maskClosable={false}
            >
                <Schedule
                    onSavaEnd={() => {
                        setisScheduleOpen(false);
                    }}
                    width="100%"
                    style={{ height: 'auto' }}
                    jobId={uuid}
                    isShowPush
                    api="catalog/metadata"
                />
            </Modal>
        </>
    );
};

export default Index;
