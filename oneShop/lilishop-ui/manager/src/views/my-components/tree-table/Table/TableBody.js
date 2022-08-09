import Checkbox from '../Checkbox/Checkbox'; // eslint-disable-line
// import Radio from '../Radio/Radio'; // eslint-disable-line
import { mixins } from './utils';
import { Radio } from 'view-design'; // eslint-disable-line

/* eslint-disable no-underscore-dangle */
export default {
  name: 'TreeTable__body',
  mixins: [mixins],
  components: { Radio },
  data() {
    return {
      radioSelectedIndex: -1,
    };
  },
  computed: {
    table() {
      return this.$parent;
    },
  },
  methods: {
    toggleStatus(type, row, rowIndex, value) {
      this.validateType(type, ['Expanded', 'Checked', 'Hide', 'Fold'], 'toggleStatus', false);
      const target = this.table.bodyData[rowIndex];
      this.table.bodyData.splice(rowIndex, 1, {
        ...target,
        [`_is${type}`]: typeof value === 'undefined' ? !row[`_is${type}`] : value,
      });
    },
    getChildrenIndex(parentLevel, parentIndex, careFold = true) {
      const data = this.table.bodyData;
      let childrenIndex = [];
      for (let i = parentIndex + 1; i < data.length; i++) {
        if (data[i]._level <= parentLevel) break;
        if (data[i]._level - 1 === parentLevel) {
          childrenIndex.push(i);
        }
      }
      const len = childrenIndex.length; // important!!!
      if (len > 0) {
        for (let i = 0; i < len; i++) {
          const childData = data[childrenIndex[i]];
          if (
            childData._childrenLen &&
            (!careFold || (careFold && !childData._isFold))
          ) {
            childrenIndex = childrenIndex.concat(
              this.getChildrenIndex(childData._level, childrenIndex[i], careFold));
          }
        }
      }
      return childrenIndex;
    },
    handleEvent($event, type, data, others) {
      const certainType = this.validateType(type, ['cell', 'row', 'checkbox', 'icon', 'radio'], 'handleEvent');
      const eventType = $event ? $event.type : '';
      const { row, rowIndex, column, columnIndex } = data;
      const latestData = this.table.bodyData;
      // Checkbox
      if (certainType.checkbox) {
        const { isChecked } = others;
        this.toggleStatus('Checked', row, rowIndex, isChecked);
        if (row._childrenLen > 0) {
          const childrenIndex = this.getChildrenIndex(row._level, rowIndex, false);
          for (let i = 0; i < childrenIndex.length; i++) {
            this.toggleStatus('Checked', latestData[childrenIndex[i]], childrenIndex[i], isChecked);
          }
        }
        return this.table.$emit('checkbox-click', latestData[rowIndex], column, columnIndex, $event);
      }
      // Radio
      if (certainType.radio) {
        this.radioSelectedIndex = rowIndex;
        return this.table.$emit('radio-click', { row, rowIndex, column, columnIndex, $event });
      }
      // Tree's icon
      if (certainType.icon) {
        $event.stopPropagation();
        this.toggleStatus('Fold', row, rowIndex);
        const childrenIndex = this.getChildrenIndex(row._level, rowIndex);
        for (let i = 0; i < childrenIndex.length; i++) {
          this.toggleStatus('Hide', latestData[childrenIndex[i]], childrenIndex[i]);
        }
        return this.table.$emit('tree-icon-click', latestData[rowIndex], column, columnIndex, $event);
      }
      if (certainType.cell && eventType === 'click') {
        // 点击扩展单元格
        if (this.isExpandCell(this.table, columnIndex)) {
          this.toggleStatus('Expanded', row, rowIndex);
          return this.table.$emit('expand-cell-click', latestData[rowIndex], column, columnIndex, $event);
        }
      }
      // 行：Hover
      if (certainType.row && (eventType === 'mouseenter' || eventType === 'mouseleave')) {
        const { hover } = others;
        const target = latestData[rowIndex];
        latestData.splice(rowIndex, 1, {
          ...target,
          _isHover: hover,
        });
      }
      if (certainType.row && others && others.clickRow && certainType.radio) {
        this.radioSelectedIndex = rowIndex;
        return this.table.$emit('radio-click', { row, rowIndex, column, columnIndex, $event });
      }
      if (certainType.cell) {
        return this.table.$emit(`${type}-${eventType}`, latestData[rowIndex], rowIndex, column, columnIndex, $event);
      }
      return this.table.$emit(`${type}-${eventType}`, latestData[rowIndex], rowIndex, $event);
    },
  },
  render() {
    // key
    // function getKey(row, rowIndex) {
    //   const rowKey = this.table.rowKey;
    //   if (rowKey) {
    //     return rowKey.call(null, row, rowIndex);
    //   }
    //   return rowIndex;
    // }

    // style
    function getStyle(type, row, rowIndex, column, columnIndex) {
      const certainType = this.validateType(type, ['cell', 'row'], 'getStyle');
      const style = this.table[`${type}Style`];
      if (typeof style === 'function') {
        if (certainType.row) {
          return style.call(null, row, rowIndex);
        }
        if (certainType.cell) {
          return style.call(null, row, rowIndex, column, columnIndex);
        }
      }
      return style;
    }

    // className
    function getClassName(type, row, rowIndex, column, columnIndex) {
      const certainType = this.validateType(type, ['cell', 'row', 'inner'], 'getClassName');
      const classList = [];
      if (column && column.key == "_normalIndex") {
        classList.push(`${this.prefixCls}--center-cell`);
      }
      // console.log(certainType.inner)
      if (certainType.row || certainType.cell) {
        const className = this.table[`${type}ClassName`];
        if (typeof className === 'string') {
          classList.push(className);
        } else if (typeof className === 'function') {
          if (certainType.row) {
            classList.push(className.call(null, row, rowIndex) || '');
          }
          if (certainType.cell) {
            classList.push(className.call(null, row, rowIndex, column, columnIndex) || '');
          }
        }
        if (certainType.row) {
          classList.push(`${this.prefixCls}__body-row`);
          if (this.table.stripe && rowIndex % 2 !== 0) {
            classList.push(`${this.prefixCls}--stripe-row`);
          }
          if (this.table.showRowHover && row._isHover) {
            classList.push(`${this.prefixCls}--row-hover`);
          }
        }
        if (certainType.cell) {
          classList.push(`${this.prefixCls}__body-cell`);
          if (this.table.border) {
            classList.push(`${this.prefixCls}--border-cell`);
          }
          const align = column.align;
          if (['center', 'right'].indexOf(align) > -1) {
            classList.push(`${this.prefixCls}--${align}-cell`);
          }
        }
      }
      if (certainType.inner) {
        classList.push(`${this.prefixCls}__cell-inner`);
        if (this.isExpandCell(this.table, columnIndex)) {
          classList.push(`${this.prefixCls}--expand-inner`);
          if (row._isExpanded) {
            classList.push(`${this.prefixCls}--expanded-inner`);
          }
        }
      }
      return classList.join(' ');
    }

    // 根据type渲染单元格Cell
    function renderCell(row, rowIndex, column, columnIndex) {
      // ExpandType
      if (this.isExpandCell(this.table, columnIndex)) {
        return <i class='zk-icon zk-icon-angle-right'></i>;
      }
      // SelectionType's Checkbox
      if (this.isSelectionCell(this.table, columnIndex)) {
        let res = null;
        if (this.table.selectType === 'checkbox') {
          let allCheck;
          let childrenIndex;
          const hasChildren = row._childrenLen > 0;
          if (hasChildren) {
            childrenIndex = this.getChildrenIndex(row._level, rowIndex, false);
            allCheck = true;
            for (let i = 0; i < childrenIndex.length; i++) {
              if (!this.table.bodyData[childrenIndex[i]]._isChecked) {
                allCheck = false;
                break;
              }
            }
          } else {
            allCheck = row._isChecked;
          }
          let indeterminate = false;
          if (hasChildren && !allCheck) {
            for (let i = 0; i < childrenIndex.length; i++) {
              if (this.table.bodyData[childrenIndex[i]]._isChecked) {
                indeterminate = true;
                break;
              }
            }
          }
          // res = <Checkbox
          //   indeterminate={indeterminate}
          //   value={allCheck}
          //   onOn-change={isChecked => this.handleEvent(null, 'checkbox', { row, rowIndex, column, columnIndex }, { isChecked })}>
          // </Checkbox>;
        } else {
          res = <Radio value={this.radioSelectedIndex === rowIndex} on-on-change={() => this.handleEvent(null, 'radio', { row, rowIndex, column, columnIndex })}></Radio>;
        }
        return res;
      }

      // Tree's firstProp
      if (this.table.treeType && this.table.firstProp === column.key) {
        return <span
          class={`${this.prefixCls}--level-${row._level}-cell`}
          style={{
            marginLeft: `${(row._level - 1) * 24}px`,
            paddingLeft: row._childrenLen === 0 ? '20px' : '',
          }}>
          {row._childrenLen > 0 &&
            <i
              class={`${this.prefixCls}--tree-icon zk-icon zk-icon-${row._isFold ? 'plus' : 'minus'}-square-o`}
              on-click={$event => this.handleEvent($event, 'icon', { row, rowIndex, column, columnIndex }, { isFold: row._isFold })}></i>
          }
          {row[column.key] ? row[column.key] : ''}
        </span>;
      }
      // TreeType children's index
      if (this.table.showIndex && this.table.treeType && column.key === '_normalIndex' && row._level > 1) {
        return '';
      }
      if (column.type === undefined || column.type === 'custom') {
        return row[column.key];
      } else if (column.type === 'template') {
        return this.table.$scopedSlots[column.template]
          ? this.table.$scopedSlots[column.template]({ row, rowIndex, column, columnIndex })
          : '';
      }
      return '';
    }

    // Template
    return (
      <table cellspacing="0" cellpadding="0" border="0" class={`${this.prefixCls}__body`}>
        {/* <colgroup>
          {this.table.tableColumns.map(column =>
            <col width={column.computedWidth || column.minWidth || column.width}></col>)
          }
        </colgroup> */}
        <tbody>
          {  this.table.bodyData.length > 0
            ? this.table.bodyData.map((row, rowIndex) =>
              [
                <tr
                  v-show={!row._isHide}
                  key={`table_row_${rowIndex}`}
                  style={getStyle.call(this, 'row', row, rowIndex)}
                  class={getClassName.call(this, 'row', row, rowIndex)}
                  on-click={$event => this.handleEvent($event, 'row', { row, rowIndex }, { clickRow: true })}
                  on-dblclick={$event => this.handleEvent($event, 'row', { row, rowIndex })}
                  on-contextmenu={$event => this.handleEvent($event, 'row', { row, rowIndex })}
                  on-mouseenter={$event => this.handleEvent($event, 'row', { row, rowIndex }, { hover: true })}
                  on-mouseleave={$event => this.handleEvent($event, 'row', { row, rowIndex }, { hover: false })}>
                  {   this.table.tableColumns.map((column, columnIndex) =>
                  column.key ?  <td
                      style={getStyle.call(this, 'cell', row, rowIndex, column, columnIndex)}
                      class={getClassName.call(this, 'cell', row, rowIndex, column, columnIndex)}
                      on-click={$event => this.handleEvent($event, 'cell', { row, rowIndex, column, columnIndex })}
                      on-dblclick={$event => this.handleEvent($event, 'cell', { row, rowIndex, column, columnIndex })}
                      on-contextmenu={$event => this.handleEvent($event, 'cell', { row, rowIndex, column, columnIndex })}
                      on-mouseenter={$event => this.handleEvent($event, 'cell', { row, rowIndex, column, columnIndex })}
                      on-mouseleave={$event => this.handleEvent($event, 'cell', { row, rowIndex, column, columnIndex })}>
                      <div class={getClassName.call(this, 'inner', row, rowIndex, column, columnIndex)}>
                        {renderCell.call(this, row, rowIndex, column, columnIndex)}
                      </div>
                    </td>:"")
                  }
                </tr>,
                this.table.expandType && row._isExpanded &&
                <tr
                  key={rowIndex}
                  class={`${this.prefixCls}__body-row ${this.prefixCls}--expand-row`}>
                  <td
                    class={`${this.prefixCls}--expand-content`}
                    colspan={this.table.tableColumns.length}>
                    {this.table.$scopedSlots.expand
                      ? this.table.$scopedSlots.expand({ row, rowIndex })
                      : ''
                    }
                  </td>
                </tr>,
              ])
            : <tr
              class={`${this.prefixCls}--empty-row`}>
              <td
                class={`${this.prefixCls}__body-cell ${this.prefixCls}--empty-content`}
                colspan={this.table.tableColumns.length}>
                {this.table.emptyText}
              </td>
            </tr>
          }
        </tbody>
      </table>
    );
  },
};
