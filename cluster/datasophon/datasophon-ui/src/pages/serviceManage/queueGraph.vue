<template>
  <div>
    <div ref="myPage" style="width: calc(100% - 10px);height:calc(100vh - 190px);">
      <SeeksRelationGraph ref="seeksRelationGraph" :options="graphOptions" :on-node-click="onNodeClick" :on-node-expand="onNodeExpand">
        <div slot="node" slot-scope="{node}">
          <div style="height:40px;line-height: 40px;border-radius: 32px;cursor: pointer;" @contextmenu.prevent.stop="showNodeMenus(node, $event)">{{node.text}}</div>
        </div>
      </SeeksRelationGraph>
    </div>
            
    <div v-show="isShowNodeMenuPanel" :style="{left: nodeMenuPanelPosition.x + 'px', top: nodeMenuPanelPosition.y + 'px' }" style="z-index: 999;padding:10px;background-color: #ffffff;border:#eeeeee solid 1px;box-shadow: 0px 0px 8px #cccccc;position: absolute;">
      <!-- <div style="line-height: 25px;padding-left: 10px;color: #888888;font-size: 12px;">对这个节点进行操作：</div> -->
      <!-- <a-button >操作1</a-button> -->
      <div class="c-node-menu-item graph-btn" @click.stop="doAction('save')">新建</div>
      <div v-if="!showRoot" class="c-node-menu-item graph-btn" @click.stop="doAction('update')">编辑</div>
      <div v-if="!showRoot" class="c-node-menu-item graph-btn" @click.stop="doAction('show')">查看</div>
      <div v-if="!showRoot" class="c-node-menu-item graph-btn" @click.stop="doAction('delete')">删除</div>
      <!-- <div class="c-node-menu-item" @click.stop="doAction('操作1')">操作4</div> -->
    </div>
  </div>
</template>

