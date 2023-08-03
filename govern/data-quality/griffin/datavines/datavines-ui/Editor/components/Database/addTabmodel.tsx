import React, { useState } from 'react';
import {
    Button, Tabs, Tag, Table, Modal, Form, Input,
} from 'antd';
import { ReloadOutlined, RightOutlined } from '@ant-design/icons';
import './index.less';

const Index = () => {
    const initialItems = [
        { label: 'Tab 1', children: '', key: '1' },
        { label: 'Tab 2', children: '', key: '2' },
    ];
    const initialTableItems = [
        { label: 'Tables', children: '', key: '1' },
        { label: 'Schema Changes', children: '', key: '2' },
    ];
    const [activeKey, setActiveKey] = useState(initialItems[0].key);
    const [activeTableKey] = useState(initialTableItems[0].key);
    const [items, setItems] = useState(initialItems);
    const onChange = (newActiveKey: string) => {
        setActiveKey(newActiveKey);
    };

    const remove = (targetKey: string) => {
        let newActiveKey = activeKey;
        let lastIndex = -1;
        items.forEach((item, i) => {
            if (item.key === targetKey) {
                lastIndex = i - 1;
            }
        });
        const newPanes = items.filter((item) => item.key !== targetKey);
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

    const onEdit = (targetKey: any, action: 'add' | 'remove') => {
        if (action === 'add') {
            setIsModalOpen(true);
        } else {
            remove(targetKey);
        }
    };
    const leftOption = (
        <div className="dv-database-name">
            <span>数据库名</span>
            <span>dataBase</span>
        </div>
    );
    const operations = {
        left: leftOption,
        right: <Button size="small" type="primary" shape="circle" icon={<ReloadOutlined />} />,
    };
    const dataSource = [
        {
            key: '1',
            name: '胡彦斌',
            age: 32,
            address: '西湖区湖底公园1号',
        },
        {
            key: '2',
            name: '胡彦祖',
            age: 42,
            address: '西湖区湖底公园1号',
        },
    ];

    const columns = [
        {
            title: 'Table',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: 'Last Refresh Time',
            dataIndex: 'age',
            key: 'age',
        },
        {
            title: 'Columns',
            dataIndex: 'address',
            key: 'address',
        },
        {
            title: 'Metrics',
            dataIndex: 'metrics',
            key: 'address',
        },
    ];
    const onFinish = (values: any) => {
        console.log('Success:', values);
    };

    const onFinishFailed = (errorInfo: any) => {
        console.log('Failed:', errorInfo);
    };
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleOk = () => {
        setIsModalOpen(false);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
    };

    return (

        <div className="dv-database">
            <p className="dv-database-title">
                <span>数据源</span>
                <RightOutlined />
                <span>数据库</span>
            </p>
            <Tabs
                type="editable-card"
                size="small"
                onChange={onChange}
                activeKey={activeKey}
                onEdit={onEdit}
                items={items}
                tabBarExtraContent={operations}
            />
            <div className="dv-database-label">
                <div>
                    <p>上次扫描时间</p>
                    <p>
                        <Tag color="success">1天前</Tag>
                    </p>
                </div>
                <div>
                    <p>表数量</p>
                    <p>
                        <Tag color="processing">4</Tag>
                    </p>
                </div>
                <div>
                    <p>标签数</p>
                    <p>
                        <Tag color="default">3</Tag>
                    </p>
                </div>
                <div>
                    <p>规则数</p>
                    <p>
                        <Tag color="warning">2</Tag>
                    </p>
                </div>
                <div>
                    <p>使用热度</p>
                    <p>
                        <Tag color="error">1</Tag>
                    </p>
                </div>
            </div>
            <div className="dv-database-table">
                <Tabs
                    size="small"
                    activeKey={activeTableKey}
                    items={initialTableItems}
                />
                <Table dataSource={dataSource} columns={columns} />
            </div>

            <Modal title="新增标签" open={isModalOpen} onOk={handleOk} onCancel={handleCancel}>
                <Form
                    name="basic"
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 20 }}
                    initialValues={{ remember: true }}
                    onFinish={onFinish}
                    onFinishFailed={onFinishFailed}
                    autoComplete="off"
                >
                    <Form.Item
                        label="标签名"
                        name="name"
                        rules={[{ required: true, message: '请输入标签名!' }]}
                    >
                        <Input autoComplete="off" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Index;
