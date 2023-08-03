#!/bin/sh
set -e
set -o noglob

# get info of System
setup_verify_os_and_arch() {
    OS=$(uname -s | tr "[:upper:]" "[:lower:]" ) 
    ARCH=$(uname -m)
    case $ARCH in
        amd64)
            ARCH=amd64
            ;;
        x86_64)
            ARCH=amd64
            ;;
        arm64)
            ARCH=arm64
            ;;
        aarch64)
            ARCH=arm64
            ;;
        arm*)
            ARCH=arm64
            ;;
        *)
            fatal "Unsupported architecture $ARCH"
    esac
}

setup_verify_os_and_arch

curl -LO https://demo.shifu.dev/demo-content/shifu_demo_aio_${OS}_${ARCH}.tar

rm -rf shifudemos && mkdir shifudemos

tar -xvf shifu_demo_aio_${OS}_${ARCH}.tar -C shifudemos 

cd shifudemos

chmod +x test/scripts/deviceshifu-demo-aio.sh && sudo ./test/scripts/deviceshifu-demo-aio.sh run_demo
