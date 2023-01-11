PROJECT_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
IMAGE_VERSION = $(shell cat version.txt)
# ENVTEST_K8S_VERSION refers to the version of kubebuilder assets to be downloaded by envtest binary.
ENVTEST_K8S_VERSION = 1.25.2

fmt: ## Run go fmt against code.
	go fmt ./...

vet: ## Run go vet against code.
	go vet ./...

.PHONY: test
test: fmt envtest ## Run tests.
	KUBEBUILDER_ASSETS="$(shell $(ENVTEST) use $(ENVTEST_K8S_VERSION) -p path)" go test -v -race -coverprofile=coverage.out -covermode=atomic $(shell go list ./... | grep -v -E '/cmd|/mockdevice')

buildx-push-image-deviceshifu-http-http:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuHTTP \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-http:${IMAGE_VERSION} --push

buildx-push-image-deviceshifu-http-mqtt:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuMQTT \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-mqtt:${IMAGE_VERSION} --push

buildx-push-image-deviceshifu-http-socket:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuSocket \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-socket:${IMAGE_VERSION} --push

buildx-push-image-deviceshifu-http-opcua:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuOPCUA \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-opcua:${IMAGE_VERSION} --push

buildx-push-image-deviceshifu-http-plc4x:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuPLC4X \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-plc4x:${IMAGE_VERSION} --push

.PHONY: buildx-push-image-deviceshifu
buildx-push-image-deviceshifu: \
	buildx-push-image-deviceshifu-http-http \
	buildx-push-image-deviceshifu-http-mqtt \
	buildx-push-image-deviceshifu-http-socket \
	buildx-push-image-deviceshifu-http-opcua

buildx-push-image-telemetry-service:
	docker buildx build --platform=linux/amd64,linux/arm64,linux/arm -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.telemetryservice \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/telemetryService:${IMAGE_VERSION} --push

buildx-build-image-deviceshifu-http-http:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuHTTP \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-http:${IMAGE_VERSION} --load

buildx-build-image-deviceshifu-http-mqtt:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuMQTT \
	 	--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-mqtt:${IMAGE_VERSION} --load

buildx-build-image-deviceshifu-http-socket:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuSocket \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-socket:${IMAGE_VERSION} --load

buildx-build-image-deviceshifu-http-opcua:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuOPCUA \
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-opcua:${IMAGE_VERSION} --load

buildx-build-image-deviceshifu-http-plc4x:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.deviceshifuPLC4X\
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/deviceshifu-http-plc4x:${IMAGE_VERSION} --load

buildx-build-image-deviceshifu: \
	buildx-build-image-deviceshifu-http-http \
	buildx-build-image-deviceshifu-http-mqtt \
	buildx-build-image-deviceshifu-http-socket \
	buildx-build-image-deviceshifu-http-opcua \
	buildx-build-image-deviceshifu-http-plc4x 

buildx-build-image-telemetry-service:
	docker buildx build --platform=linux/$(shell go env GOARCH) -f ${PROJECT_ROOT}/dockerfiles/Dockerfile.telemetryservice\
		--build-arg PROJECT_ROOT="${PROJECT_ROOT}" ${PROJECT_ROOT} \
		-t edgehub/telemetryservice:${IMAGE_VERSION} --load

.PHONY: download-demo-files
download-demo-files:
	docker pull edgehub/mockdevice-agv:${IMAGE_VERSION}
	docker pull edgehub/mockdevice-plate-reader:${IMAGE_VERSION}
	docker pull edgehub/mockdevice-robot-arm:${IMAGE_VERSION}
	docker pull edgehub/mockdevice-thermometer:${IMAGE_VERSION}
	docker pull edgehub/deviceshifu-http-http:${IMAGE_VERSION}
	docker pull edgehub/shifu-controller:${IMAGE_VERSION}
	docker pull quay.io/brancz/kube-rbac-proxy:v0.13.1
	docker pull nginx:1.21

docker-push-image-deviceshifu:
	docker push edgehub/deviceshifu-http-http:${IMAGE_VERSION}

docker-push-deviceshifu-demo-image:
	docker push edgehub/demo-image-alpine:${IMAGE_VERSION}

docker-push-image-mockdevices:
	docker push edgehub/mockdevice-thermometer:${IMAGE_VERSION}
	docker push edgehub/mockdevice-robot-arm:${IMAGE_VERSION}
	docker push edgehub/mockdevice-plate-reader:${IMAGE_VERSION}
	docker push edgehub/mockdevice-agv:${IMAGE_VERSION}

.PHONY: clean-images
clean-images:
	docker rmi $(shell sudo docker images | grep 'edgehub')

tag:
	go run tools/tag.go ${PROJECT_ROOT} ${IMAGE_VERSION} $(VERSION)
	cd pkg/k8s/crd/ && (make generate-controller-yaml IMG=edgehub/shifu-controller:$(VERSION) generate-install-yaml)
	sed -e "s/${IMAGE_VERSION}/${VERSION}/g" ./test/scripts/deviceshifu-demo-aio.sh > ./test/scripts/tmp.sh && mv ./test/scripts/tmp.sh ./test/scripts/deviceshifu-demo-aio.sh
	echo $(VERSION) > version.txt

## Location to install dependencies to
LOCALBIN ?= $(shell pwd)/bin
$(LOCALBIN):
	mkdir -p $(LOCALBIN)

## Tool Binaries
KUSTOMIZE ?= $(LOCALBIN)/kustomize
CONTROLLER_GEN ?= $(LOCALBIN)/controller-gen
ENVTEST ?= $(LOCALBIN)/setup-envtest

## Tool Versions
KUSTOMIZE_VERSION ?= v4.5.5
CONTROLLER_TOOLS_VERSION ?= v0.9.2

KUSTOMIZE_INSTALL_SCRIPT ?= "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"
.PHONY: kustomize
kustomize: $(KUSTOMIZE) ## Download kustomize locally if necessary.
$(KUSTOMIZE): $(LOCALBIN)
	test -s $(LOCALBIN)/kustomize || { curl -s $(KUSTOMIZE_INSTALL_SCRIPT) | bash -s -- $(subst v,,$(KUSTOMIZE_VERSION)) $(LOCALBIN); }

.PHONY: controller-gen
controller-gen: $(CONTROLLER_GEN) ## Download controller-gen locally if necessary.
$(CONTROLLER_GEN): $(LOCALBIN)
	test -s $(LOCALBIN)/controller-gen || GOBIN=$(LOCALBIN) go install sigs.k8s.io/controller-tools/cmd/controller-gen@$(CONTROLLER_TOOLS_VERSION)

.PHONY: envtest
envtest: $(ENVTEST) ## Download envtest-setup locally if necessary.
$(ENVTEST): $(LOCALBIN)
	test -s $(LOCALBIN)/setup-envtest || GOBIN=$(LOCALBIN) go install sigs.k8s.io/controller-runtime/tools/setup-envtest@latest
