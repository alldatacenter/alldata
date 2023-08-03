## To build:

`docker build . -t edgehub/camera-python:v0.0.1`

## To run locally(example IP and port):

`IP_CAMERA_ADDRESS=192.168.14.172 IP_CAMERA_USERNAME=admin IP_CAMERA_PASSWORD=password IP_CAMERA_CONTAINER_PORT=8000 python3 camera.py`

## To run the camera deviceShifu, update the following fields in `shifu/examples/rtspDeviceShifu/camera-deployment/deviceshifu-camera-deployment.yaml`

```
......
        - name: IP_CAMERA_ADDRESS
          value: "192.168.14.172"     # IP address of your RTSP camera
        - name: IP_CAMERA_USERNAME
          value: "admin"              # Username for RTSP camera
        - name: IP_CAMERA_PASSWORD
          value: "password"          # Password for RTSP camera
......
```