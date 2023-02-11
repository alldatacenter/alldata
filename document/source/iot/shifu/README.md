# Shifu

<div align="center">

Shifu is a Kubernetes-native IoT development framework that

greatly improves the efficiency, quality and reusability of IoT application development.


|Feature|Description |
|---|---|
|🔌 Fast Device integration &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|Compatible with almost all protocols and drivers.|
|👨‍💻 Efficient Application development|Shifu structually virtualizes each device and expose its capabilities in the form of APIs.|
|👨‍🔧 Easy Operation & Maintenance|Kubernetes-native framework, sparing the need for maintaining an additional O&M infrastructure.|
</div>
<br/><br/>

# 🔧 Install

- If you have a running Kubernetes cluster: Please use the command `kubectl apply` to install Shifu in your cluster:

    ```sh
    cd shifu
    kubectl apply -f pkg/k8s/crd/install/shifu_install.yml
    ```

- If you don't have a running Kubernetes cluster: Please follow the following steps to try our demo.
  - Download and Install Docker

    [Mac](https://docs.docker.com/desktop/install/mac-install/) | [Windows(WSL)](https://docs.docker.com/desktop/install/windows-install/) | [Linux](https://docs.docker.com/desktop/install/linux-install/)
  - Download and Install Shifu Demo with a single command
    ```sh
    curl -sfL https://raw.githubusercontent.com/Edgenesis/shifu/main/test/scripts/shifu-demo-install.sh | sudo sh -
    ```

- Now that you have installed Shifu, please visit our🗒️[documentation](https://shifu.run/docs/) to🔌[connect a device](https://shifu.run/docs/guides/cases/) and 👨‍💻[develop your own application](https://shifu.run/docs/guides/application/)!

# 🌟 Stargazers over time

[![Stargazers over time](https://starchart.cc/Edgenesis/shifu.svg)](https://starchart.cc/Edgenesis/shifu)

# License
This project is distributed under Apache 2.0 License.

## 项目官网地址

https://github.com/Edgenesis/shifu