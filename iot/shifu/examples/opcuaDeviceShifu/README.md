## To try out OPC UA ***deviceShifu*** locally:
1. Install Shifu in your Kubernetes cluster(kind/k3d will work too)
2. Install python requirements:
    - `pip3 install server/requirements.txt`
3. Start the sample OPC UA server:
    ```bash
    localhost server % python3 server.py 
    Endpoints other than open requested but private key and certificate are not set.
    Listening on 0.0.0.0:4840
    ```
4. Create a `configmap` to import certificate for OPC UA (Optional, if you do not use certificate authentication, please skip this step)
   ```bash
   kubectl create configmap edgedevice-opcua-certificate --from-file=your_certificate_file.pem/your_certificate_file.der --from-file=your_private_key.pem -n deviceshifu
   ```
   If you don't have a certificate, you can use `generate_cert.go` to generate a local certificate for testing
   ```bash
   go run generate_cert.go
   ```
5. Deploy OPC UA ***deviceShifu***, in `shifu/examples/opcuaDeviceShifu`:
    ```bash
    # kubectl apply -f opcua_deploy/
    configmap/opcua-configmap-0.0.1 created
    deployment.apps/edgedevice-opcua-deployment created
    service/edgedevice-opcua created
    edgedevice.shifu.edgenesis.io/edgedevice-opcua created
    ```
6. Start an `Nginx` pod and enter its shell:
    ```bash
    # kubectl run nginx --image=nginx
    ```
    ```bash
    # kubectl exec -it nginx -- bash
    root@nginx:/#
    ```
7. Interact with the `OPC UA` ***deviceShifu***:
    ```bash
    root@nginx:/# curl edgedevice-opcua/get_time;echo
    2022-05-25 07:29:36.879869 +0000 UTC
    root@nginx:/# curl edgedevice-opcua/get_server;echo
    FreeOpcUa Python Server
    root@nginx:/# curl edgedevice-opcua/get_value;echo
    3175.5999999982073
    ```
