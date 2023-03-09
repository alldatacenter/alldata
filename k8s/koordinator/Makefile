# Git
GIT_VERSION ?= $(shell git describe --tags --always)
GIT_BRANCH ?= $(shell git rev-parse --abbrev-ref HEAD)
GIT_COMMIT_ID ?= $(shell git rev-parse --short HEAD)

# Image URL to use all building/pushing image targets
REG ?= ghcr.io
REG_NS ?= koordinator-sh
REG_USER ?= ""
REG_PWD ?= ""

KOORDLET_IMG ?= "${REG}/${REG_NS}/koordlet:${GIT_BRANCH}-${GIT_COMMIT_ID}"
KOORD_MANAGER_IMG ?= "${REG}/${REG_NS}/koord-manager:${GIT_BRANCH}-${GIT_COMMIT_ID}"
KOORD_SCHEDULER_IMG ?= "${REG}/${REG_NS}/koord-scheduler:${GIT_BRANCH}-${GIT_COMMIT_ID}"
KOORD_DESCHEDULER_IMG ?= "${REG}/${REG_NS}/koord-descheduler:${GIT_BRANCH}-${GIT_COMMIT_ID}"

# ENVTEST_K8S_VERSION refers to the version of kubebuilder assets to be downloaded by envtest binary.
ENVTEST_K8S_VERSION = 1.22

# Set license header files.
LICENSE_HEADER_GO ?= hack/boilerplate/boilerplate.go.txt

PACKAGES ?= $(shell go list ./... | grep -vE 'vendor|test/e2e')

# Get the currently used golang install path (in GOPATH/bin, unless GOBIN is set)
ifeq (,$(shell go env GOBIN))
GOBIN=$(shell go env GOPATH)/bin
else
GOBIN=$(shell go env GOBIN)
endif

# Setting SHELL to bash allows bash commands to be executed by recipes.
# This is a requirement for 'setup-envtest.sh' in the test target.
# Options are set to exit when a recipe line exits non-zero or a piped command fails.
SHELL = /usr/bin/env bash -o pipefail
.SHELLFLAGS = -ec

.PHONY: all
all: build

##@ General

# The help target prints out all targets with their descriptions organized
# beneath their categories. The categories are represented by '##@' and the
# target descriptions by '##'. The awk commands is responsible for reading the
# entire set of makefiles included in this invocation, looking for lines of the
# file as xyz: ## something, and then pretty-format the target and help. Then,
# if there's a line with ##@ something, that gets pretty-printed as a category.
# More info on the usage of ANSI control characters for terminal formatting:
# https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
# More info on the awk command:
# http://linuxcommand.org/lc3_adv_awk.php

.PHONY: help
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

.PHONY: manifests
manifests: controller-gen ## Generate WebhookConfiguration, ClusterRole and CustomResourceDefinition objects.
	$(CONTROLLER_GEN) rbac:roleName=manager-role crd webhook paths="./..." output:crd:artifacts:config=config/crd/bases
	@hack/fix_crd_plural.sh

.PHONY: generate
generate: controller-gen ## Generate code containing DeepCopy, DeepCopyInto, and DeepCopyObject method implementations.
	$(CONTROLLER_GEN) object:headerFile="$(LICENSE_HEADER_GO)" paths="./apis/..."
	$(CONTROLLER_GEN) object:headerFile="$(LICENSE_HEADER_GO)" paths="./pkg/slo-controller/config/..."
	@hack/update-codegen.sh

.PHONY: fmt
fmt: ## Run go fmt against code.
	go fmt ./...

.PHONY: vet
vet: ## Run go vet against code.
	go vet ./...

.PHONY: lint
lint: lint-go lint-license ## Lint all code.

.PHONY: lint-go
lint-go: golangci-lint ## Lint Go code.
	$(GOLANGCI_LINT) run -v --timeout=10m

.PHONY: lint-license
lint-license:
	@hack/update-license-header.sh

