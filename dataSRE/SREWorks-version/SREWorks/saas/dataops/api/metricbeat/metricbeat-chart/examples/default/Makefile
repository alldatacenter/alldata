default: test

include ../../../helpers/examples.mk

RELEASE = helm-metricbeat-default
GOSS_SELECTOR = release=$(RELEASE),app=helm-metricbeat-default-metricbeat

install:
	helm upgrade --wait --timeout=$(TIMEOUT) --install $(RELEASE) ../../

test-metrics:
	 GOSS_FILE=goss-metrics.yaml make goss GOSS_SELECTOR=release=$(RELEASE),app=helm-metricbeat-default-metricbeat-metrics

test: install goss test-metrics

purge:
	helm del $(RELEASE)
