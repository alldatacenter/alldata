### 1. Start *Shifu* and connect a simple virtual PLC
There is already a demo virtual PLC deployment configuration in `shifu/deviceshifu/examples/demo_device` . The virtual PLC will virtualize the binary code of the four storage areas "M", "Q", "T" and "C" of the PLC device. This binary encoding can be read with the `getcontent` API, or modified with the `sendsinglebit` API.


From the `shifu` root directory, run the following command to install *shifu* :

```
kubectl apply -f k8s/crd/install/shifu_install.yml
```
In the `shifu` root directory, run the following command to package the virtual PLC into a Docker image:

```
docker build -t edgehub/mockdevice-plc:v0.0.1 . \
       -f deviceshifu/examples/mockdevice/plc/Dockerfile.mockdevice-plc
```
In the `shifu` root directory, run the following commands to load the virtual PLC image into Kind and deploy it to the Kubernetes cluster:

```
kind load docker-image edgehub/mockdevice-plc:v0.0.1
kubectl apply -f deviceshifu/examples/demo_device/edgedevice-plc/
```
### 2. Interact with *deviceShifu*
We can interact with *deviceShifu* through the nginx application, the command is:

```
kubectl run nginx --image=nginx
kubectl exec -it nginx -- bash
```
Interact with the virtual PLC through the following commands on the nginx command line:

```
curl "deviceshifu-plc/sendsinglebit?rootaddress=DB&address=0&start=0&digit=0&value=1";echo 
curl deviceshifu-plc/getcontent?rootaddress=Q;echo 
```
example output:

```
curl edgedevice-plc/getcontent?rootaddress=Q;echo 
0b0000000000000000
curl "edgedevice-plc/sendsinglebit?rootaddress=DB&address=0&start=0&digit=0&value=1";echo 
0b0000000000000001 
```
