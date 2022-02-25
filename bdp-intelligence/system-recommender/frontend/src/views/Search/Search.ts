import { Component, Prop, Vue, Watch } from "vue-property-decorator";

@Component
export default class Search extends Vue {

    public searchResult: any = []

    public colors: any = ['#99A9BF', '#F7BA2A', '#FF9900']

    @Watch('$route')
    public routerChanged() {
        console.log("进入watch")
        console.log(this.$route.query.searchInput)
        this.getData('/business/product/search', String(this.$route.query.searchInput))
    }

    public created() {
        console.log("进入create")

        this.getData('/business/product/search', String(this.$route.query.searchInput))
    }

    public async getData(url: string, searchInput: string) {

        // 动画加载过程中，若 axios 出现异常会导致动画无法关闭
        const loading = this.$loading({
            lock: true,
            text: 'Loading',
            spinner: 'el-icon-loading',
            background: 'rgba(0, 0, 0, 0.7)'
        });
        try {

            let res = await this.axios.get(url, {
                params: {
                    sql: searchInput
                }
            })
            console.dir(res)
            this.searchResult = res.data.products
        } catch (err) {
            console.error('请求：' + url + ' 异常 ' + err)
        }
        this.$nextTick(() => { // 以服务的方式调用的 Loading 需要异步关闭
            loading.close();
        });
    }

    public async doRate(rate: number, productId: number) {
        console.log('收到评分数据,productId: ' + productId + " rate: " + rate)
        let user = localStorage.getItem('user')
        let res = await this.axios.get('/business/product/rate/' + productId, {
            params: {
                score: rate,
                username: user
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