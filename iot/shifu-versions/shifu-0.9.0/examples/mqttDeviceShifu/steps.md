## Choose one of the two options, don't do both
### Option 1
To start MQTT broker in local machine:

```bash
# install mosquitto server and client
sudo apt-add-repository ppa:mosquitto-dev/mosquitto-ppa
sudo apt-get install mosquitto mosquitto-clients
```

edit `/etc/mosquitto/conf.d/mosquitto.conf`:

```
listener 1883 0.0.0.0
allow_anonymous true
log_dest stdout
```

restart mosquitto service:

```bash
sudo service mosquitto restart
```


### Option 2
Deploy the MQTT broker in Kubernetes:

```bash
kubectl apply -f /shifu/examples/mqttDeviceShifu/mqtt_broker
# Forward Kubernetes svc to localhost:
kubectl port-forward svc/mosquitto-service -n devices 1883:1883 --address='0.0.0.0'
```

### Testing:
Deploy MQTT deviceshifu YAMLs:

```bash
kubectl apply -f examples/mqttDeviceShifu/mqtt_deploy/
```

Publish some ramdom data to the topic:

```bash
mosquitto_pub -h 172.28.15.229 -d -p 1883 -t /test/test -m "test2333"
```

Test using an nginx pod

```bash
kubectl run nginx --image=nginx -n deviceshifu
kubectl -n deviceshifu exec -it nginx -- bash
curl deviceshifu-mqtt/mqtt_data
```

output:

```bash
root@nginx:/# curl deviceshifu-mqtt/mqtt_data
{"mqtt_message":"test2333","mqtt_receive_timestamp":"2022-04-29 08:57:49.9492744 +0000 UTC m=+75.407609501"}
```

For debugging, subscribe to your machine's IP (change accordingly):

```bash
mosquitto_sub -h 172.28.15.229 -p 1883 -t /test/test -d
```

