{{/*
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
*/}}

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "inlong.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "inlong.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "inlong.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create the common labels.
*/}}
{{- define "inlong.commonLabels" -}}
app: {{ template "inlong.name" . }}
chart: {{ template "inlong.chart" . }}
release: {{ .Release.Name }}
heritage: {{ .Release.Service }}
cluster: {{ template "inlong.fullname" . }}
{{- end -}}

{{/*
Create the template labels.
*/}}
{{- define "inlong.template.labels" -}}
app: {{ template "inlong.name" . }}
release: {{ .Release.Name }}
cluster: {{ template "inlong.fullname" . }}
{{- end -}}

{{/*
Create the match labels.
*/}}
{{- define "inlong.matchLabels" -}}
app: {{ template "inlong.name" . }}
release: {{ .Release.Name }}
{{- end -}}

{{/*
Define the audit hostname
*/}}
{{- define "inlong.audit.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.audit.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the dashboard hostname
*/}}
{{- define "inlong.dashboard.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.dashboard.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the manager hostname
*/}}
{{- define "inlong.manager.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.manager.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the dataproxy hostname
*/}}
{{- define "inlong.dataproxy.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.dataproxy.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the tubemq manager hostname
*/}}
{{- define "inlong.tubemqManager.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.tubemqManager.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the tubemq master hostname
*/}}
{{- define "inlong.tubemqMaster.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.tubemqMaster.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Define the mysql hostname
*/}}
{{- define "inlong.mysql.hostname" -}}
{{- if .Values.external.mysql.enabled -}}
{{ .Values.external.mysql.hostname }}
{{- else -}}
{{ template "inlong.fullname" . }}-{{ .Values.mysql.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}
{{- end -}}

{{/*
Define the mysql port
*/}}
{{- define "inlong.mysql.port" -}}
{{- if .Values.external.mysql.enabled -}}
{{ .Values.external.mysql.port }}
{{- else -}}
{{ .Values.mysql.port }}
{{- end -}}
{{- end -}}

{{/*
Define the mysql username
*/}}
{{- define "inlong.mysql.username" -}}
{{- if .Values.external.mysql.enabled -}}
{{ .Values.external.mysql.username }}
{{- else -}}
{{ .Values.mysql.username }}
{{- end -}}
{{- end -}}

{{/*
Define the zookeeper hostname
*/}}
{{- define "inlong.zookeeper.hostname" -}}
{{ template "inlong.fullname" . }}-{{ .Values.zookeeper.component }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end -}}

{{/*
Common labels
*/}}
{{- define "inlong.labels" -}}
helm.sh/chart: {{ include "inlong.chart" . }}
{{ include "inlong.selectorLabels" . }}
{{- if .Chart.AppVersion -}}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "inlong.selectorLabels" -}}
app.kubernetes.io/name: {{ include "inlong.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "inlong.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "inlong.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
{{- default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}
