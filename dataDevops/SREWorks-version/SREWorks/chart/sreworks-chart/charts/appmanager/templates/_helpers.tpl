{{- define "images.server" -}}
{{- if .Values.images.server -}}
{{- .Values.images.server -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-paas-appmanager" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}

{{- define "images.operator" -}}
{{- if .Values.images.operator -}}
{{- .Values.images.operator -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-paas-appmanager-operator" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}

{{- define "images.dbMigration" -}}
{{- if .Values.images.dbMigration -}}
{{- .Values.images.dbMigration -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-paas-appmanager-db-migration" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}

{{- define "images.postrun" -}}
{{- if .Values.images.postrun -}}
{{- .Values.images.postrun -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-paas-appmanager-postrun" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}

{{- define "images.clusterInitJob" -}}
{{- if .Values.images.clusterInitJob -}}
{{- .Values.images.clusterInitJob -}}
{{- else -}}
{{- printf "%s/%s:%s" .Values.global.images.registry "sw-paas-appmanager-cluster-init" .Values.global.images.tag -}}
{{- end -}}
{{- end -}}

{{- define "domain.base" -}}
{{- (split ":" ((split "://" .Values.home.url)._1))._0 | quote -}}
{{- end -}}

{{- define "domain.networkProtocol" -}}
{{- (split "://" .Values.home.url)._0 | quote -}}
{{- end -}}

{{- define "name.server-configmap" -}}
{{- printf "%s-%s" .Release.Name "appmanager-server-configmap" -}}
{{- end -}}

{{- define "name.server-init-configmap" -}}
{{- printf "%s-%s" .Release.Name "appmanager-server-init-configmap" -}}
{{- end -}}

{{- define "name.server" -}}
{{- printf "%s-%s" .Release.Name "appmanager-server" -}}
{{- end -}}

{{- define "name.service" -}}
{{- printf "%s-%s" .Release.Name "appmanager" -}}
{{- end -}}

{{- define "server.database.host" -}}
{{- if .Values.server.database.host -}}
{{- .Values.server.database.host | quote -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name "mysql" | quote -}}
{{- end -}}
{{- end -}}

{{- define "server.rocketmq.namesrv" -}}
{{- if .Values.server.rocketmq.namesrv -}}
{{- .Values.server.rocketmq.namesrv | quote -}}
{{- else -}}
{{- printf "%s-%s:%s" .Release.Name "name-server-service" "9876" | quote -}}
{{- end -}}
{{- end -}}

{{- define "server.package.endpoint" -}}
{{- if .Values.server.package.endpoint -}}
{{- .Values.server.package.endpoint | quote -}}
{{- else -}}
{{- printf "%s-%s:%s" .Release.Name "minio" "9000" | quote -}}
{{- end -}}
{{- end -}}

{{- define "server.redis.host" -}}
{{- if .Values.server.redis.host -}}
{{- .Values.server.redis.host | quote -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name "redis-master" | quote -}}
{{- end -}}
{{- end -}}

{{- define "server.kafkaBroker" -}}
{{- if .Values.server.kafkaBroker -}}
{{- .Values.server.kafkaBroker | quote -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name "kafka" | quote -}}
{{- end -}}
{{- end -}}




