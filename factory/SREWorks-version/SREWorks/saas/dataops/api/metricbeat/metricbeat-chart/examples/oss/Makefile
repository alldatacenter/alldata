default: test

include ../../../helpers/examples.mk

RELEASE := helm-metricbeat-oss
GOSS_SELECTOR = release=$(RELEASE),app=helm-metricbeat-oss-metricbeat

install:
	helm upgrade --wait --timeout=$(TIMEOUT) --install --values values.yaml $(RELEASE) ../../

test-metrics:
	 GOSS_FILE=goss-metrics.yaml make goss GOSS_SELECTOR=release=$(RELEASE),app=helm-metricbeat-oss-metricbeat-metrics

test: install goss test-metrics

purge:
	helm del $(RELEASE)
