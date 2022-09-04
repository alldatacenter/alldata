/**
 * Created by caoshuaibiao on 2020/6/9.
 * 文件上传
 */
import { UploadOutlined } from '@ant-design/icons';

import { Upload, Button, message } from 'antd';
import React, { PureComponent } from 'react';
import properties from 'appRoot/properties';

export default class FileUpload extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            fileList: [],
            uploading: false,
        };
    }


    render() {
        const { fileList } = this.state, { onChange, model } = this.props;
        let { defModel } = model, uploadProps = {};
        if (defModel.type === 'async') {
            uploadProps = {
                name: 'file',
                withCredentials: true,
                accept: model && model.defModel && model.defModel.accept,//增加上传类型
                action: `${properties.baseUrl}gateway/sreworks/other/avatar/put`,
                onChange: (info) => {
                    let { fileList = [] } = info;
                    if (info.file.status !== 'uploading') {
                        //console.log(info.file, info.fileList);
                    }
                    //增加上传数量
                    if (model && model.defModel && model.defModel.maxLength) {
                        fileList = fileList.slice(Number("-" + model.defModel.maxLength));
                    }
                    if (['done', 'removed'].includes(info.file.status)) {
                        let fileDownList = fileList.map(file => {
                            return `${properties.baseUrl}gateway/minio/public/avatar/${file.response.data}`;
                        });
                        onChange && onChange(fileDownList);
                        message.success(`${info.file.name} file uploaded successfully`);
                    } else if (info.file.status === 'error') {
                        message.error(`${info.file.name} file upload failed.`);
                    }
                    this.setState({ fileList });
                },
                fileList: this.state.fileList,
            }
        } else {
            uploadProps = {
                onRemove: file => {
                    this.setState(state => {
                        const index = state.fileList.indexOf(file);
                        const newFileList = state.fileList.slice();
                        newFileList.splice(index, 1);
                        return {
                            fileList: newFileList,
                        };
                    });
                },
                beforeUpload: file => {
                    if (model && model.defModel && model.defModel.mode === 'multi') {
                        this.setState({
                            fileList: [...this.state.fileList, file],
                        });
                        onChange && onChange([...this.state.fileList, file]);
                    } else {
                        this.setState({
                            fileList: [file],
                        });
                        onChange && onChange([file]);
                    }
                    return false;
                },
                fileList,
            };
        }

        return (
            <div>
                <Upload {...uploadProps}>
                    <a><UploadOutlined /> 添加文件</a>
                </Upload>
            </div>
        );
    }
}

