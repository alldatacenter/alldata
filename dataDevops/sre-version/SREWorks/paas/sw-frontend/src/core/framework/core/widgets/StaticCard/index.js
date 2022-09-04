import React, { Component } from 'react';
import { EllipsisOutlined, SecurityScanOutlined } from '@ant-design/icons';
import { StatisticCard } from '@ant-design/pro-card';
import { Avatar } from 'antd';
import JSXRender from '../../JSXRender';
const { Statistic } = StatisticCard;
class StaticCard extends Component {

  render() {
    let { widgetData, widgetConfig = {} } = this.props;
    let { renderAction, backgroundImg,cardWidth, cardHeight, cardIcon = '', bordered=false } = widgetConfig;
    const data = [
      {
        value: 28,
        title: '占有率',
        status: 'success',
        trend: 'up',
        suffix: '万',
        icon: '',
      },
      {
        value: 28,
        title: '占有率',
        status: 'success',
        trend: 'down',
        suffix: '万',
        icon: '',
        link: 'href'
      },
      {
        value: 28,
        title: '占有率',
        status: 'success',
        trend: 'up',
        suffix: '万',
        icon: '',
      }
    ]
    let finalData = (widgetData && widgetData.data) || data;
    const imgStyle = {
      display: 'block',
      width: 42,
      height: 42,
    };
    const imgArr = [
      'https://gw.alipayobjects.com/mdn/rms_7bc6d8/afts/img/A*dr_0RKvVzVwAAAAAAAAAAABkARQnAQ',
      'https://gw.alipayobjects.com/mdn/rms_7bc6d8/afts/img/A*-jVKQJgA1UgAAAAAAAAAAABkARQnAQ',
      'https://gw.alipayobjects.com/mdn/rms_7bc6d8/afts/img/A*FPlYQoTNlBEAAAAAAAAAAABkARQnAQ',
      'https://gw.alipayobjects.com/mdn/rms_7bc6d8/afts/img/A*pUkAQpefcx8AAAAAAAAAAABkARQnAQ'
    ]
    return (
      <StatisticCard
        class="globalBackground"
        bordered={bordered}
        // chart={
        //   // <JSXRender jsx={cardIcon || <SecurityScanOutlined />}/>
        // }
        statistic={{
          title: (widgetData && widgetData.title) || undefined,
          value: (widgetData && widgetData.value) || undefined,
          icon:
            (backgroundImg || cardIcon || (widgetData && widgetData.icon)) ?
              <img
                style={imgStyle}
                src={backgroundImg || cardIcon || (widgetData && widgetData.icon)}
                alt="icon"
              /> : <Avatar style={{ fontSize: 26, width: 48, height: 48, lineHeight: "48px", backgroundColor: 'var(--PrimaryColor)', verticalAlign: 'middle' }} size="large">{(widgetData && widgetData.value && widgetData.value[0])}</Avatar>
          ,
        }}
        footer={
          finalData && finalData.map(item => {
            if (item.link) {
              return <a href={item.link || '#'}>
                <Statistic trend={item.trend || ''} status={item.status || ''} icon={item.icon || ''} value={item.value} title={item.title || 'name'} suffix={item.suffix || '万'} layout="horizontal" />
              </a>
            }
            return <Statistic className='static-custom' trend={item.trend || ''} status={item.status || ''} icon={item.icon || ''} value={item.value} title={item.title || 'name'} suffix={item.suffix || '万'} layout="horizontal" />
          })
        }
        style={{ width: (cardWidth && Number(cardWidth)) || null, height: (cardHeight && Number(cardHeight)) || 'auto' }}
      />
    );
  }
}

export default StaticCard;