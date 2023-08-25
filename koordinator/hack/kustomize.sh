#!/bin/bash

KUSTOMIZE=$1

${KUSTOMIZE} build config/default | \
  perl -pe 's#koord-webhook-service#koordinator-webhook-service#g' | \
  perl -pe 's#koord-koordlet#koordlet#g' | \
  perl -pe 's#koord-slo-controller-config#slo-controller-config#g' | \
  perl -pe 's#koord-(.*)-webhook-configuration#koordinator-$1-webhook-configuration#g'
