import React from "react";
import { Card, Avatar } from "antd"
import JSXRender from "../../../../../components/JSXRender";
import "./index.less";

function DescribtionCard(props) {
  let { widgetConfig = {} } = props;
  let { cardTitle = '工作负载', cardContent = "理解 Pods，Kubernetes 中可部署的最小计算对象，以及辅助它运行它们的高层抽象对象", imgUrl, describeLayout = 'cardLayout', cardActions = '<a href="#">详情</a>' } = widgetConfig;
  const randomImgArr = [
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*PgcWQY93DdQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*-1hUSLTEQ2sAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*TPzaQ6lFV84AAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_7d1485/afts/img/A*pVxcSaO1IYQAAAAAAAAAAAAAARQnAQ',
  ]
  if (describeLayout === 'cardLayout') {
    return (
      <Card class="card-layout"
        hoverable
        style={{ maxWidth: 400 }}
        actions={[<JSXRender jsx={cardActions || '<a href="#">详情</a>'} />]}
        cover={
          imgUrl ?
            <img
              alt="example"
              src={imgUrl || "https://gw.alipayobjects.com/zos/rmsportal/JiqGstEfoWAOHiTxclqi.png"}
            /> : <Avatar style={{ fontSize: 26, width: 48, height: 48, lineHeight: "48px", backgroundColor: 'rgb(35, 91, 157)', verticalAlign: 'middle' }} size="large">{cardTitle && cardTitle[0]}</Avatar>
        }>
        <h2 class="describe-title">{cardTitle}</h2>
        <p class="describe-content">{cardContent}</p>
      </Card>
    )
  }
  return (
    <div class="describe-card">
      <img class="describe-img" alt="icon-0" width='88' src={imgUrl || randomImgArr[Math.floor(Math.random() * 10)]}></img>
      <h2 class="describe-title">{cardTitle}</h2>
      <p class="describe-content">{cardContent}</p>
      <JSXRender class="describe-link" jsx={cardActions || '<a href="#">详情</a>'} />
    </div>
  )
}

export default DescribtionCard;