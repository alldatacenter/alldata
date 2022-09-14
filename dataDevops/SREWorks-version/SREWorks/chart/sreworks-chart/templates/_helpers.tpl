

{{- define "swcli.endpoint" -}}
{{- if .Values.swcli.endpoint -}}
{{- .Values.swcli.endpoint -}}
{{- else -}}
{{- printf "http://%s-%s" .Release.Name "appmanager" -}}
{{- end -}}
{{- end -}}

{{- define "images.swcliBuiltinPackage" -}}
{{- if .Values.images.swcliBuiltinPackage -}}
{{- .Values.images.swcliBuiltinPackage -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "swcli-builtin-package" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}


{{- define "nodePort" -}}
{{- if eq .Values.global.accessMode "ingress" -}}
"80"
{{- else -}}
{{- (split ":" .Values.appmanager.home.url)._2 | quote -}}
{{- end -}}
{{- end -}}

{{- define "dataops.namespace" -}}
{{- printf "%s-%s" .Release.Namespace "dataops" -}}
{{- end -}}

{{- define "aiops.namespace" -}}
{{- printf "%s-%s" .Release.Namespace "aiops" -}}
{{- end -}}

{{- define "minio.endpoint" -}}
{{- if .Values.appmanager.server.package.endpoint -}}
{{- .Values.appmanager.server.package.endpoint | quote -}}
{{- else -}}
{{- printf "%s-%s.%s:%s" .Release.Name "minio" .Release.Namespace "9000" | quote -}}
{{- end -}}
{{- end -}}

{{- define "migrate.image" -}}
{{- if eq .Values.global.artifacts.migrateImage "sw-migrate" -}}
{{ .Values.global.images.registry }}/sw-migrate:{{ .Values.global.images.tag }}
{{- else -}}
{{ .Values.global.artifacts.migrateImage }}
{{- end -}}
{{- end -}}

{{- define "postrun.image" -}}
{{- if eq .Values.global.artifacts.postrunImage "sw-postrun" -}}
{{ .Values.global.images.registry }}/sw-postrun:{{ .Values.global.images.tag }}
{{- else -}}
{{ .Values.global.artifacts.postrunImage }}
{{- end -}}
{{- end -}}

{{- define "python.pip.domain" -}}
{{- (split "/" ((split "://" .Values.global.artifacts.pythonPip)._1))._0 | quote -}}
{{- end -}}

{{- define "images.progressCheck" -}}
{{- if .Values.images.progressCheck -}}
{{- .Values.images.progressCheck -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-progress-check" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}




