<template lang="html">
  <div
    v-if="columns.length > 0"
    ref="table"
    :class="[prefixCls, `${prefixCls}-${size}`, tableClass]">
    <Spin fix v-if="loading"></Spin>
    <div
      v-show="showHeader"
      ref="header-wrapper"
      :class="`${prefixCls}__header-wrapper`"
      @mousewheel="handleEvent('header', $event)">
      <table-header
        ref="table-header">
      </table-header>
    </div>
    <div
      ref="body-wrapper"
      :style="bodyWrapperStyle"
      :class="`${prefixCls}__body-wrapper`"
      @scroll="handleEvent('body', $event)">
      <table-body
        ref="table-body"
        :class="bodyClass">
      </table-body>
    </div>
    <div
      v-show="showSummary && data.length > 0"
      ref="footer-wrapper"
      :class="`${prefixCls}__footer-wrapper`"
      @mousewheel="handleEvent('footer', $event)">
      <table-footer
        ref="table-footer">
      </table-footer>
    </div>
  </div>
</template>

<script>
import TableHeader from "./TableHeader";
import TableBody from "./TableBody";
import TableFooter from "./TableFooter";
import { mixins, scrollBarWidth as getSbw } from "./utils";

/* eslint-disable no-underscore-dangle */
/* eslint-disable no-param-reassign */

// function getBodyData(data, isTreeType, childrenProp, isFold, level = 1) {
function getBodyData(
  primaryKey,
  oldBodyData,
  data,
  isTreeType,
  childrenProp,
  isFold,
  parentFold,
  level = 1
) {
  let bodyData = [];
  data.forEach((row, index) => {
    const children = row[childrenProp];
    const childrenLen =
      Object.prototype.toString.call(children).slice(8, -1) === "Array"
        ? children.length
        : 0;
    let curIsFold = isFold;
    if (
      isFold &&
      typeof primaryKey === "string" &&
      Array.isArray(oldBodyData)
    ) {
      for (let i = 0; i < oldBodyData.length; i++) {
        const oldRow = oldBodyData[i];
        if (oldRow[primaryKey] === row[primaryKey]) {
          if ("_isFold" in oldRow) {
            curIsFold = oldRow._isFold;
          }
          break;
        }
      }
    }
    bodyData.push({
      _isHover: false,
      _isExpanded: false,
      _isChecked: false,
      _level: level,
      // _isHide: isFold ? level !== 1 : false,
      // _isFold: isFold,
      _isHide: level !== 1 ? isFold && parentFold : false,
      _isFold: isFold && curIsFold,
      _childrenLen: childrenLen,
      _normalIndex: index + 1,
      ...row
    });
    if (isTreeType) {
      if (childrenLen > 0) {
        // bodyData = bodyData.concat(getBodyData(children, true, childrenProp, isFold, level + 1));
        bodyData = bodyData.concat(
          getBodyData(
            primaryKey,
            oldBodyData,
            children,
            true,
            childrenProp,
            isFold,
            curIsFold,
            level + 1
          )
        );
      }
    }
  });
  return bodyData;
}

function initialState(table, expandKey) {
  return {
    bodyHeight: "auto",
    firstProp: expandKey || (table.columns[0] && table.columns[0].key),
    // bodyData: getBodyData(table.data, table.treeType, table.childrenProp, table.isFold),
    bodyData: getBodyData(
      table.primaryKey,
      table.bodyData,
      table.data,
      table.treeType,
      table.childrenProp,
      table.isFold,
      false
    )
  };
}

function initialColumns(table, clientWidth) {
  let columnsWidth = 0;
  const minWidthColumns = [];
  const otherColumns = [];
  const columns = table.columns.concat();
  if (table.expandType) {
    columns.unshift({
      width: "50"
    });
  }
  if (table.selectable) {
    columns.unshift({
      width: "50"
    });
  }
  if (table.showIndex) {
    columns.unshift({
      width: "50px",
      key: "_normalIndex",
      title: table.indexText
    });
  }
  columns.forEach((column, index) => {
    let width = "";
    let minWidth = "";
    if (!column.width) {
      if (column.minWidth) {
        minWidth =
          typeof column.minWidth === "number"
            ? column.minWidth
            : parseInt(column.minWidth, 10);
      } else {
        minWidth = 80;
      }
      minWidthColumns.push({
        ...column,
        minWidth,
        _index: index
      });
    } else {
      width =
        typeof column.width === "number"
          ? column.width
          : parseInt(column.width, 10);
      otherColumns.push({
        ...column,
        width,
        _index: index
      });
    }
    columnsWidth += minWidth || width;
  });
  const scrollBarWidth = getSbw();
  const totalWidth = columnsWidth + scrollBarWidth;
  const isScrollX = totalWidth > clientWidth;
  if (!isScrollX) {
    const extraWidth = clientWidth - totalWidth;
    const averageExtraWidth = Math.floor(extraWidth / minWidthColumns.length);
    minWidthColumns.forEach(column => {
      column.computedWidth = column.minWidth + averageExtraWidth;
    });
  }
  const tableColumns = otherColumns.concat(minWidthColumns);
  tableColumns.sort((a, b) => a._index - b._index);
  return tableColumns;
}

