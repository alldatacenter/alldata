### 1. 运行 *Shifu* 并连接一个虚拟PLC
在 `shifu/deviceshifu/examples/demo_device` 路径中已经有一个演示虚拟 PLC 的 deployment 配置。该虚拟 PLC 会虚拟出 PLC 设备的 "M"、"Q"、"T"、"C" 四个存储区域的二进制编码，可以通过 `getcontent` API 来读取这个二进制编码，也可以通过 `sendsinglebit` API 来修改这个编码。

在 `shifu` 根目录下，运行下面命令来运行 *shifu* :

```
kubectl apply -f k8s/crd/install/shifu_install.yml
```
在 `shifu` 根目录下，运行下面命令来把虚拟 PLC 打包成 Docker 镜像:

```
docker build -t edgehub/mockdevice-plc:v0.0.1 . \
       -f deviceshifu/examples/mockdevice/plc/Dockerfile.mockdevice-plc
```
在 `shifu` 根目录下，运行下面命令来把虚拟 PLC 镜像加载到 Kind 中，并部署到 Kubernetes 集群中:

```
kind load docker-image edgehub/mockdevice-plc:v0.0.1
kubectl apply -f deviceshifu/examples/demo_device/edgedevice-plc/
```
### 2. 与 *deviceShifu* 交互
我们可以通过 nginx 应用来和 *deviceShifu* 交互，命令为：

```
kubectl run nginx --image=nginx
kubectl exec -it nginx -- bash
```
在 nginx 命令行中通过如下命令与虚拟 PLC 进行交互：

```
curl "deviceshifu-plc/sendsinglebit?rootaddress=DB&address=0&start=0&digit=0&value=1";echo 
curl deviceshifu-plc/getcontent?rootaddress=Q;echo 
```
输出示例：

```
curl deviceshifu-plc/getcontent?rootaddress=Q;echo 
0b0000000000000000
curl "deviceshifu-plc/sendsinglebit?rootaddress=DB&address=0&start=0&digit=0&value=1";echo 
0b0000000000000001 
```
