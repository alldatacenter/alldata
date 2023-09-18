## kubeConfigContent

为了通过CS模式连接K8S服务端可以先通过kubectl config命令生成服务端连接的证书配置文件，config命令请查看
[kubectl-commands#config](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#config)

执行 `kubectl config view  --flatten=true` 将得到的内容粘贴到上面输入框中

> TIS中有较多组件是运行在K8S容器中的，需要在TIS运行环境中安装部署K8S环境。有多种方式安装K8S环境，[详细请查看](http://tis.pub/blog/k8s-using/)

