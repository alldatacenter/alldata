import React from "react";
import { Card } from "antd"
import JSXRender from "../../../../../components/JSXRender";
import "./index.less";

const { Meta } = Card;
function HoverableCard(props) {
  let { widgetConfig = {} } = props;
  let { cardTitle = '工作负载', cardContent, backgroundImg, cardWidth = '240', cardHeight, bordered = false } = widgetConfig;
  const randomImgArr = [
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*T_HeSIJ30IIAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*ZhzDQLMyYlYAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*mb-WQILTlSEAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ'
  ]
  let randomUrl = randomImgArr[Math.ceil(Math.random() * 5)]
  let coverImg = backgroundImg || randomUrl
  return <Card
    hoverable
    bordered={bordered}
    style={{ width: (cardWidth && Number(cardWidth)) || '100%', height: cardHeight && Number(cardHeight) }}
    cover={<img alt="example" src={coverImg} />}
  >
    <Meta title={cardTitle || "Europe Street beat"} description={cardContent || "www.instagram.com"} />
  </Card>
}

export default HoverableCard;