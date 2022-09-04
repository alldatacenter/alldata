import React from "react";
import { Carousel,Button } from "antd"
import components_props from '../COMPONENTS_PROPS'

class CarouselCompFour extends React.Component {
    static defaultProps = {
        ...components_props,   
        widgetConfig:{
            backgroundImgList:'',
            autoPlay: true
        }
    }
    constructor(props) {
        super(props)
    }
    render() {
        let { widgetConfig = {} } = this.props;
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
                    <img style={{ width: '100%', height: '100%' }} src={img}></img>
                </a>)
            }
        </Carousel>
    }
}


export default CarouselCompFour;