import React from "react";
import { CloseOutlined } from '@ant-design/icons';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { Popover } from "antd";
import "brace/mode/javascript";
import "brace/theme/monokai";
import AceViewer from "./AceViewer";
import JSONSchemaItem from "./JSONSchemaItem";

export default class PopoverAceEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      visible: false,
    };
  }

  handleClick = () => {
    this.setState({
      visible: !this.state.visible,
    });
  };
  render() {
    let { visible } = this.state;
    let { model, value,onChange } = this.props;
    return (
      <div>
        <Popover
          content={
            <div style={{ width: 560 }}>
              <AceViewer value={value || {}} model={model} onChange={onChange} />
            </div>
          }
          title={
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <div>编辑内容</div>
              <div>
                <a onClick={this.handleClick}><CloseOutlined /></a>
              </div>
            </div>
          }
          trigger="click"
          visible={visible}
          onVisibleChange={null}
        >
          <a><LegacyIcon type={value !== model.template ? "edit" : "plus-circle"} onClick={this.handleClick} /></a>
        </Popover>
      </div>
    );
  }
}