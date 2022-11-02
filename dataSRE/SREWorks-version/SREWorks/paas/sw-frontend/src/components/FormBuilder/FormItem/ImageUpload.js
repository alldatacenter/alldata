import { LoadingOutlined, PlusOutlined } from "@ant-design/icons";

import { Upload, Button, message } from "antd";
import React, { PureComponent } from "react";
import properties from "../../../properties";
import cacheRepository from "../../../utils/cacheRepository";

export default class ImageUpload extends React.Component {

  constructor(props) {
    super(props);
    let { isMulti = false } = props;
    this.state = {
      fileList: [],
      uploading: false,
      loading: false,
      imageUrl: props.value,
      imgList: isMulti ? props.value : ''
    };
  }

  getBase64 = (img, callback) => {
    const reader = new FileReader();
    reader.addEventListener("load", () => callback(reader.result));
    reader.readAsDataURL(img);
  };
  handleChange = (info) => {
    let { onChange, model, isMulti = false } = this.props;
    if (info.file.status === "uploading") {
      this.setState({ loading: true });
      return;
    } else {
      this.setState({ loading: false });
    }
    if (["done", "removed"].includes(info.file.status)) {
      this.getBase64(info.file.originFileObj, imageUrl =>
        this.setState({
          imageUrl,
          loading: false,
        }),
      );
      let { fileList = [] } = info;
      let fileDownList = fileList.map(file => {
        return `${file.response.data}`;
      });
      let result = isMulti ? fileDownList : fileDownList[0]
      if (isMulti) {
        this.setState({ imgList: fileDownList })
      }
      onChange && onChange(result);
      message.success(`${info.file.name} 上传成功`);
    } else if (info.file.status === "error") {
      message.error(`${info.file.name} 上传失败`);
    }
  };
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
    const { fileList, loading, imageUrl, imgList } = this.state, { onChange, model, isMulti = false } = this.props;
    let productId = this.getProductName();
    let bizAppId = cacheRepository.getBizEnv(productId)
    const uploadButton = (
      <div>
        {loading ? <LoadingOutlined /> : <PlusOutlined />}
        <div style={{ marginTop: 8 }}>上传图片</div>
      </div>
    );

    return (
      <div>
        <Upload
          name="file"
          listType="picture-card"
          className="avatar-uploader"
          showUploadList={false}
          accept="image/*,.pdf"
          withCredentials={true}
          multiple={isMulti}
          headers={{
            'x-biz-app': bizAppId
          }}
          maxCount={isMulti ? 10 : 1}
          action={`${properties.baseUrl}gateway/sreworks/other/sreworksFile/put`}
          onChange={this.handleChange}
        >
          {!isMulti && (imageUrl && imageUrl.length !== 0 ? <img src={imageUrl} alt="avatar" style={{ maxWidth: '100%', maxHeight: '100%' }} /> : uploadButton)}
          {isMulti && (imgList && imgList.length > 0 ? <Button type='text'>重新上传</Button> : uploadButton)}
        </Upload>
        {(isMulti && imgList && imgList.length > 0) ? imgList.map(item => <img src={(process.env.NODE_ENV === 'local' ? properties.baseUrl : '') + item} alt="avatar" style={{ width: 52, height: 52, marginRight: 3 }} />) : ''}
      </div>
    );
  }
}

