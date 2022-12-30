default: test

include ../../../helpers/examples.mk

RELEASE := helm-es-default

install:
	helm upgrade --wait --timeout=600 --install $(RELEASE) ../../

restart:
	helm upgrade --set terminationGracePeriod=121 --wait --timeout=600 --install $(RELEASE) ../../

test: install goss

purge:
	helm del --purge $(RELEASE)
