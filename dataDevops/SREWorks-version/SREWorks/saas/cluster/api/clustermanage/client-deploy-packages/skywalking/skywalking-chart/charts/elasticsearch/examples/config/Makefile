default: test
include ../../../helpers/examples.mk

RELEASE := helm-es-config

install:
	helm upgrade --wait --timeout=600 --install $(RELEASE) --values ./values.yaml ../../

secrets:
	kubectl delete secret elastic-config-credentials elastic-config-secret elastic-config-slack elastic-config-custom-path || true
	kubectl create secret generic elastic-config-credentials --from-literal=password=changeme --from-literal=username=elastic
	kubectl create secret generic elastic-config-slack --from-literal=xpack.notification.slack.account.monitoring.secure_url='https://hooks.slack.com/services/asdasdasd/asdasdas/asdasd'
	kubectl create secret generic elastic-config-secret --from-file=xpack.watcher.encryption_key=./watcher_encryption_key
	kubectl create secret generic elastic-config-custom-path --from-literal=slack_url='https://hooks.slack.com/services/asdasdasd/asdasdas/asdasd' --from-literal=thing_i_don_tcare_about=test

test: secrets install goss

purge:
	helm del --purge $(RELEASE)
