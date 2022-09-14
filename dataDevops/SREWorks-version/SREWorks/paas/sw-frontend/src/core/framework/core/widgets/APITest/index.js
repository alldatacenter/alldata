import React, { useState, useEffect } from "react";
import { Col, Row, Form, Input, Button, message, Modal } from "antd";
import properties from "../../../../../properties";
import httpClient from "../../../../../utils/httpClient";
import JSONEditor from "../../../../../components/FormBuilder/FormItem/JSONEditor";
import CardWrapper from "../../../../../components/CardWrapper";

import _ from "lodash";
let timer = null; // 计时器
const { TextArea } = Input;
let serviceMap = {
  200: {
    text: "成功",
    color: "#52c41a",
  },
  400: {
    text: "失败",
    color: "#ff4d4f",
  },
  500: {
    text: "失败",
    color: "#ff4d4f",
  },
};
const APITest = (props) => {
  let { widgetConfig = {} } = props, sceneCodeProp = 'default', detectorCodeProp = 'drilldown';
  let { businessConfig } = widgetConfig
  sceneCodeProp = (businessConfig && businessConfig.sceneCode) || ''
  detectorCodeProp = (businessConfig && businessConfig.detectorCode) || ''
  const [inputParams, setDefaultInput] = useState();
  const [outputParams, setOutputParams] = useState();
  const [refresh, setRefresh] = useState(false);
  const [refreshOutput, setRefreshOutput] = useState(false);
  const [sceneCode] = useState(sceneCodeProp);
  const [detectorCode] = useState(detectorCodeProp);
  const [runTime, setRunTime] = useState(1);
  const [testLink, setLink] = useState();
  const [serviceCode, setCode] = useState(200);
  const [link, setPageLink] = useState();
  const [isModalVisible, setModalVisible] = useState(false);
  const [resultCode, setResultCode] = useState("");
  const initData = () => {
    getDefaultInput();
    setPageLink(`${properties.baseUrl}gateway/aiops/aisp/${sceneCode}/${detectorCode}/analyze`);
    setLink(`${properties.baseUrl}gateway/aiops/aisp/${sceneCode}/${detectorCode}/analyze`);
  };

  const getDefaultInput = () => {
    httpClient.get(`gateway/aiops/aisp/${sceneCode}/${detectorCode}/input`)
      .then(res => {
        setDefaultInput(res);
      });
  };

  const setTestRuntime = () => {
    let n = 1;
    timer = setInterval(() => {
      n++;
      setRunTime(n);
    }, 1);
  };

  const handleRunTest = () => {
    setTestRuntime();
    httpClient.post(testLink, inputParams)
      .then(res => {
        setCode(res.httpStatus);
        setOutputParams(res.data);
        message.success("执行成功");
        clearInterval(timer);
      })
      .catch(err => {
        setCode(err.response.status);
        clearInterval(timer);
        setOutputParams(err.response);
      });
  };

  const onChange = (changedFields) => {
    if (!_.isEqual(changedFields.inputParams, inputParams)) {
      setDefaultInput(changedFields.inputParams);
    }
  };

  const copyButton = (props, text) => {
    return <Button type={props.type} size={props.size} style={props.style} onClick={() => copy(text)}>复制</Button>;
  };

  const copy = (res) => {
    let copedStr = typeof res === "string" ? res : JSON.stringify(res)
    try {
      if(!navigator.clipboard){
        localStorage.setItem("copedApiParams",copedStr)
        message.success("复制成功");
      } else {
        navigator.clipboard.writeText(copedStr);
        message.success("复制成功");
      }
    } catch(error){
      message.error("复制失败")
    }  
  };
  const paste = () => {
    if(!navigator.clipboard) {
      copedStr = localStorage.getItem("copedApiParams")
      try {
        copedStr && setDefaultInput(JSON.parse(copedStr));
      } catch (e) {
        message.error("复制的数据不符合JSON格式");
      }
    } else {
      navigator.clipboard.readText().then(
        clipText => {
          try {
            setDefaultInput(JSON.parse(clipText));
          } catch (e) {
            message.error("复制的数据不符合JSON格式");
          }
        });
    }
  };

  const handleSubmit = codeType => {
    httpClient.post(`/gateway/aiops/aisp/${sceneCode}/${detectorCode}/code`, { codeType, input: inputParams,url:testLink })
      .then(res => {
        setResultCode(res);
        setModalVisible(true);
      });
  };

  const handleCancel = () => {
    setModalVisible(false);
  };

  const handleChangeOutput = res => {
    if (!_.isEqual(res, outputParams)) {
      setOutputParams(res)
    }
  };

  const handleChangeResult = e => {
    if (e.target.value) {
      setResultCode(e.target.value);
    }
  };
  useEffect(() => {
    initData();
  }, []);


  useEffect(() => {
    setRefresh(true);
    setTimeout(() => {
      setRefresh(false);
    });
  }, [inputParams]);

  useEffect(() => {
    setRefreshOutput(true);
    setTimeout(() => {
      setRefreshOutput(false);
    });
  }, [outputParams]);

  return <div style={{ marginRight: 20 }}>
    <CardWrapper title="服务地址">
      <Row span={24}>
        <Col span={20}>
          <Input disabled
            value={link} />
        </Col>
        <Col span={4} style={{ textAlign: "right" }}>
          {copyButton({ style: { marginRight: 10 } }, link)}
          <Button type="primary" onClick={handleRunTest}>测试</Button>
        </Col>
      </Row>
    </CardWrapper>
    <CardWrapper title="输入参数" extButton={<div>
      {copyButton({ style: { marginRight: 10 }, size: "small", type: "primary" }, inputParams)}
      <Button size="small" type="primary" onClick={paste}>粘贴</Button>
    </div>}>
      <Form onValuesChange={onChange} initialValues={{ inputParams }}>
        <Form.Item
          name="inputParams">
          {!refresh &&
            <JSONEditor model={{ initValue: inputParams, height: 180, defModel: { disableShowDiff: true } }} />}
        </Form.Item>
      </Form>
    </CardWrapper>
    <CardWrapper title="输出参数" extButton={<div>
      {copyButton({ size: "small", type: "primary" }, outputParams)}
    </div>}>

      {!refreshOutput &&
        <JSONEditor onChange={handleChangeOutput} model={{ initValue: outputParams, height: 180, defModel: { disableShowDiff: true } }} />}
    </CardWrapper>

    {outputParams && <div>结果：<span
      style={{ color: serviceMap[serviceCode] && serviceMap[serviceCode].color || "#ff4d4f" }}>{serviceCode}</span>&nbsp;&nbsp;耗时：<span>{runTime}ms</span>
    </div>}

    {/*todo 时序图表*/}

    <div style={{ textAlign: "right" }}>
      <Button style={{ marginRight: 10 }} type="primary" onClick={() => handleSubmit("curl")}>生成CURL命令</Button>
      <Button style={{ marginRight: 10 }} type="primary" onClick={() => handleSubmit("python")}>生成Python代码</Button>
      <Button type="primary" onClick={() => handleSubmit("java")}>生成Java代码</Button>
    </div>

    <Modal title="结果展示" width={"60%"} visible={isModalVisible} onCancel={handleCancel} footer={[
      copyButton({ size: "small", type: "primary" }, resultCode),
      <Button type="primary" size="small" onClick={handleCancel}>
        关闭
      </Button>]}>
      <TextArea onChange={handleChangeResult} value={resultCode} defaultValue={resultCode} style={{ height: 300, width: "100%" }} />
    </Modal>
  </div>;
};
export default APITest;