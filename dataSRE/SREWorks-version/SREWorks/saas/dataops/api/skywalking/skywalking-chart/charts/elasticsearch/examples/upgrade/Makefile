default: test

include ../../../helpers/examples.mk

RELEASE := helm-es-upgrade

# Right now the version is hardcoded because helm install will ignore
# anything with an alpha tag when trying to install the latest release
# This hardcoding can be removed once we drop the alpha tag
# The "--set terminationGracePeriod=121" always makes sure that a rolling
# upgrade is forced for this test
install:
	helm repo add elastic https://helm.elastic.co && \
	helm upgrade --wait --timeout=600 --install $(RELEASE) elastic/elasticsearch --version 7.0.0-alpha1 --set clusterName=upgrade ; \
	kubectl rollout status sts/upgrade-master --timeout=600s
	helm upgrade --wait --timeout=600 --set terminationGracePeriod=121 --install $(RELEASE) ../../ --set clusterName=upgrade ; \
	kubectl rollout status sts/upgrade-master --timeout=600s

init:
	helm init --client-only

test: init install goss

purge:
	helm del --purge $(RELEASE)
