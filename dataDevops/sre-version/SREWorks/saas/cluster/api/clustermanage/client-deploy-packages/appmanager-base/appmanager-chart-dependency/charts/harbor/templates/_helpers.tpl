{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "harbor.name" -}}
{{- default "harbor" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "harbor.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default "harbor" .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/* Helm required labels */}}
{{- define "harbor.labels" -}}
heritage: {{ .Release.Service }}
release: {{ .Release.Name }}
chart: {{ .Chart.Name }}
app: "{{ template "harbor.name" . }}"
{{- end -}}

{{/* matchLabels */}}
{{- define "harbor.matchLabels" -}}
release: {{ .Release.Name }}
app: "{{ template "harbor.name" . }}"
{{- end -}}

{{- define "harbor.autoGenCert" -}}
  {{- if and .Values.expose.tls.enabled (eq .Values.expose.tls.certSource "auto") -}}
    {{- printf "true" -}}
  {{- else -}}
    {{- printf "false" -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.autoGenCertForIngress" -}}
  {{- if and (eq (include "harbor.autoGenCert" .) "true") (eq .Values.expose.type "ingress") -}}
    {{- printf "true" -}}
  {{- else -}}
    {{- printf "false" -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.autoGenCertForNginx" -}}
  {{- if and (eq (include "harbor.autoGenCert" .) "true") (ne .Values.expose.type "ingress") -}}
    {{- printf "true" -}}
  {{- else -}}
    {{- printf "false" -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.host" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- template "harbor.database" . }}
  {{- else -}}
    {{- .Values.database.external.host -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.port" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "5432" -}}
  {{- else -}}
    {{- .Values.database.external.port -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.username" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "postgres" -}}
  {{- else -}}
    {{- .Values.database.external.username -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.rawPassword" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- .Values.database.internal.password -}}
  {{- else -}}
    {{- .Values.database.external.password -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.escapedRawPassword" -}}
  {{- include "harbor.database.rawPassword" . | urlquery | replace "+" "%20" -}}
{{- end -}}

{{- define "harbor.database.encryptedPassword" -}}
  {{- include "harbor.database.rawPassword" . | b64enc | quote -}}
{{- end -}}

{{- define "harbor.database.coreDatabase" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "registry" -}}
  {{- else -}}
    {{- .Values.database.external.coreDatabase -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.notaryServerDatabase" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "notaryserver" -}}
  {{- else -}}
    {{- .Values.database.external.notaryServerDatabase -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.notarySignerDatabase" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "notarysigner" -}}
  {{- else -}}
    {{- .Values.database.external.notarySignerDatabase -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.sslmode" -}}
  {{- if eq .Values.database.type "internal" -}}
    {{- printf "%s" "disable" -}}
  {{- else -}}
    {{- .Values.database.external.sslmode -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.database.notaryServer" -}}
postgres://{{ template "harbor.database.username" . }}:{{ template "harbor.database.escapedRawPassword" . }}@{{ template "harbor.database.host" . }}:{{ template "harbor.database.port" . }}/{{ template "harbor.database.notaryServerDatabase" . }}?sslmode={{ template "harbor.database.sslmode" . }}
{{- end -}}

{{- define "harbor.database.notarySigner" -}}
postgres://{{ template "harbor.database.username" . }}:{{ template "harbor.database.escapedRawPassword" . }}@{{ template "harbor.database.host" . }}:{{ template "harbor.database.port" . }}/{{ template "harbor.database.notarySignerDatabase" . }}?sslmode={{ template "harbor.database.sslmode" . }}
{{- end -}}

{{- define "harbor.redis.scheme" -}}
  {{- with .Values.redis }}
    {{- ternary "redis+sentinel" "redis"  (and (eq .type "external" ) (not (not .external.sentinelMasterSet))) }}
  {{- end }}
{{- end -}}

/*host:port*/
{{- define "harbor.redis.addr" -}}
  {{- with .Values.redis }}
    {{- ternary (printf "%s:6379" (include "harbor.redis" $ )) .external.addr (eq .type "internal") }}
  {{- end }}
{{- end -}}

{{- define "harbor.redis.masterSet" -}}
  {{- with .Values.redis }}
    {{- ternary .external.sentinelMasterSet "" (eq "redis+sentinel" (include "harbor.redis.scheme" $)) }}
  {{- end }}
{{- end -}}

{{- define "harbor.redis.password" -}}
  {{- with .Values.redis }}
    {{- ternary "" .external.password (eq .type "internal") }}
  {{- end }}
{{- end -}}

/*scheme://[redis:password@]host:port[/master_set]*/
{{- define "harbor.redis.url" -}}
  {{- with .Values.redis }}
    {{- $path := ternary "" (printf "/%s" (include "harbor.redis.masterSet" $)) (not (include "harbor.redis.masterSet" $)) }}
    {{- $cred := ternary (printf "redis:%s@" (.external.password | urlquery)) "" (and (eq .type "external" ) (not (not .external.password))) }}
    {{- printf "%s://%s%s%s" (include "harbor.redis.scheme" $) $cred (include "harbor.redis.addr" $) $path -}}
  {{- end }}
{{- end -}}

/*scheme://[redis:password@]addr/db_index?idle_timeout_seconds=30*/
{{- define "harbor.redis.urlForCore" -}}
  {{- with .Values.redis }}
    {{- $index := ternary "0" .external.coreDatabaseIndex (eq .type "internal") }}
    {{- printf "%s/%s?idle_timeout_seconds=30" (include "harbor.redis.url" $) $index -}}
  {{- end }}
{{- end -}}

/*scheme://[redis:password@]addr/db_index*/
{{- define "harbor.redis.urlForJobservice" -}}
  {{- with .Values.redis }}
    {{- $index := ternary "1" .external.jobserviceDatabaseIndex (eq .type "internal") }}
    {{- printf "%s/%s" (include "harbor.redis.url" $) $index -}}
  {{- end }}
{{- end -}}

/*scheme://[redis:password@]addr/db_index?idle_timeout_seconds=30*/
{{- define "harbor.redis.urlForRegistry" -}}
  {{- with .Values.redis }}
    {{- $index := ternary "2" .external.registryDatabaseIndex (eq .type "internal") }}
    {{- printf "%s/%s?idle_timeout_seconds=30" (include "harbor.redis.url" $) $index -}}
  {{- end }}
{{- end -}}

/*scheme://[redis:password@]addr/db_index?idle_timeout_seconds=30*/
{{- define "harbor.redis.urlForTrivy" -}}
  {{- with .Values.redis }}
    {{- $index := ternary "5" .external.trivyAdapterIndex (eq .type "internal") }}
    {{- printf "%s/%s?idle_timeout_seconds=30" (include "harbor.redis.url" $) $index -}}
  {{- end }}
{{- end -}}

{{- define "harbor.redis.dbForRegistry" -}}
  {{- with .Values.redis }}
    {{- ternary "2" .external.registryDatabaseIndex (eq .type "internal") }}
  {{- end }}
{{- end -}}

{{- define "harbor.redis.dbForChartmuseum" -}}
  {{- with .Values.redis }}
    {{- ternary "3" .external.chartmuseumDatabaseIndex (eq .type "internal") }}
  {{- end }}
{{- end -}}

{{- define "harbor.portal" -}}
  {{- printf "%s-portal" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.core" -}}
  {{- printf "%s-core" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.redis" -}}
  {{- printf "%s-redis" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.jobservice" -}}
  {{- printf "%s-jobservice" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.registry" -}}
  {{- printf "%s-registry" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.chartmuseum" -}}
  {{- printf "%s-chartmuseum" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.database" -}}
  {{- printf "%s-database" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.trivy" -}}
  {{- printf "%s-trivy" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.notary-server" -}}
  {{- printf "%s-notary-server" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.notary-signer" -}}
  {{- printf "%s-notary-signer" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.nginx" -}}
  {{- printf "%s-nginx" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.exporter" -}}
  {{- printf "%s-exporter" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.ingress" -}}
  {{- printf "%s-ingress" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.ingress-notary" -}}
  {{- printf "%s-ingress-notary" (include "harbor.fullname" .) -}}
{{- end -}}

{{- define "harbor.noProxy" -}}
  {{- printf "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" (include "harbor.core" .) (include "harbor.jobservice" .) (include "harbor.database" .) (include "harbor.chartmuseum" .) (include "harbor.notary-server" .) (include "harbor.notary-signer" .) (include "harbor.registry" .) (include "harbor.portal" .) (include "harbor.trivy" .) (include "harbor.exporter" .) .Values.proxy.noProxy -}}
{{- end -}}

{{- define "harbor.caBundleVolume" -}}
- name: ca-bundle-certs
  secret:
    secretName: {{ .Values.caBundleSecretName }}
{{- end -}}

{{- define "harbor.caBundleVolumeMount" -}}
- name: ca-bundle-certs
  mountPath: /harbor_cust_cert/custom-ca.crt
  subPath: ca.crt
{{- end -}}

{{/* scheme for all components except notary because it only support http mode */}}
{{- define "harbor.component.scheme" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "https" -}}
  {{- else -}}
    {{- printf "http" -}}
  {{- end -}}
{{- end -}}

{{/* chartmuseum component container port */}}
{{- define "harbor.chartmuseum.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "9443" -}}
  {{- else -}}
    {{- printf "9999" -}}
  {{- end -}}
{{- end -}}

{{/* chartmuseum component service port */}}
{{- define "harbor.chartmuseum.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "443" -}}
  {{- else -}}
    {{- printf "80" -}}
  {{- end -}}
{{- end -}}

{{/* core component container port */}}
{{- define "harbor.core.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* core component service port */}}
{{- define "harbor.core.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "443" -}}
  {{- else -}}
    {{- printf "80" -}}
  {{- end -}}
{{- end -}}

{{/* jobservice component container port */}}
{{- define "harbor.jobservice.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* jobservice component service port */}}
{{- define "harbor.jobservice.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "443" -}}
  {{- else -}}
    {{- printf "80" -}}
  {{- end -}}
{{- end -}}

{{/* portal component container port */}}
{{- define "harbor.portal.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* portal component service port */}}
{{- define "harbor.portal.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "443" -}}
  {{- else -}}
    {{- printf "80" -}}
  {{- end -}}
{{- end -}}

{{/* registry component container port */}}
{{- define "harbor.registry.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "5443" -}}
  {{- else -}}
    {{- printf "5000" -}}
  {{- end -}}
{{- end -}}

{{/* registry component service port */}}
{{- define "harbor.registry.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "5443" -}}
  {{- else -}}
    {{- printf "5000" -}}
  {{- end -}}
{{- end -}}

{{/* registryctl component container port */}}
{{- define "harbor.registryctl.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* registryctl component service port */}}
{{- define "harbor.registryctl.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* trivy component container port */}}
{{- define "harbor.trivy.containerPort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* trivy component service port */}}
{{- define "harbor.trivy.servicePort" -}}
  {{- if .Values.internalTLS.enabled -}}
    {{- printf "8443" -}}
  {{- else -}}
    {{- printf "8080" -}}
  {{- end -}}
{{- end -}}

{{/* CORE_URL */}}
{{/* port is included in this url as a workaround for issue https://github.com/aquasecurity/harbor-scanner-trivy/issues/108 */}}
{{- define "harbor.coreURL" -}}
  {{- printf "%s://%s:%s" (include "harbor.component.scheme" .) (include "harbor.core" .) (include "harbor.core.servicePort" .) -}}
{{- end -}}

{{/* JOBSERVICE_URL */}}
{{- define "harbor.jobserviceURL" -}}
  {{- printf "%s://%s-jobservice" (include "harbor.component.scheme" .)  (include "harbor.fullname" .) -}}
{{- end -}}

{{/* PORTAL_URL */}}
{{- define "harbor.portalURL" -}}
  {{- printf "%s://%s" (include "harbor.component.scheme" .) (include "harbor.portal" .) -}}
{{- end -}}

{{/* REGISTRY_URL */}}
{{- define "harbor.registryURL" -}}
  {{- printf "%s://%s:%s" (include "harbor.component.scheme" .) (include "harbor.registry" .) (include "harbor.registry.servicePort" .) -}}
{{- end -}}

{{/* REGISTRY_CONTROLLER_URL */}}
{{- define "harbor.registryControllerURL" -}}
  {{- printf "%s://%s:%s" (include "harbor.component.scheme" .) (include "harbor.registry" .) (include "harbor.registryctl.servicePort" .) -}}
{{- end -}}

{{/* TOKEN_SERVICE_URL */}}
{{- define "harbor.tokenServiceURL" -}}
  {{- printf "%s/service/token" (include "harbor.coreURL" .) -}}
{{- end -}}

{{/* TRIVY_ADAPTER_URL */}}
{{- define "harbor.trivyAdapterURL" -}}
  {{- printf "%s://%s:%s" (include "harbor.component.scheme" .) (include "harbor.trivy" .) (include "harbor.trivy.servicePort" .) -}}
{{- end -}}

{{- define "harbor.internalTLS.chartmuseum.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.chartmuseum.secretName -}}
  {{- else -}}
    {{- printf "%s-chartmuseum-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.internalTLS.core.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.core.secretName -}}
  {{- else -}}
    {{- printf "%s-core-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.internalTLS.jobservice.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.jobservice.secretName -}}
  {{- else -}}
    {{- printf "%s-jobservice-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.internalTLS.portal.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.portal.secretName -}}
  {{- else -}}
    {{- printf "%s-portal-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.internalTLS.registry.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.registry.secretName -}}
  {{- else -}}
    {{- printf "%s-registry-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.internalTLS.trivy.secretName" -}}
  {{- if eq .Values.internalTLS.certSource "secret" -}}
    {{- .Values.internalTLS.trivy.secretName -}}
  {{- else -}}
    {{- printf "%s-trivy-internal-tls" (include "harbor.fullname" .) -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.tlsCoreSecretForIngress" -}}
  {{- if eq .Values.expose.tls.certSource "none" -}}
    {{- printf "" -}}
  {{- else if eq .Values.expose.tls.certSource "secret" -}}
    {{- .Values.expose.tls.secret.secretName -}}
  {{- else -}}
    {{- include "harbor.ingress" . -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.tlsNotarySecretForIngress" -}}
  {{- if eq .Values.expose.tls.certSource "none" -}}
    {{- printf "" -}}
  {{- else if eq .Values.expose.tls.certSource "secret" -}}
    {{- .Values.expose.tls.secret.notarySecretName -}}
  {{- else -}}
    {{- include "harbor.ingress" . -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.tlsSecretForNginx" -}}
  {{- if eq .Values.expose.tls.certSource "secret" -}}
    {{- .Values.expose.tls.secret.secretName -}}
  {{- else -}}
    {{- include "harbor.nginx" . -}}
  {{- end -}}
{{- end -}}

{{- define "harbor.metricsPortName" -}}
  {{- if .Values.internalTLS.enabled }}
    {{- printf "https-metrics" -}}
  {{- else -}}
    {{- printf "http-metrics" -}}
  {{- end -}}
{{- end -}}