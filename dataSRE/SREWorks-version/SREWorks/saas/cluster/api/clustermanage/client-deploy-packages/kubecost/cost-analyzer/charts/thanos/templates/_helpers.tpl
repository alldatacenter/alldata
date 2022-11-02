{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "thanos.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "thanos.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "thanos.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Create a default fully qualified component name from the full app name and a component name.
We truncate the full name at 63 - 1 (last dash) - len(component name) chars because some Kubernetes name fields are limited to this (by the DNS naming spec)
and we want to make sure that the component is included in the name.
*/}}
{{- define "thanos.componentname" -}}
{{- $global := index . 0 -}}
{{- $component := index . 1 | trimPrefix "-" -}}
{{- printf "%s-%s" (include "thanos.fullname" $global | trunc (sub 62 (len $component) | int) | trimSuffix "-" ) $component | trimSuffix "-" -}}
{{- end -}}

{{/*

*/}}
{{- define "thanos.secretname" }}
{{- default (include "thanos.name" .) .Values.storeSecretName }}
{{- end }}
