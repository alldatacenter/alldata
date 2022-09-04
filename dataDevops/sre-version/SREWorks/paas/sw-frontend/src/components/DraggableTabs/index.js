import React, { Component } from "react";
import uuidv4 from "uuid/v4";
import { DragDropContext, Droppable, Draggable } from "react-beautiful-dnd";
import "@ant-design/compatible/assets/index.css";
import {
  message,
  Tabs,
  Card,
  Tooltip,
  Popover,
  Modal,
  Row,
  Col,
  Radio,
  Spin,
  Collapse,
  Select,
  Input,
} from "antd";
import "./index.scss";
import FormItemEditor from "../../core/designer/editors/FormItemEditor";
//初始化数据
const getItems = count =>
  Array.from({ length: count }, (v, k) => k).map(k => ({
    id: `item-${k + 1}`,
    content: `this is content ${k + 1}`,
  }));

// 重新记录数组顺序
const reorder = (list, startIndex, endIndex) => {
  const result = Array.from(list);

  const [removed] = result.splice(startIndex, 1);

  result.splice(endIndex, 0, removed);
  return result;
};

const grid = 8;

// 设置样式
const getItemStyle = (isDragging, draggableStyle) => ({
  // some basic styles to make the items look a bit nicer
  userSelect: "none",
  padding: 0,
  margin: 0,
  // 拖拽的时候背景变化
  background: isDragging ? "#fff" : "#ffffff",

  // styles we need to apply on draggables
  ...draggableStyle,
});


export default class DraggableTabs extends Component {
  constructor(props) {
    super(props);
    this.state = {
      order: [],
      items: getItems(11),
    };
    this.onDragEnd = this.onDragEnd.bind(this);
  }

  onDragEnd(result) {
    if (!result.destination) {
      return;
    }

    const items = reorder(
      this.props.items,
      result.source.index,
      result.destination.index,
    );
    items.map((item, index) => {
      item.order = index + 1;
    });
    this.props.onChangeTab({ items, activeKey: items[result.destination.index].name });
    this.setState({
      items,
    });
  }

  // renderTabBar=（
  renderTabBar = (props, DefaultTabBar) => {

    return <DragDropContext onDragEnd={this.onDragEnd}>
      <Droppable droppableId="droppable" direction="horizontal">
        {(provided, snapshot) => (
          <div
            {...provided.droppableProps}
            ref={provided.innerRef}
          >

          </div>
        )}
      </Droppable>
    </DragDropContext>;
  };

  handleParameterChanged = () => {

  };

  render() {
    // const {order} = this.state;
    const { children } = this.props;

    const tabs = [];
    React.Children.forEach(children, c => {
      tabs.push(c);
    });

    return <div className="draggable-tabs">
      <DragDropContext onDragEnd={this.onDragEnd}>
        <Droppable droppableId="droppable">
          {(provided) => (
            <div {...provided.droppableProps} ref={provided.innerRef}>
              <Tabs
                {...this.props}
                renderTabBar={(props, DefaultTabBar) => <div>
                  {this.props._renderTabBar}
                  <DefaultTabBar {...props}>
                    {(node) => (
                      <Draggable
                        key={node.key}
                        index={Number(node._owner.index)}
                        draggableId={node.key}
                      >
                        {(provided, snapshot) => (
                          <div
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                          >
                            {node}
                          </div>
                        )}
                      </Draggable>
                    )}
                  </DefaultTabBar>
                </div>
                }
              >
                {tabs}
              </Tabs>
              {provided.placeholder}
            </div>
          )}
        </Droppable>
      </DragDropContext>
    </div>;
  }
}

