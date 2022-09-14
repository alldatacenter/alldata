default: test

RELEASE := helm-es-kind

install:
	kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
	helm upgrade --wait --timeout=900 --install --values values.yaml $(RELEASE) ../../

test: install
	helm test $(RELEASE)

purge:
	helm del --purge $(RELEASE)
