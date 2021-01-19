import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import VueRouter, { Route } from 'vue-router';

@Component({
    name: 'ComponentName',
    filters: {
        numFilter(value: number) {
            // 截取当前数据到小数点后两位
            let realVal = parseFloat(String(value)).toFixed(2)
            return realVal
        }
    }
})
export default class Detail extends Vue {

    public info: any = {}

    public itemcf: any = []

    public contentbased: any = []

    public colors: any = ['#99A9BF', '#F7BA2A', '#FF9900']

    @Watch('$route')
    private routerChanged(val: Route, oldVal: Route) {
        this.info = {}
        this.itemcf = []
        this.contentbased = []
        this.getDataList(Number(val.query.productId))
    }

    public created() {
        this.info = {}
        this.itemcf = []
        this.contentbased = []
        this.getDataList(Number(this.$route.query.productId))
    }

    public async getDataList(productId: number) {
        // 动画加载过程中，若 axios 出现异常会导致动画无法关闭
        const loading = this.$loading({
            lock: true,
            text: 'Loading',
            spinner: 'el-icon-loading',
            background: 'rgba(0, 0, 0, 0.7)'
        });
        try {
            let res1 = await this.axios.get('/business/product/query/' + productId)
            this.info = res1.data.products
            console.log(this.info)

            let res2 = await this.axios.get('/business/product/itemcf/' + productId)
            this.itemcf = res2.data.products

            // let res3 = await this.axios.get('/business/product/contentbased/' + productId)
            // this.contentbased = res3.data.products
        } catch (err) {
            console.error('请求出现异常：' + err)
        }

        this.$nextTick(() => { // 以服务的方式调用的 Loading 需要异步关闭
            loading.close();
        });
    }

    public async doRate(rate: number, productId: number) {
        console.log('收到评分数据,productId: ' + productId + " rate: " + rate)
        let userId = localStorage.getItem('userId')
        let res = await this.axios.get('/business/product/rate/' + productId, {
            params: {
                score: rate,
                userId: userId
            }
        })
        if (res.data.success == true) {
            this.$alert('评分成功', '提示', {
                confirmButtonText: '确定'
            });
        } else {
            this.$alert('评分失败', '提示', {
                confirmButtonText: '确定'
            });
        }
    }
}