.PHONY: test
test: manifests generate fmt vet envtest ## Run tests.
	@KUBEBUILDER_ASSETS="$(shell $(ENVTEST) use $(ENVTEST_K8S_VERSION) -p path)" go test $(PACKAGES) -race -covermode atomic -coverprofile cover.out

.PHONY: fast-test
fast-test: envtest ## Run tests fast.
	@KUBEBUILDER_ASSETS="$(shell $(ENVTEST) use $(ENVTEST_K8S_VERSION) -p path)" go test $(PACKAGES) -race -covermode atomic -coverprofile cover.out

##@ Build

.PHONY: build
build: build-koordlet build-koord-manager build-koord-scheduler build-koord-descheduler build-koord-runtime-proxy

.PHONY: build-koordlet
build-koordlet: ## Build koordlet binary.
	go build -o bin/koordlet cmd/koordlet/main.go

.PHONY: build-koord-manager
build-koord-manager: ## Build koord-manager binary.
	go build -o bin/koord-manager cmd/koord-manager/main.go

.PHONY: build-koord-scheduler
build-koord-scheduler: ## Build koord-scheduler binary.
	go build -o bin/koord-scheduler cmd/koord-scheduler/main.go

.PHONY: build-koord-descheduler
build-koord-descheduler: ## Build koord-descheduler binary.
	go build -o bin/koord-descheduler cmd/koord-descheduler/main.go

.PHONY: build-koord-runtime-proxy
build-koord-runtime-proxy: ## Build koord-runtime-proxy binary.
	go build -o bin/koord-runtime-proxy cmd/koord-runtime-proxy/main.go

.PHONY: docker-build
docker-build: test docker-build-koordlet docker-build-koord-manager docker-build-koord-scheduler docker-build-koord-descheduler

.PHONY: docker-build-koordlet
docker-build-koordlet: ## Build docker image with the koordlet.
	docker build --pull -t ${KOORDLET_IMG} -f docker/koordlet.dockerfile .

.PHONY: docker-build-koord-manager
docker-build-koord-manager: ## Build docker image with the koord-manager.
	docker build --pull -t ${KOORD_MANAGER_IMG} -f docker/koord-manager.dockerfile .

.PHONY: docker-build-koord-scheduler
docker-build-koord-scheduler: ## Build docker image with the scheduler.
	docker build --pull -t ${KOORD_SCHEDULER_IMG} -f docker/koord-scheduler.dockerfile .

.PHONY: docker-build-koord-descheduler
docker-build-koord-descheduler: ## Build docker image with the descheduler.
	docker build --pull -t ${KOORD_DESCHEDULER_IMG} -f docker/koord-descheduler.dockerfile .

.PHONY: docker-push
docker-push: docker-push-koordlet docker-push-koord-manager docker-push-koord-scheduler docker-push-koord-descheduler

.PHONY: docker-push-koordlet
docker-push-koordlet: ## Push docker image with the koordlet.
ifneq ($(REG_USER), "")
	docker login -u $(REG_USER) -p $(REG_PWD) ${REG}
endif
	docker push ${KOORDLET_IMG}

.PHONY: docker-push-koord-manager
docker-push-koord-manager: ## Push docker image with the koord-manager.
ifneq ($(REG_USER), "")
	docker login -u $(REG_USER) -p $(REG_PWD) ${REG}
endif
	docker push ${KOORD_MANAGER_IMG}

.PHONY: docker-push-koord-scheduler
docker-push-koord-scheduler: ## Push docker image with the scheduler.
ifneq ($(REG_USER), "")
	docker login -u $(REG_USER) -p $(REG_PWD) ${REG}
endif
	docker push ${KOORD_SCHEDULER_IMG}

.PHONY: docker-push-koord-descheduler
docker-push-koord-descheduler: ## Push docker image with the descheduler.
ifneq ($(REG_USER), "")
	docker login -u $(REG_USER) -p $(REG_PWD) ${REG}
endif
	docker push ${KOORD_DESCHEDULER_IMG}

##@ Deployment

