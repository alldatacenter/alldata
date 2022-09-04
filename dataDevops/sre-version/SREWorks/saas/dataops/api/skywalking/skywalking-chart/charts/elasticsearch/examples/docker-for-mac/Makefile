default: test

RELEASE := helm-es-docker-for-mac

install:
	helm upgrade --wait --timeout=900 --install --values values.yaml $(RELEASE) ../../

test: install
	helm test $(RELEASE)

purge:
	helm del --purge $(RELEASE)
