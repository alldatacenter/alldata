<template>
  <div>
    <virtual-tree
      :list="treeData"
      :render="renderNode"
      :open="openNode"
      :height="height"
      @we-click="onClick"
      @we-contextmenu="onContextMenu"
      @we-open-node="openNodeChange"
      @we-dblclick="handledbclick"/>
    <Spin
      v-if="loading"
      size="large"
      fix/>
  </div>
</template>
<script>
import VirtualTree from '@/components/virtualTree';
export default {
  name: 'DataSoureceTree',
  components: {
    VirtualTree,
  },
  props: {
    data: Array,
    openNode: Object,
    filterText: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      treeData: [],
      height: 0,
      currentActive: null
    }
  },
  watch: {
    filterText() {
      this.getTreeData()
    },
    data() {
      this.getTreeData()
    },
  },
  mounted() {
    this.height = this.$el.clientHeight
    window.addEventListener('resize', this.resize);
  },
  methods: {
    recursion(node, text, isCaseSensitive = false){
      node.forEach((child) => {
        let name = child.name;
        let searchText = text;
        // 是否对大小写敏感
        if (!isCaseSensitive) {
          name = child.name.toLowerCase();
          searchText = text.toLowerCase();
        }
        child.isVisible = name.indexOf(searchText) > -1
        // 获取数据库中是否有符合搜索条件的表，有的话得显示库名
        if (child.dataType === 'db') {
          if (child.children && child.children.length) {
            this.recursion(child.children, text, isCaseSensitive);
            let hasVisible = child.children.some((item)=>item.isVisible === true)
            if (hasVisible) {
              child.isVisible = true;
            }
          }
        } else if (child.dataType == 'tb') {
          if (child.children && child.children.length) {
            child.children.forEach(it=>it.isVisible = child.isVisible)
          }
        }
      });
    },
    openNodeChange() {
    },
    onClick({item}) {
      let node = this.nodeItem(item._id, this.treeData)
      if (!node) return
      this.currentActive = node
      this.$emit('we-click', {item: node})
    },
    onContextMenu(/*{ev, item}*/) {
    //   let node = this.nodeItem(item._id, this.treeData)
    //   if (!node) return
    //   this.$emit('we-contextmenu', {ev, item: node, isOpen: this.openNode[item._id]})
    },
    handledbclick(data) {
      this.$emit('we-dblclick', data)
    },
    /**
     * 递归查找点击的对应树节点
     */
    nodeItem(_id, data) {
      let node
      let helperFn = (list) => {
        for(let i=0,len=list.length;i<len;i++) {
          if (node) return
          if (list[i]._id === _id) {
            return node = list[i]
          }
          if(list[i].children) {
            helperFn(list[i].children)
          }
        }
      }
      helperFn(data)
      return node
    },
    getTreeData() {
      let treeData = []
      if(this.timer) {
        clearTimeout(this.timer)
      }
      this.timer = setTimeout(() => {
        this.recursion(this.data, this.filterText)
        treeData = [...this.data]
        this.treeData = treeData
      }, 100);
    },
    /**
     * 自定义渲染节点
     */
    renderNode(h, params) {
      let item = params.item
      let activeNode = ''
      if (this.currentActive) {
        activeNode = this.currentActive._id === item._id ? 'active-node' : ''
      } else if(item.active) {
        activeNode = 'active-node'
      }
      if (item.dataType === 'field') { // 表字段
        return h('span', {
          class: `node-name ${activeNode}`
        }, [
          h('span', {
            class: 'v-hivetable-type',
            attrs: {
              title: item.type
            }
          }, [`[${item.type}]`]),
          h('span', {
            class: 'v-hivetable-text',
            attrs: {
              title: item.name
            }
          }, [item.name])
        ])
      } else {
        return h('span', {
          class: `node-name ${activeNode}`
        }, [item.name])
      }
    },
    resize() {
      this.height = this.$el.clientHeight
    },
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resize);
  }
};
</script>
<style src="./index.scss" lang="scss">
</style>