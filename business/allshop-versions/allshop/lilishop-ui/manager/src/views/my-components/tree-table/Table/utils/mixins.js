export default {
  data() {
    return {
      prefixCls: 'zk-table',
    };
  },
  methods: {
    validateType(type, validTypes, funcName, isReturn = true) {
      if (validTypes.indexOf(type) < 0) throw new Error(`${funcName}'s type must is ${validTypes.join(' or ')}.`);
      if (isReturn) {
        const certainType = {};
        validTypes.forEach((item) => {
          certainType[item] = item === type;
        });
        return certainType;
      }
      return true;
    },
    isExpandCell(table, columnIndex) {
      return table.expandType && (
        (table.showIndex && columnIndex === 1) ||
        (!table.showIndex && columnIndex === 0)
      );
    },
    isSelectionCell(table, columnIndex) {
      return table.selectable && (
        (table.showIndex && table.expandType && columnIndex === 2) ||
        (!table.showIndex && table.expandType && columnIndex === 1) ||
        (table.showIndex && !table.expandType && columnIndex === 1) ||
        (!table.showIndex && !table.expandType && columnIndex === 0)
      );
    },
  },
};
