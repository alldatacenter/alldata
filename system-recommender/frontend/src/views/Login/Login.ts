import { Component, Prop, Vue, Emit } from "vue-property-decorator";
@Component
export default class Login extends Vue {
  /*
        子组件如何给父组件传递数据：
        首先：
            导入 Emit
        然后：
            定义发送数据的事件 @Emit('bindSend') send(msg: string) { };    
        最后：
            定义发送数据的方法，这个方法要调用上面的 send 方法
        最最后：
            父组件接收值用 @bindSend="" 这个事件接收数据，里面放一个回调函数
            用该回调函数处理子组件返回的数据
    */
  public msg: string = 'updateUserData';
  // bindSend 为父组件引用子组件上 绑定的事件名称
  @Emit('bindSend') send(msg: string) { }; // send 处理给父组件传值的逻辑
  // 通过触发这个事件来处理发送的内容数据的逻辑，然后执行 @Emit() 定义的 send(msg: string){} 事件
  public propMsg() {
    this.send(this.msg)
  }

  public isRegisterStatus: boolean = false

  public form: any = {
    'username': 'admin',
    'password': 'admin'
  }

  public user: any = {
    'name': '',
    'id': '',
  }

  public async created() {
    // 检测浏览器是否缓存了用户登录信息，没有就登陆，否则就进入首页
    // dubug 的时候手动打开开发者工具的 Applicatuion 清空 localStorage
    let user = localStorage.getItem('user')
    if (user == null) {
      console.log('没有找到已登陆用户')
    } else {
      console.log('发现已登陆用户：' + user)
      // 直接前往首页
      this.$router.push({ name: 'home' })
    }
  }

  public async doLogin() {
    console.dir(this.form)
    // 发送登录请求
    let res = await this.axios.get('/business/user/login', {
      params: {
        username: this.form.username,
        password: this.form.password
      }
    })

    if (res.data.success) {
      await this.$alert('登录成功', '提示', {
        confirmButtonText: '确定'
      });

      // 将登陆信息缓存在浏览器里面
      localStorage.setItem('user', res.data.user.name)
      localStorage.setItem('userId', res.data.user.id)

      // 通知Head组件该更新数据了
      this.propMsg()

      // 前往首页
      this.$router.push({ name: 'home' })

    } else {
      this.$alert('登陆失败', '提示', {
        confirmButtonText: '确定'
      });
      this.form = {}
    }
  }

  public async doRegister() {
    console.dir(this.form)
    // 向后端发送注册请求
    let res = await this.axios.get('/business/user/register', {
      params: {
        username: this.form.username,
        password: this.form.password
      }
    })
    if (res.data.success) {
      await this.$alert('注册成功', '提示', {
        confirmButtonText: '确定'
      });
      // 成功后跳回到上一级登录页面
      this.goRegister()
    } else {
      this.$alert('注册失败', '提示', {
        confirmButtonText: '确定'
      });
    }
  }

  public goRegister(): void {
    this.form = {
      'username': '',
      'password': ''
    }
    this.isRegisterStatus = !this.isRegisterStatus
  }
}