<template>
    <div class="not-enough">
        <ul class="nav-bar setup-content">
            <li v-for="(item, index) in conData.options.navList" :class="currentIndex===index?'curr':''" @click="changeCurr(index)" :key="index">
                <p>{{item.title}}</p>
                <p>{{item.desc}}</p>
            </li>
            <div class="setup-box" style="width:100px;left:1100px;">
                <div>
                    <Button size="small" @click.stop="handleSelectModel">编辑</Button>
                </div>
            </div>
        </ul>
        <div class="content" v-if="showContent">
            <div v-for="(item, index) in conData.options.list[currentIndex]" :key="index" class="setup-content">
                <img :src="item.img" width="210" height="210" :alt="item.name">
                <p>{{item.name}}</p>
                <p>
                    <span>{{item.price | unitPrice('￥')}}</span> 
                    <!-- <span>{{item.price | unitPrice('￥')}}</span> -->
                </p>
                <div class="setup-box">
                    <div>
                        <Button size="small" @click.stop="handleSelectGoods(item)">编辑</Button>
                    </div>
                </div>
            </div>
        </div>
        <Modal
            v-model="showModal"
            title="装修"
            draggable
            width="800"
            :z-index="100"
            :mask-closable="false"
            >
            <div class="modal-tab-bar">
                <Button type="primary" size='small' @click="handleAddNav">添加分类</Button>
                <table cellspacing="0">
                    <thead>
                        <tr>
                            <th width="250">主标题</th>
                            <th width="250">描述</th>
                            <th width="250">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(item, index) in conData.options.navList" :key="index">
                            <td><Input v-model="item.title" /></td>
                            <td><Input v-model="item.desc" /></td>
                            <td v-if="index!=0">
                                <Button type="error" size="small" @click="handleDelNav(index)">删除</Button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </Modal>
        <!-- 选择商品。链接 -->
        <liliDialog
            ref="liliDialog"
            @selectedGoodsData="selectedGoodsData"
        ></liliDialog>
    </div>
</template>
<script>
export default {
    props:{
        data:{
            type: Object,
            default: null
        }
    },
    data() {
        return {
            currentIndex:0, // 当前商品index
            conData:this.data, // 当前数据
            selected:{}, // 已选数据
            showModal:false, // modal显隐
            showContent:true, // 选择后刷新数据用
        }
    },
    watch:{
        data:function(val){
            this.conData = val
        },
        conData:function(val){
            this.$emit('content',val)
        }
    },
    methods:{
        // tab点击切换
        changeCurr(index){
            this.currentIndex = index;
        },
        // 编辑
        handleSelectModel (item,type) {
            this.selected = item;
            this.showModal = true
        },
        handleSelectGoods(item) { // 调起选择商品弹窗
            if(item) this.selected = item;
            this.$refs.liliDialog.open('goods', 'single')
            setTimeout(() => {
                this.$refs.liliDialog.goodsData = [this.selected]
            }, 500);
        },
        // 选择商品回调
        selectedGoodsData(val){
            console.log(val)
            let goods = val[0]
            this.selected.img = goods.thumbnail
            this.selected.price = goods.price
            this.selected.name = goods.goodsName
            this.selected.url = `/goodsDetail?skuId=${goods.id}&goodsId=${goods.goodsId}`
        },
        handleDelNav(index){ // 删除导航
            this.conData.options.navList.splice(index,1)
            this.conData.options.list.splice(index,1)
        },
        handleAddNav(){ // 添加导航
            this.conData.options.navList.push(
                {title:'',desc:''}
            )
            this.conData.options.list.push(
                [{ img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                { img:'', name:'', price:0, url:'' },
                ],
            )
            this.showContent = false
            this.$nextTick(()=>{
                this.showContent = true;
            })
        },
    }
}
</script>
<style lang="scss" scoped>
@import './setup-box.scss';
.nav-bar{
    display: flex;
    justify-content: center;
    width: 100%;
    margin-bottom: 10px;
    background-color: rgb(218, 217, 217);
    height: 60px;
    align-items: center;
    position: relative;
    li{
        padding: 0 30px;
        text-align: center;
        p:nth-child(1){
            font-size: 16px;
            border-radius: 50px;
            padding: 0 7px;
        }

        p:nth-child(2){
            font-size: 14px;
            color: #999;
        }

        &:hover{
            p{
                color: $theme_color;
            }
            cursor: pointer;
        }
        border-right: 1px solid #eee;
        
    }
    li:last-of-type{
        border: none;
    }
   
    .curr{
        p:nth-child(1){
            background-color: $theme_color;
            
            color: #fff;
        }
        p:nth-child(2){
            color: $theme_color;
        }
    }
}

.content{
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    >div{
        padding: 10px;
        box-sizing: border-box;
        border: 1px solid #eee;
        margin-bottom: 10px;
        p:nth-of-type(1){
            overflow: hidden;
            width: 210px;
            white-space: nowrap;
            text-overflow:ellipsis;
            margin: 10px 0 5px 0;
        }
        p:nth-of-type(2){
            color: $theme_color;
            font-size: 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            span:nth-child(2){
                text-decoration: line-through;
                font-size: 12px;
                color: #999;
            }
        }
    }
}
</style>