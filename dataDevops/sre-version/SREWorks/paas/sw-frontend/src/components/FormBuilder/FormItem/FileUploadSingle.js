
import { UploadOutlined } from '@ant-design/icons';
import { Upload, message } from 'antd';
import React, { Component } from 'react';
import properties from 'appRoot/properties';
import cacheRepository from "../../../utils/cacheRepository";

class FileUploadSingle extends Component {

    constructor(props) {
        super(props);
        this.state = {
            fileList: [],
            uploading: false,
        };
    }
    getProductName = () => {
        let productName = window.location.hash.split("/")[1];
        if (!productName) {
            if (properties.defaultProduct && !properties.defaultProduct.includes("$")) {
                productName = properties.defaultProduct;
            } else {
                productName = 'desktop';
            }
        }
        if (productName.includes("?")) {
            productName = productName.split("?")[0];
        }
        return productName;
    }
    render() {
        const { fileList } = this.state, { onChange, model } = this.props;
        let { defModel } = model, uploadProps = {};
        let productId = this.getProductName();
        let bizAppId = cacheRepository.getBizEnv(productId)
        uploadProps = {
            name: 'file',
            withCredentials: true,
            accept: model && defModel && defModel.accept,//增加上传类型
            action: `${properties.baseUrl}gateway/sreworks/other/sreworksFile/put`,
            multiple: false,
            headers: {
                'x-biz-app': bizAppId
            },
            onChange: (info) => {
                let { fileList = [] } = info;
                if (info.file.status !== 'uploading') {
                    //console.log(info.file, info.fileList);
                }
                if (['done', 'removed'].includes(info.file.status)) {
                    let fileDownList = fileList.map(file => {
                        return `${file.response.data}`;
                    });
                    onChange && onChange(fileDownList[0]);
                    message.success(`${info.file.name} file uploaded successfully`);
                } else if (info.file.status === 'error') {
                    message.error(`${info.file.name} file upload failed.`);
                }
                this.setState({ fileList });
            },
            fileList: this.state.fileList,
        }

        return (
            <div>
                <Upload {...uploadProps}>
                    {
                        !fileList.length && <a>
                            <UploadOutlined /> 添加文件</a>
                    }
                </Upload>
            </div>
        );
    }
}



export default FileUploadSingle;
