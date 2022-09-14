default: test

include ../../../helpers/examples.mk

RELEASE := helm-kibana-security

install:
	helm upgrade --wait --timeout=$(TIMEOUT) --install --values values.yaml $(RELEASE) ../../

test: secrets install goss

purge:
	kubectl delete secret kibana || true
	helm del $(RELEASE)

secrets:
	encryptionkey=$$(docker run --rm busybox:1.31.1 /bin/sh -c "< /dev/urandom tr -dc _A-Za-z0-9 | head -c50") && \
	kubectl create secret generic kibana --from-literal=encryptionkey=$$encryptionkey