<script>
import SeeksRelationGraph from "relation-graph";
import convertQueue from './convertQueue.vue'
export default {
  name: "queuegraph",
  components: { SeeksRelationGraph },
  data() {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 8 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 16 },
      },
      showRoot: false,
      graphData:{},
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      form: this.$form.createForm(this),
      isShowNodeMenuPanel: false,
      showForm:{},
      nodeMenuPanelPosition: { x: 0, y: 0 },
      g_loading: true,
      graphOptions: {
        allowSwitchLineShape: true,
        allowSwitchJunctionPoint: true,
        // // debug: true,
        layouts: [
          {
            label: "树",
            layoutName: "tree",
            layoutClassName: "seeks-layout-center",
            defaultNodeShape: 0,
            defaultLineShape: 1,
            from: "left",
            // 通过这4个属性来调整 tree-层级距离&节点距离
            min_per_width: undefined,
            max_per_width: "300",
            min_per_height: "40",
            max_per_height: undefined,
            levelDistance: "", // 如果此选项有值，则优先级高于上面那4个选项
          },
        ],
        defaultLineMarker: {
          markerWidth: 12,
          markerHeight: 12,
          refX: 6,
          refY: 6,
          data: "M2,2 L10,6 L2,10 L6,6 L2,2",
        },
        disableDragNode:false,
        // defaultExpandHolderPosition: "right",
        defaultNodeShape: 1,
        defaultNodeWidth: "100",
        defaultLineShape: 4,
        defaultJunctionPoint: "lr",
        defaultNodeBorderWidth: 0,
        defaultLineColor: "rgba(0, 186, 189, 1)",
        defaultNodeColor: "rgba(0, 206, 209, 1)",
      },
    };
  },
  created() {
    this.getGraphData();
  },
  mounted() {
  },
  watch:{
  },
  methods: {
    refGraphData(){
      // this.$axiosJsonPost('/ddh/cluster/queue/capacity/refreshToYarn', {clusterId: this.clusterId}).then((res) => {
      //   this.getGraphData();
      // });
      this.getGraphData();
    },
    getGraphData(){
      this.$axiosPost(global.API.getCapacityList, {clusterId: this.clusterId}).then((res) => {
        this.graphData = res.data ||{};
        let sum = 0;
        if(this.graphData.nodes){
          this.graphData.nodes.map(item=>{
            sum += Number(item.capacity || 0)
          })
        }
        
        let style = sum < 100?{color: 'red'}:{};
        this.setGraphData(style);
      });
    },
    setGraphData(nodeStyle={}) {
      // 使用要点：通过节点属性expandHolderPosition: 'right' 和 expanded: false 可以让节点在没有子节点的情况下展示一个"展开"按钮
      //         通过onNodeExpand事件监听节点，在被展开的时候有选择的去从后台获取数据，如果已经从后台加载过数据，则让当前图谱根据当前的节点重新布局
      this.g_loading = false;
      if(this.graphData.rootId){
        let nodes = (this.graphData.nodes|| []).map(item=>{
          let text = item.queueName+'('+item.capacity+'%)';
          // let text = item.queueName+item.capacity+'xxxxxx%';
          return {
            data:item,
            id:item.queueName,
            text:text,
            ...nodeStyle
          }
        }).concat({id: this.graphData.rootId, text: this.graphData.rootId});
        this.$refs.seeksRelationGraph.setJsonData(
          {
            rootId:this.graphData.rootId,
            nodes:nodes,
            links:this.graphData.links|| [],
          },
          (seeksRGGraph) => {
            // 这些写上当图谱初始化完成后需要执行的代码
          }
        );
        this.$refs.seeksRelationGraph.refresh();
      }
    },
    onNodeExpand(node, e) {
      // 当有一些节点被显示或隐藏起来时，会让图谱看着很难看，需要布局器重新为节点分配位置，所以这里需要调用refresh方法来重新布局
      this.$refs.seeksRelationGraph.refresh();
    },
    showNodeMenus(nodeObject, $event) {
      if (nodeObject.id === 'root') {
        this.showRoot = true
      } else {
        this.showRoot = false
      }
      this.currentNode = nodeObject;
      var _base_position = this.$refs.myPage.getBoundingClientRect();
      console.log("showNodeMenus:", $event, _base_position);
      this.isShowNodeMenuPanel = true;
      this.nodeMenuPanelPosition.x = $event.clientX - _base_position.x;
      this.nodeMenuPanelPosition.y = $event.clientY - _base_position.y;
    },
    onNodeClick(nodeObject, $event) {
      console.log('onNodeClick:', nodeObject)
      // this.showForm[nodeObject.id] = this.showForm[nodeObject.id]?false:true;
      // for(let key in this.showForm){
      //   if(key != nodeObject.id){
      //     this.showForm[key] = false;
      //   }
      // }
      this.isShowNodeMenuPanel = false;
    },
    delNode(node){
      this.$axiosJsonPost('/ddh/cluster/queue/capacity/delete', {id:node.id}).then((res) => { 
        if (res.code !== 200) return
        this.$message.success('删除成功')
        this.$destroyAll();
        this.refGraphData();
      }).catch((err) => {});
    },
    doAction(key) {
      // for(let key in this.showForm){
      //   this.showForm[key] = false;
      // }
      let node = key == 'save'?{parent: this.currentNode.id } :this.currentNode;
      this.isShowNodeMenuPanel = false;
      let width = key == 'delete'?400:800;
      let title = key == 'save'?"新建":key == 'delete'?'删除':key == 'show'?'查看':'编辑';
      let content = key != 'delete'?(
        <convertQueue obj={node} type={key} callBack={()=>{this.refGraphData()}}/>
      ):(<div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              确认删除吗？
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.delNode(node)}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>);
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.c-label {
  color: #666666;
  font-size: 14px;
  padding-left: 10px;
  padding-right: 10px;
}
.graph-btn{
  cursor: pointer;
  padding:0 10px;
}
.graph-btn:hover{
  background: #2872e03b;
  color: #1D202D;
}
/* .graph-form{
  position: absolute;
  left: 120px;
  top: 0;
  background: #fff;
  width: 300px;
  z-index: 9;
} */
</style>