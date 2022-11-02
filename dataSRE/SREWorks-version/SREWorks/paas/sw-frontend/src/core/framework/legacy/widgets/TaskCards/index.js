import React, { Component } from 'react';
import { Icon } from '@ant-design/compatible';


import './index.less';

class TaskCards extends Component {
    turnToPage(obj = {}) { }
    renderTaskItem = (task) => {
        const { iconType, value, title, key } = task;
        return (
            <div classname="taskCards_item" onClick={this.turnToPage(task)} key={key}>
                <div className="taskCards_item_title">{title}</div>
                <div classname="taskCards_item_body">
                    {iconType ? <Icon type={ticonType} style={{ marginRight: '5px' }} /> : null}
                    <span>{value}</span>
                </div>
            </div>
        )
    }

    render() {
        const { widgetData = [] } = this.props;
        return (
            <div class="taskCards">
                {widgetData.map(task => {
                    return this.renderTaskItem(task)
                })}
            </div>
        );
    }
}

export default TaskCards;