import React from "react";
import {Popover, Input, Tabs} from "antd";
import {Icon} from "@ant-design/compatible";
// import Icon, * as AllIcon from '@ant-design/icons';
import "@ant-design/compatible/assets/index.css";
import "./style.less";
import iconsData from "./icons";

const {TabPane} = Tabs;


export default class IconSelector extends React.Component {
  static getDerivedStateFromProps(nextProps) {
    if ("value" in nextProps) {
      return {
        ...(nextProps.value || {}),
      };
    }
    return null;
  }

  constructor(props) {
    super(props);
    const value = props.value || "";
    this.state = {
      value,
      activeKey: "universal",
      visible: false,
    };
  }

  componentDidMount() {
    iconsData.map(item => {
      item.icons.map(icon => {
        if (icon === this.props.value) {
          this.setState({
            activeKey: item.type,
          });
        }
      });
    });
  }

  handleChangeAction = value => {
    if ("value" in this.props) {
      this.setState({value, visible: false});
    }
    this.triggerChange(value);
  };

  triggerChange = (value) => {
    const onChange = this.props.onChange;
    if (onChange) {
      onChange(value);
    }
  };

  handleVisibleChange = visible => {
    this.setState({visible});
  };

  handleChange = activeKey => {
    this.setState({
      activeKey,
    });
  };


  render() {
    const {value, activeKey} = this.state;

    const icon_box = (
      <div className="icon-selector">
        <Tabs onChange={this.handleChange} activeKey={activeKey} type="card" size="small">
          {
            iconsData.map(item => {
              return <TabPane tab={item.name} key={item.type}>
                <div style={{
                  overflowX: "hidden",
                  overflowY: "auto",
                  height: 220,
                }}>
                  {item.icons.map(icon => (
                    <Icon
                      className="icon-selector-wrapper"
                      key={icon} type={icon} style={value === icon ? {color: "#40a9ff"} : null}
                      onClick={() => this.handleChangeAction(icon)}/>
                  ))}
                </div>
              </TabPane>;
            })
          }
        </Tabs>
      </div>
    );
    return (
      <div>
        <Popover
          visible={this.state.visible}
          onVisibleChange={this.handleVisibleChange}
          trigger="click"
          content={icon_box}
        >
          <Input type="primary"
                 placeholder='请选择图标'
                 addonBefore={value && <Icon type={value} style={{color: "#40a9ff"}}/>}
                 onChange={this.handleChangeAction}
                 value={value}/>
        </Popover>
      </div>
    );
  }
}
