default: test
include ../../../helpers/examples.mk

RELEASE := kibana

template:
	helm template --values values.yaml ../../

install:
	helm upgrade --wait --timeout=$(TIMEOUT) --install --values values.yaml $(RELEASE) ../../

test: install goss

purge:
	helm del $(RELEASE)
