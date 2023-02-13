import React from 'react';
import moment from 'moment';
import { Icon,Button } from 'antd';
import  { ExpandOutlined,CompressOutlined } from '@ant-design/icons'
import "./screenHeader.less"

export default class ScreenHeader extends React.PureComponent {
  // init
  constructor(props) {
    super(props);
    this.state = {
      date: moment().format('YYYY-MM-DD HH:mm:ss'),
    }
  }
  intervalPromise = null;
  // mountÍ
  componentWillMount() {
      this.intervalPromise = setInterval(()=> {
        this.setState({ date: moment().format('YYYY-MM-DD HH:mm:ss') })
      }, 990);
  }
  render() {
    const { title } = this.props;
    let { date,isFull } = this.state;
    return (
    <div style={{position:'relative'}}> 
      <Button onClick={this.requestFullScreen.bind(this)} size="small" style={{ right: 10,top: 10, position:'absolute'}}>{document.fullscreenElement? <CompressOutlined />:<ExpandOutlined />}</Button>
      <div className="screen-title">
        <h1 style={{fontSize:'2em'}}>{title}</h1>
        <p><Icon type="clock-circle-o" style={{marginRight: 5}} />{date}</p>
      </div>
    </div>
      );
  }
  componentWillUnmount() {
    clearInterval(this.intervalPromise);
  }
  requestFullScreen() {
    // this.setState({ showFull: 'none' })
    let element = document.getElementById("datav-content");

    let w = window,
      d = document,
      e = d.documentElement,
      g = d.getElementsByTagName('body')[0],
      x = w.innerWidth || e.clientWidth || g.clientWidth,
      y = w.innerHeight|| e.clientHeight|| g.clientHeight;
    element.style.width = x + "px";
    let requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      requestMethod.call(element)
    }
  }
}

