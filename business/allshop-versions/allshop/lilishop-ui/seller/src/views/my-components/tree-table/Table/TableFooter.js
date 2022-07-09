import { mixins } from './utils';

/* eslint-disable no-underscore-dangle */
export default {
  name: 'TreeTable__footer',
  mixins: [mixins],
  data() {
    return {

    };
  },
  computed: {
    table() {
      return this.$parent;
    },
  },
  methods: {

  },
  render() {
    // 计算各列总和
    function renderCell({ key }, columnIndex) {
      if (columnIndex === 0) {
        return this.table.sumText;
      }
      const rows = this.table.bodyData;
      const values = rows.map(row => Number(row[key]));
      const precisions = [];
      let notNumber = true;
      values.forEach((value) => {
        if (!isNaN(value)) {
          notNumber = false;
          const decimal = value.toString().split('.')[1];
          precisions.push(decimal ? decimal.length : 0);
        }
      });
      const precision = Math.max.apply(null, precisions);
      if (!notNumber) {
        return values.reduce((prev, curr) => {
          const value = Number(curr);
          if (!isNaN(value)) {
            return parseFloat((prev + curr).toFixed(precision));
          }
          return prev;
        }, 0);
      }
      return '';
    }

    // className
    function getClassName() {
      const classList = [];
      classList.push(`${this.prefixCls}__footer-cell`);
      if (this.table.border) {
        classList.push(`${this.prefixCls}--border-cell`);
      }
      return classList.join(' ');
    }

    // Template
    return (
      <table cellspacing="0" cellpadding="0" border="0" class={ `${this.prefixCls}__footer` }>
        <colgroup>
          { this.table.tableColumns.map(column =>
            <col width={ column.computedWidth || column.minWidth || column.width }></col>)
          }
        </colgroup>
        <tfoot>
          <tr class={ `${this.prefixCls}__footer-row` }>
            { this.table.tableColumns.map((column, columnIndex) =>
              <td class={ getClassName.call(this) }>
                <div class={ `${this.prefixCls}__cell-inner` }>
                  { this.table.summaryMethod
                    ? this.table.summaryMethod(this.table.bodyData, column, columnIndex)
                    : renderCell.call(this, column, columnIndex) }
                </div>
              </td>)
            }
          </tr>
        </tfoot>
      </table>
    );
  },
};
