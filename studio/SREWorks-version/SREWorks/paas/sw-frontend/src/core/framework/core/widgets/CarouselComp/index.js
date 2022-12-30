import React from "react";
import { Carousel } from "antd"
import JSXRender from "../../../../../components/JSXRender";
import properties from "../../../../../properties";
import "./index.less";

function CarouselComp(props) {
  let { widgetConfig = {} } = props;
  let { backgroundImgList, carouselHeight = '100%', carouselWidth = '100%', autoPlay, links = [] } = widgetConfig;
  const randomImgArr = [
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*T_HeSIJ30IIAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*ZhzDQLMyYlYAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*mb-WQILTlSEAAAAAAAAAAABkARQnAQ',
    'https://gw.alipayobjects.com/mdn/rms_08e378/afts/img/A*MaL2SYtHPuMAAAAAAAAAAABkARQnAQ'
  ]
  let randomUrlArr = [randomImgArr[Math.ceil(Math.random() * 5)], randomImgArr[Math.ceil(Math.random() * 5)], randomImgArr[Math.ceil(Math.random() * 5)], randomImgArr[Math.ceil(Math.random() * 5)]]
  let finalArr = backgroundImgList || randomUrlArr
  return <Carousel style={{ width: carouselWidth, height: carouselHeight }} autoplay={autoPlay}>
    {
      finalArr && finalArr.map((img, num) => <a href={links[num] || '#'}>
        <img style={{ width: '100%', height: '100%' }} src={(process.env.NODE_ENV === 'local' ? properties.baseUrl : '') + img}></img>
      </a>)
    }
  </Carousel>
}

export default CarouselComp;