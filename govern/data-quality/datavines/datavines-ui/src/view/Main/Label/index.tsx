import React, { useState, useEffect } from 'react';
import {
    List, Tooltip, Button, Modal, Form, Input, message, Popconfirm,
} from 'antd';
import './index.less';
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { useIntl } from 'react-intl';
import { $http } from '@/http';
import { useSelector } from '@/store';

const Index = () => {
    // const data = [
    //     'Racing ',
    //     'Japanese ',
    //     'Australian ',
    //     'Man charged ',
    //     'Los Angeles',
    // ];
    const intl = useIntl();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();
    const showModal = (type:string) => {
        setType(type);
        setIsModalOpen(true);
    };
    type Category = {
        id:number;
        name:string;
        uuid:string;
    }
    const [tagCategoryList, setTagCategoryList] = useState<Category[]>([]);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    useEffect(() => {
        getList();
    }, []);
    const getList = async () => {
        const res = await $http.get(`catalog/tag/category/list/${workspaceId}`);
        setTagCategoryList(res);
        if (res.length > 0) {
            setCurrentIndex(0);
            getTagList(res[0].uuid);
        }
    };
    // 删除标签分类
    const deleteCategory = async (categoryUUID:string) => {
        // event.preventDefault();
        // event.stopPropagation();
        await $http.delete(`catalog/tag/category/${categoryUUID}`);
        getList();
        message.success('删除成功');
    };
    const deleteTag = async (categoryUUID:string) => {
        await $http.delete(`catalog/tag/${categoryUUID}`);
        getTagList(tagCategoryList[currentIndex].uuid);
        message.success('删除成功');
    };

    const [tagList, setTagList] = useState<{
        name:string;
        uuid:string;
    }[]>([]);
    const getTagList = async (categoryUUID:string) => {
        setTagList([]);
        const res = await $http.get(`catalog/tag/list-in-category/${categoryUUID}`);
        console.log('res', res);
        setTagList(res);
        // catalog/tag/list-in-category/{categoryUUID}
    };
    const handleOk = async () => {
        form.validateFields().then(async (value) => {
            console.log('value', value);
            if (type === 'category') {
                await $http.post('/catalog/tag/category', {
                    ...value,
                    workspaceId,
                });
                message.success(intl.formatMessage({ id: 'common_success' }));
                getList();
            } else {
                await $http.post('/catalog/tag/', {
                    ...value,
                    categoryUuid: tagCategoryList[currentIndex].uuid,
                });
                message.success(intl.formatMessage({ id: 'common_success' }));
                getTagList(tagCategoryList[currentIndex].uuid);
            }
            // createTag(value);
            // console.log("12312")

            // console.log('res', res);
            // // catalog/tag/category
            setIsModalOpen(false);
            form.resetFields();
        }).catch(() => {

        });
    };
    const [type, setType] = useState('');

    const handleCancel = () => {
        form.resetFields();
        setIsModalOpen(false);
    };
    const [currentIndex, setCurrentIndex] = useState(0);
    const getCurrentTag = (index:number) => {
        setCurrentIndex(index);
        getTagList(tagCategoryList[index].uuid);
    };
    return (
        <div className="dv-label dv-page-paddinng">
            <div>
                <p className="dv-label-title">
                    {intl.formatMessage({ id: 'label_title' })}
                    <Tooltip title={intl.formatMessage({ id: 'label_add_category' })}>
                        <Button onClick={() => showModal('category')} className="fr" size="small" style={{ marginTop: '6px' }} shape="circle" icon={<PlusOutlined />} />
                    </Tooltip>
                </p>
                {
                    tagCategoryList.map((item, index) => (
                        <div
                            onClick={() => getCurrentTag(index)}
                            className={currentIndex === index ? ' actived category-item ' : 'category-item'}
                        >
                            { item.name || '  '}
                            <div>
                                <Popconfirm
                                    title={intl.formatMessage({ id: 'common_delete_tip' })}
                                    onConfirm={() => { deleteCategory(item.uuid); }}
                                    okText={intl.formatMessage({ id: 'common_Ok' })}
                                    cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                                >
                                    <Button type="text" icon={<DeleteOutlined />} danger />
                                </Popconfirm>

                                {/* <Button type="text" icon={<EditOutlined />} /> */}

                            </div>

                        </div>
                    ))
                }

            </div>
            <div>
                <p className="dv-label-title">
                    {intl.formatMessage({ id: 'label_list' })}
                    <Tooltip title={intl.formatMessage({ id: 'label_add' })}>
                        <Button onClick={() => showModal('tag')} className="fr" size="small" style={{ marginTop: '6px' }} shape="circle" icon={<PlusOutlined />} />
                    </Tooltip>
                </p>
                <List
                    dataSource={tagList}
                    renderItem={(item) => (
                        <List.Item>
                            <div
                                className="category-item"
                                style={{ padding: '0px 20px 0px 0px' }}
                            >
                                { item.name }
                                <div>
                                    <Popconfirm
                                        title={intl.formatMessage({ id: 'common_delete_tip' })}
                                        onConfirm={() => { deleteTag(item.uuid); }}
                                        okText={intl.formatMessage({ id: 'common_Ok' })}
                                        cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                                    >
                                        <Button type="link" icon={<DeleteOutlined />} danger />
                                    </Popconfirm>
                                    {/* <Button type="link" icon={<EditOutlined />} /> */}

                                </div>
                            </div>
                        </List.Item>
                    )}
                />
            </div>
            {/* 弹窗 */}
            <Modal
                title={type === 'category' ? intl.formatMessage({ id: 'label_add_category' }) : intl.formatMessage({ id: 'label_add' })}
                open={isModalOpen}
                onOk={handleOk}
                onCancel={handleCancel}
            >
                <Form
                    labelCol={{ span: 3 }}
                    wrapperCol={{ span: 20 }}
                    initialValues={{ name: '' }}
                    autoComplete="off"
                    form={form}
                >
                    <Form.Item
                        label={intl.formatMessage({ id: 'warn_SLAs_name' })}
                        name="name"
                        rules={[{ required: true, message: intl.formatMessage({ id: 'common_input_tip' }) }]}
                    >
                        <Input />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Index;
