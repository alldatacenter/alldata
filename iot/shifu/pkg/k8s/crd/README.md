## Commands (Work In Progress)

### Initialize CRD project
```
make kube-builder-init
```

### Create a new kind cluster
```
kind delete cluster && kind create cluster
```

### Install CRD
```
make install
```

### Uninstall CRD
```
make uninstall
```

### Create EdgeDevice API
```
make kube-builder-create-api-edgedevice
```

### Build EdgeDevice controller and load into Docker
```
make docker-buildx-load IMG=edgehub/shifu-controller:v0.0.1
```

### Deploy EdgeDeivce controller
```
make deploy IMG=edgehub/shifu-controller:v0.0.1
```

### Deploy sample edgedevice
```
kubectl apply -f config/samples/shifu_v1alpha1_edgedevice.yaml
```

### Create install CRD file
```
make generate-controller-yaml IMG=edgehub/shifu-controller:v0.0.1 generate-install-yaml
```