export default {
  name: "TreeTable",
  mixins: [mixins],
  components: {
    TableHeader,
    TableBody,
    TableFooter
  },
  props: {
    data: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    size: {
      default() {
        return !this.$IVIEW || this.$IVIEW.size === ""
          ? "default"
          : this.$IVIEW.size;
      }
    },
    loading: {
      type: Boolean,
      default: false
    },
    maxHeight: {
      type: [String, Number],
      default: "auto"
    },
    stripe: {
      type: Boolean,
      default: false
    },
    border: {
      type: Boolean,
      default: false
    },
    treeType: {
      type: Boolean,
      default: true
    },
    childrenProp: {
      type: String,
      default: "children"
    },
    isFold: {
      type: Boolean,
      default: true
    },
    expandType: {
      type: Boolean,
      default: true
    },
    selectable: {
      type: Boolean,
      default: true
    },
    selectType: {
      type: String,
      default: "checkbox"
    },
    emptyText: {
      type: String,
      default: "暂无数据"
    },
    showHeader: {
      type: Boolean,
      default: true
    },
    showIndex: {
      type: Boolean,
      default: false
    },
    indexText: {
      type: String,
      default: "#"
    },
    showSummary: {
      type: Boolean,
      default: false
    },
    sumText: {
      type: String,
      default: "合计"
    },
    primaryKey: String,
    summaryMethod: Function,
    showRowHover: {
      type: Boolean,
      default: true
    },
    rowKey: Function,
    rowClassName: [String, Function],
    cellClassName: [String, Function],
    rowStyle: [Object, Function],
    cellStyle: [Object, Function],
    expandKey: String
  },
  data() {
    return {
      computedWidth: "",
      computedHeight: "",
      tableColumns: [],
      ...initialState(this, this.expandKey)
    };
  },
  computed: {
    bodyWrapperStyle() {
      return {
        height: this.bodyHeight
      };
    },
    tableClass() {
      return {
        [`${this.prefixCls}--border`]: this.border
      };
    },
    bodyClass() {
      return {
        [`${this.prefixCls}--stripe`]: this.stripe
      };
    }
  },
  methods: {
    handleEvent(type, $event) {
      this.validateType(type, ["header", "body", "footer"], "handleEvent");
      const eventType = $event.type;
      if (eventType === "scroll") {
        this.$refs["header-wrapper"].scrollLeft = $event.target.scrollLeft;
        this.$refs["footer-wrapper"].scrollLeft = $event.target.scrollLeft;
      }
      if (eventType === "mousewheel") {
        const deltaX = $event.deltaX;
        const $body = this.$refs["body-wrapper"];
        if (deltaX > 0) {
          $body.scrollLeft += 10;
        } else {
          $body.scrollLeft -= 10;
        }
      }
      return this.$emit(`${type}-${eventType}`, $event);
    },
    // computedWidth, computedHeight, tableColumns
    measure() {
      this.$nextTick(() => {
        const { clientWidth, clientHeight } = this.$el;
        this.computedWidth = clientWidth + 2;
        this.computedHeight = clientHeight + 2;

        const maxHeight = parseInt(this.maxHeight, 10);
        if (this.maxHeight !== "auto" && this.computedHeight > maxHeight) {
          this.bodyHeight = `${maxHeight - 83}px`;
        }
        this.tableColumns = initialColumns(this, clientWidth);
      });
    },
    getCheckedProp(key = "index") {
      if (!this.selectable) {
        return [];
      }
      const checkedIndexs = [];
      this.bodyData.forEach((item, index) => {
        if (item._isChecked) {
          if (key === "index") {
            checkedIndexs.push(index);
          } else {
            checkedIndexs.push(item[key]);
          }
        }
      });
      return checkedIndexs;
    }
  },
  watch: {
    $props: {
      deep: true,
      handler() {
        Object.assign(this.$data, initialState(this, this.expandKey));
      }
    }
  },
  updated() {
    this.measure();
  },
  mounted() {
    this.measure();
    window.addEventListener("resize", this.measure);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.measure);
  }
};
</script>

<style lang="less" src="./Table.less"></style>
