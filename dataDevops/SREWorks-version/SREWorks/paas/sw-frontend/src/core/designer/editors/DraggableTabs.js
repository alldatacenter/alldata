import React, { useRef } from 'react';
import '@ant-design/compatible/assets/index.css';
import { DndProvider, useDrag, useDrop } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import {
    Tabs,
} from 'antd';

const type = 'DraggableTabNode';
const DraggableTabNode = ({ index, children, moveNode }) => {
    const ref = useRef();
    const [{ isOver, dropClassName }, drop] = useDrop({
        accept: type,
        collect: monitor => {
            const { index: dragIndex } = monitor.getItem() || {};
            if (dragIndex === index) {
                return {};
            }
            return {
                isOver: monitor.isOver(),
                dropClassName: 'dropping',
            };
        },
        drop: item => {
            moveNode(item.index, index);
        },
    });
    const [, drag] = useDrag({
        type,
        item: { index },
        collect: monitor => ({
            isDragging: monitor.isDragging(),
        }),
    });
    drop(drag(ref));
    return (
        <div ref={ref} className={`dragnode ${isOver ? dropClassName : ''}`}>
            {children}
        </div>
    );
};
export default class DraggableTabs extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            order: [],
        }
    }
    moveTabNode = (dragKey, hoverKey) => {
        let {orderChange} = this.props;
        const newOrder = this.state.order.slice();
        const { children } = this.props;

        React.Children.forEach(children, c => {
            if (newOrder.indexOf(c.key) === -1) {
                newOrder.push(c.key);
            }
        });

        const dragIndex = newOrder.indexOf(dragKey);
        const hoverIndex = newOrder.indexOf(hoverKey);

        newOrder.splice(dragIndex, 1);
        newOrder.splice(hoverIndex, 0, dragKey);
        this.setState({
            order: newOrder,
        },()=> {
            orderChange && orderChange(dragIndex,hoverIndex)
        });
    };

    renderTabBar = (props, DefaultTabBar) => (
        <DefaultTabBar {...props}>
            {node => (
                <DraggableTabNode key={node.key} index={node.key} moveNode={this.moveTabNode}>
                    {node}
                </DraggableTabNode>
            )}
        </DefaultTabBar>
    );

    render() {
        const { order } = this.state;
        const { children } = this.props;

        const tabs = [];
        React.Children.forEach(children, c => {
            tabs.push(c);
        });

        const orderTabs = tabs.slice().sort((a, b) => {
            const orderA = order.indexOf(a.key);
            const orderB = order.indexOf(b.key);

            if (orderA !== -1 && orderB !== -1) {
                return orderA - orderB;
            }
            if (orderA !== -1) {
                return -1;
            }
            if (orderB !== -1) {
                return 1;
            }

            const ia = tabs.indexOf(a);
            const ib = tabs.indexOf(b);

            return ia - ib;
        });

        return (
            <DndProvider backend={HTML5Backend}>
                <Tabs renderTabBar={this.renderTabBar} {...this.props}>
                    {orderTabs}
                </Tabs>
            </DndProvider>
        );
    }
}