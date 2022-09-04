import React from "react";
import { Button, Spin } from "antd";
import { RedoOutlined, FullscreenOutlined } from "@ant-design/icons";
import "./index.scss";

export default class Granfana extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      disableRefresh: true,
    };
  }

  componentDidMount() {
    document.getElementById("grafanaIframe").onload = () => {
      // 控制iframe加载完成后刷新按钮才可操作
      this.setState({
        disableRefresh: false,
      });
    };
  }

  refresh = () => {
    //跨域刷新iframe
    window.open(document.all.grafanaIframe.src, "grafanaIframe", "");
  };

  requestFullScreen = () => {
    let element = document.getElementById("datav-content");
    let w = window,
      d = document,
      e = d.documentElement,
      g = d.getElementsByTagName("body")[0],
      x = w.innerWidth || e.clientWidth || g.clientWidth,
      y = w.innerHeight || e.clientHeight || g.clientHeight;

    // 兼容safari浏览器
    element.style.width = "100%";
    element.style.height = "100%";
    let requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
    if (requestMethod) {
      requestMethod.call(element);
    }
  };

  render() {
    let { widgetConfig = {}, actionParams, ...otherProps } = this.props;
    let { url } = widgetConfig;
    let { disableRefresh } = this.state;
    return <div className="abm-grafana" id="datav-content">
      <div className="operation-btn">
        <Button disabled={disableRefresh} onClick={this.refresh}><RedoOutlined /></Button>
        <Button onClick={this.requestFullScreen}><FullscreenOutlined /></Button>
      </div>
      <div className="grafana-content">
        <iframe width="100%" height="100%" src={url} id="grafanaIframe" name="grafanaIframe" />
      </div>
    </div>;
  }


}