ifndef ignore-not-found
  ignore-not-found = false
endif

.PHONY: install
install: manifests kustomize ## Install CRDs into the K8s cluster specified in ~/.kube/config.
	$(KUSTOMIZE) build config/crd | kubectl apply -f -

.PHONY: uninstall
uninstall: manifests kustomize ## Uninstall CRDs from the K8s cluster specified in ~/.kube/config. Call with ignore-not-found=true to ignore resource not found errors during deletion.
	$(KUSTOMIZE) build config/crd | kubectl delete --ignore-not-found=$(ignore-not-found) -f -

.PHONY: deploy
deploy: manifests kustomize ## Deploy controller to the K8s cluster specified in ~/.kube/config.
	cd config/manager && $(KUSTOMIZE) edit set image manager=$(KOORD_MANAGER_IMG) scheduler=$(KOORD_SCHEDULER_IMG) descheduler=$(KOORD_DESCHEDULER_IMG) koordlet=$(KOORDLET_IMG)
	@hack/kustomize.sh $(KUSTOMIZE) | kubectl apply -f -

.PHONY: undeploy
undeploy: ## Undeploy controller from the K8s cluster specified in ~/.kube/config. Call with ignore-not-found=true to ignore resource not found errors during deletion.
	@hack/kustomize.sh $(KUSTOMIZE) | kubectl delete --ignore-not-found=$(ignore-not-found) -f -

##@ Build Dependencies

## Location to install dependencies to
LOCALBIN ?= $(shell pwd)/bin
$(LOCALBIN):
	mkdir -p $(LOCALBIN)

## Tool Binaries
KUSTOMIZE ?= $(LOCALBIN)/kustomize
CONTROLLER_GEN ?= $(LOCALBIN)/controller-gen
ENVTEST ?= $(LOCALBIN)/setup-envtest
GOLANGCI_LINT ?= $(LOCALBIN)/golangci-lint
GINKGO ?= $(LOCALBIN)/ginkgo
HACK_DIR ?= $(PWD)/hack

## Tool Versions
KUSTOMIZE_VERSION ?= v3.8.7
CONTROLLER_TOOLS_VERSION ?= v0.8.0
GOLANGCILINT_VERSION ?= v1.47.3
GINKGO_VERSION ?= v1.16.4

KUSTOMIZE_INSTALL_SCRIPT ?= "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"
.PHONY: kustomize
kustomize: $(KUSTOMIZE) ## Download kustomize locally if necessary.
$(KUSTOMIZE): $(LOCALBIN)
	curl -s $(KUSTOMIZE_INSTALL_SCRIPT) | bash -s -- $(subst v,,$(KUSTOMIZE_VERSION)) $(LOCALBIN)

.PHONY: controller-gen
controller-gen: $(CONTROLLER_GEN) ## Download controller-gen locally if necessary.
$(CONTROLLER_GEN): $(LOCALBIN)
	GOBIN=$(LOCALBIN) go install sigs.k8s.io/controller-tools/cmd/controller-gen@$(CONTROLLER_TOOLS_VERSION)

.PHONY: envtest
envtest: $(ENVTEST) ## Download envtest-setup locally if necessary.
$(ENVTEST): $(LOCALBIN)
	GOBIN=$(LOCALBIN) go install sigs.k8s.io/controller-runtime/tools/setup-envtest@latest

.PHONY: golangci-lint
golangci-lint: $(GOLANGCI_LINT) ## Download golangci-lint locally if necessary.
$(GOLANGCI_LINT): $(LOCALBIN)
	GOBIN=$(LOCALBIN) go install github.com/golangci/golangci-lint/cmd/golangci-lint@$(GOLANGCILINT_VERSION)

.PHONY: ginkgo
ginkgo: $(GINKGO) ## Download ginkgo locally if necessary.
$(GINKGO): $(LOCALBIN)
	GOBIN=$(LOCALBIN) go install github.com/onsi/ginkgo/ginkgo@$(GINKGO_VERSION)
