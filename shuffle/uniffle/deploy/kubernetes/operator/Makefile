#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# REGISTRY URL to use all building/pushing image targets
REGISTRY ?= ''

ifneq ('', ${REGISTRY})
REGISTRY := $(addsuffix /, ${REGISTRY})
endif

MODULES ?= webhook controller

# ENVTEST_K8S_VERSION refers to the version of kubebuilder assets to be downloaded by envtest binary.
ENVTEST_K8S_VERSION = 1.24.1

LOCAL_DIR=$(shell pwd)/local

# Get the currently used golang install path (in GOPATH/bin, unless GOBIN is set)
ifeq (,$(shell go env GOBIN))
GOBIN=$(shell go env GOPATH)/bin
else
GOBIN=$(shell go env GOBIN)
endif

# set shell to bash
SHELL := /bin/bash

.PHONY: all
all: build

.PHONY: manifests
manifests: controller-gen ## Generate WebhookConfiguration, ClusterRole and CustomResourceDefinition objects.
	$(CONTROLLER_GEN) rbac:roleName=manager-role crd webhook paths="./..." output:crd:artifacts:config=config/crd/bases
	sh hack/add_header_for_crds.sh

.PHONY: generate
generate: controller-gen ## Generate code containing DeepCopy, DeepCopyInto, and DeepCopyObject method implementations.
	$(CONTROLLER_GEN) object:headerFile="hack/headers/header.go.txt" paths="./..."

.PHONY: version
version:
	@version=$(VERSION); \
	[ ! -z "$$version" ] || version="$$(git describe --dirty --always --tags | sed 's/-/./g')"; \
	if [[ $$version == *dirty ]]; then \
		echo image version $$version is dirty, dirty files:; \
		git diff; \
		exit 1; \
	fi; \
	touch VERSION && echo $$version > VERSION && echo image version is $$version

.PHONY: fmt
fmt: goimports revive ## Run go fmt against code.
	go fmt ./...

.PHONY: vet
vet: ## Run go vet against code.
	go vet ./...

.PHONY: test
test: manifests generate fmt vet envtest ## Run tests.
	export KUBEBUILDER_ASSETS=$(shell $(ENVTEST) use $(ENVTEST_K8S_VERSION) --bin-dir $(LOCAL_DIR) -p path); \
	go test ./... -coverprofile cover.out

.PHONY: build
build: test ## Build manager binary.
	for module in ${MODULES}; do \
  		go build -o bin/$$module cmd/$$module/main.go; \
    done

# Build the docker image
docker-build: build version
	@for module in ${MODULES}; do \
		echo registry is ${REGISTRY}; \
		image_name=${REGISTRY}rss-$$module; \
		echo image_name $$image_name; \
		image=$$image_name:$$(cat VERSION); \
		echo building $$image;\
		docker build . -t $$image --build-arg MODULE=$$module -f hack/Dockerfile; \
	done

# Push the docker image
docker-push: docker-build
	@for module in ${MODULES}; do \
		image_name=${REGISTRY}rss-$$module; \
		echo image_name $$image_name; \
		image=$$image_name:$$(cat VERSION); \
		echo pushing $$image;\
		docker push $$image; \
	done

CONTROLLER_GEN = $(LOCAL_DIR)/controller-gen
.PHONY: controller-gen
controller-gen: ## Download controller-gen locally if necessary.
	$(call go-get-tool,$(CONTROLLER_GEN),sigs.k8s.io/controller-tools/cmd/controller-gen@v0.7.0)
	./hack/update-codegen.sh

KUSTOMIZE = $(LOCAL_DIR)/kustomize
.PHONY: kustomize
kustomize: ## Download kustomize locally if necessary.
	$(call go-get-tool,$(KUSTOMIZE),sigs.k8s.io/kustomize/kustomize/v4@v4.5.2)

ENVTEST = $(LOCAL_DIR)/setup-envtest
.PHONY: envtest
envtest: ## Download envtest-setup locally if necessary.
	$(call go-get-tool,$(ENVTEST),sigs.k8s.io/controller-runtime/tools/setup-envtest@latest)

GOIMPORTS = $(LOCAL_DIR)/goimports
.PHONY: goimports
goimports: ## Download goimports locally if necessary.
	$(call go-get-tool,$(GOIMPORTS),golang.org/x/tools/cmd/goimports@latest)
	$(GOIMPORTS) -local github.com/apache/incubator-uniffle/deploy/kubernetes/operator -w .

REVIVE = $(LOCAL_DIR)/revive
.PHONY: revive
revive: ## Download revive locally if necessary.
	$(call go-get-tool,$(REVIVE),github.com/mgechev/revive@latest)
	files=$$(find . -name '*.go' | egrep -v './vendor|zz_generated|./pkg/generated|./api|./hack'); \
    $(REVIVE) -config hack/revive.toml -formatter friendly $$files


# go-get-tool will 'go get' any package $2 and install it to $1.
PROJECT_DIR := $(shell dirname $(abspath $(lastword $(MAKEFILE_LIST))))
define go-get-tool
@[ -f $(1) ] || { \
set -e ;\
TMP_DIR=$$(mktemp -d) ;\
cd $$TMP_DIR ;\
go mod init tmp ;\
echo "Downloading $(2)" ;\
GOBIN=$(PROJECT_DIR)/local go install $(2) ;\
rm -rf $$TMP_DIR ;\
}
endef
