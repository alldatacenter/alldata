{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "cost-analyzer.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "cost-analyzer.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create the fully qualified name for Prometheus server service.
*/}}
{{- define "cost-analyzer.prometheus.server.name" -}}
{{- if .Values.prometheus -}}
{{- if .Values.prometheus.server -}}
{{- if .Values.prometheus.server.fullnameOverride -}}
{{- .Values.prometheus.server.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-prometheus-server" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- printf "%s-prometheus-server" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- printf "%s-prometheus-server" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create the fully qualified name for Prometheus alertmanager service.
*/}}
{{- define "cost-analyzer.prometheus.alertmanager.name" -}}
{{- if .Values.prometheus -}}
{{- if .Values.prometheus.alertmanager -}}
{{- if .Values.prometheus.alertmanager.fullnameOverride -}}
{{- .Values.prometheus.alertmanager.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-prometheus-alertmanager" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- printf "%s-prometheus-alertmanager" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- printf "%s-prometheus-alertmanager" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "cost-analyzer.serviceName" -}}
{{- printf "%s-%s" .Release.Name "cost-analyzer" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Network Costs name used to tie autodiscovery of metrics to daemon set pods
*/}}
{{- define "cost-analyzer.networkCostsName" -}}
{{- printf "%s-%s" .Release.Name "network-costs" -}}
{{- end -}}

{{- define "kubecost.clusterControllerName" -}}
{{- printf "%s-%s" .Release.Name "cluster-controller" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "cost-analyzer.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create the name of the service account
*/}}
{{- define "cost-analyzer.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "cost-analyzer.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Create the common labels.
*/}}
{{- define "cost-analyzer.commonLabels" -}}
app.kubernetes.io/name: {{ include "cost-analyzer.name" . }}
helm.sh/chart: {{ include "cost-analyzer.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app: cost-analyzer
{{- end -}}

{{/*
Create the selector labels.
*/}}
{{- define "cost-analyzer.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cost-analyzer.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: cost-analyzer
{{- end -}}

{{/*
Return the appropriate apiVersion for daemonset.
*/}}
{{- define "cost-analyzer.daemonset.apiVersion" -}}
{{- if semverCompare "<1.9-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "extensions/v1beta1" -}}
{{- else if semverCompare "^1.9-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "apps/v1" -}}
{{- end -}}
{{- end -}}

{{/*
Return the appropriate apiVersion for priorityClass.
*/}}
{{- define "cost-analyzer.priorityClass.apiVersion" -}}
{{- if semverCompare "<1.14-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "scheduling.k8s.io/v1beta1" -}}
{{- else if semverCompare "^1.14-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "scheduling.k8s.io/v1" -}}
{{- end -}}
{{- end -}}

{{/*
Return the appropriate apiVersion for networkpolicy.
*/}}
{{- define "cost-analyzer.networkPolicy.apiVersion" -}}
{{- if semverCompare ">=1.4-0, <1.7-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "extensions/v1beta1" -}}
{{- else if semverCompare "^1.7-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "networking.k8s.io/v1" -}}
{{- end -}}
{{- end -}}

{{/*
Return the appropriate apiVersion for podsecuritypolicy.
*/}}
{{- define "cost-analyzer.podSecurityPolicy.apiVersion" -}}
{{- if semverCompare ">=1.3-0, <1.10-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "extensions/v1beta1" -}}
{{- else if semverCompare "^1.10-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "policy/v1beta1" -}}
{{- end -}}
{{- end -}}

{{/*
Recursive filter which accepts a map containing an input map (.v) and an output map (.r). The template
will traverse all values inside .v recursively writing non-map values to the output .r. If a nested map 
is discovered, we look for an 'enabled' key. If it doesn't exist, we continue traversing the 
map. If it does exist, we omit the inner map traversal iff enabled is false. This filter writes the 
enabled only version to the output .r
*/}}
{{- define "cost-analyzer.filter" -}}
{{- $v := .v }}
{{- $r := .r }}
{{- range $key, $value := .v }}
    {{- $tp := kindOf $value -}}
    {{- if eq $tp "map" -}}
        {{- $isEnabled := true -}}
        {{- if (hasKey $value "enabled") -}}
            {{- $isEnabled = $value.enabled -}}
        {{- end -}}
        {{- if $isEnabled -}}
            {{- $rr := "{}" | fromYaml }}
            {{- template "cost-analyzer.filter" (dict "v" $value "r" $rr) }}
            {{- $_ := set $r $key $rr -}}
        {{- end -}}
    {{- else -}}
        {{- $_ := set $r $key $value -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
This template accepts a map and returns a base64 encoded json version of the map where all disabled
leaf nodes are omitted.

The implied use case is {{ template "cost-analyzer.filterEnabled" .Values }}
*/}}
{{- define "cost-analyzer.filterEnabled" -}}
{{- $result := "{}" | fromYaml }}
{{- template "cost-analyzer.filter" (dict "v" . "r" $result) }}
{{- $result | toJson | b64enc }}
{{- end -}}
