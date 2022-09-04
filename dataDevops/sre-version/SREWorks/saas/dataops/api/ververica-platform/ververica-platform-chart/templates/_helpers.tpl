{{/* vim: set filetype=mustache et sw=2 ts=2: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*

  Ververica Platform "public" templates

  Templates meant to be used from outside this file have names starting with `vvp.` but not
  `vvp.private`. All "public" templates are guaranteed to work regardless of the order in which they
  are invoked.

  Templates in this section which need to consider user-provided values, directly or indirectly, must
  always include the private template `vvp.private.process.application.config`. Additionally, any
  subsequent references to a value under the `vvp` key must use `.applicationConfig` and not
  `.Values`.

*/}}

{{/*
Create a Secret entry for a blob storage credentials value if the provider section and given key exist.
*/}}
{{- define "vvp.blob.storage.creds.secret.value" -}}
  {{- $config := required "Missing param 'config'" .config -}}
  {{- $provider := required "Missing param 'provider'" .provider -}}
  {{- $key := required "Missing param 'key'" .key -}}
  {{- if (index $config $provider) -}}
    {{- $value := index (index $config $provider) $key -}}
    {{- if $value -}}
      {{ if and (eq $provider "hdfs") (eq $key "keytab") }}
        {{/*
        Because hdfs.keytab is binary content, we let users directly configure the content after Base64 encoding, so b64enc will not be used here
        */}}
        {{- printf "%s.%s: %s" $provider $key ($value) -}}
      {{else}}
        {{- printf "%s.%s: %s" $provider $key (b64enc $value) -}}
      {{end}}
    {{- end -}}
  {{- end -}}
{{- end -}}

{{/*
Render the final application config after processing the user-provided values.
*/}}
{{- define "vvp.application.config" -}}
  {{- $_ := include "vvp.private.process.application.config" . -}}
  {{- toYaml .applicationConfig -}}
{{- end -}}

{{/*
Using a PVC is enabled if the provided persistence type requires it or it was manually enabled.
*/}}
{{- define "vvp.pvc.enabled" -}}
  {{- $_ := include "vvp.private.process.application.config" . -}}
  {{- or .Values.persistentVolume.enabled (eq (include "vvp.private.persistence.type.requires.pvc" .) "true") -}}
{{- end -}}

{{/*
A PVC should be created if enabled and no existing claim was provided.
*/}}
{{- define "vvp.should.create.pvc" -}}
  {{- and (eq (include "vvp.pvc.enabled" .) "true") (not .Values.persistentVolume.existingClaim) -}}
{{- end -}}

{{/*
Return the appmanager image repo, defaulting to ${vvp.registry}/vvp-appmanager if none was provided.
*/}}
{{- define "vvp.appmanager.repo" -}}
  {{- $defaultAppmanagerRepo := printf "%s/%s" .Values.vvp.registry "vvp-appmanager" -}}
  {{- .Values.appmanager.image.repository | default $defaultAppmanagerRepo -}}
{{- end -}}

{{/*
Return the gateway image repo, defaulting to ${vvp.registry}/vvp-gateway if none was provided.
*/}}
{{- define "vvp.gateway.repo" -}}
  {{- $defaultGatewayRepo := printf "%s/%s" .Values.vvp.registry "vvp-gateway" -}}
  {{- .Values.gateway.image.repository | default $defaultGatewayRepo -}}
{{- end -}}

{{/*
Return the ui image repo, defaulting to ${vvp.registry}/vvp-ui if none was provided.
*/}}
{{- define "vvp.ui.repo" -}}
  {{- $defaultUiRepo := printf "%s/%s" .Values.vvp.registry "vvp-ui" -}}
  {{- .Values.ui.image.repository | default $defaultUiRepo -}}
{{- end -}}

{{/*
Return the bootstrap token value if provided.
*/}}
{{- define "vvp.bootstrap.token" -}}
  {{- $auth := default dict .Values.vvp.auth -}}
  {{- $bootstrapToken := default dict $auth.bootstrapToken -}}
  {{- default "" $bootstrapToken.token -}}
{{- end -}}

{{/*

  Ververica Platform "private" templates

  Templates meant to be used only from inside this file have names starting with "vvp.private.". These
  templates generally require some pre-processing of the config in order to return the correct result.

*/}}

{{/*
Process the user-provided values into the final configuration.

The "inject" templates used in this template actually mutate the `applicationConfig` dict, so we
need to ensure that they are only called once by checking for and setting `.configHasBeenProcessed`
on the context.

This template first creates a new dict, copying in `.Values.vvp`, and stores it in the context as
`.applicationConfig`. Then it invokes the "inject" templates, starting with
`vvp.private.inject.top.level.config` so that the others (e.g. license and datasource) take
precedence. Finally, it sets the `configHasBeenProcessed` flag.
*/}}
{{- define "vvp.private.process.application.config" -}}
  {{- if not .configHasBeenProcessed -}}
    {{- $_ := set . "applicationConfig" (dict "vvp" (deepCopy .Values.vvp)) -}}
    {{- include "vvp.private.redact.bootstrap.token" . -}}
    {{- include "vvp.private.inject.top.level.config" . -}}
    {{- if not .Values.licenseConfigPath -}}
      {{- include "vvp.private.inject.license.config" . -}}
    {{- end -}}
    {{- if eq (include "vvp.private.is.community.edition" .) "true" -}}
      {{- include "vvp.private.inject.community.edition.presets" . -}}
    {{- end -}}
    {{- include "vvp.private.inject.datasource.config" . -}}
    {{- include "vvp.private.inject.discretionary.defaults" . -}}
    {{- include "vvp.private.inject.prescribed.defaults" . -}}
    {{- $_ = set . "configHasBeenProcessed" "true" -}}
  {{- end -}}
{{- end -}}

{{/*
Redact `vvp.auth.bootstrapToken` from the application config.
*/}}
{{- define "vvp.private.redact.bootstrap.token" -}}
  {{- $_ := unset .applicationConfig.vvp.auth "bootstrapToken" -}}
{{- end -}}

{{/*
Merge any values under `topLevelConfig` into the application config.
*/}}
{{- define "vvp.private.inject.top.level.config" -}}
  {{- $_ := mergeOverwrite .applicationConfig (.Values.topLevelConfig | default dict) -}}
{{- end -}}

{{- define "vvp.private.is.community.edition" -}}
  {{- $licenseConfig := .applicationConfig.vvp.license | default dict -}}
  {{- if or (.Values.licenseConfigPath) ($licenseConfig.data) -}}
    false
  {{- else -}}
    true
  {{- end -}}
{{- end -}}

{{/*
Apply any presets implied by Community Edition into the application config.

Must be applied after `vvp.private.inject.license.config`.
*/}}
{{- define "vvp.private.inject.community.edition.presets" -}}
  {{- $persistenceConfig := dict "type" "local" -}}
  {{- $_ := mergeOverwrite .applicationConfig.vvp (dict "persistence" $persistenceConfig) -}}
{{- end -}}

{{- define "vvp.private.license.warning" -}}
================================================================================
ERROR: No Ververica Platform license provided.

Actions to resolve:

* Provide a valid license at `vvp.license.data` or `licenseConfigPath`.

- OR -

* Read the printed Ververica Platform Community Edition license agreement and
indicate that you accept it by setting `acceptCommunityEditionLicense=true`.
================================================================================
{{- end -}}

{{- define "vvp.private.community.edition.license.agreement" -}}
  {{- .Files.Get "CommunityEditionLicenseAgreement.txt" -}}
{{- end -}}

{{/*
If the top level values key `acceptCommunityEditionLicense` is set, merge it into `vvp.license` in
the application config.
*/}}
{{- define "vvp.private.inject.license.config" -}}
  {{- $licenseConfig := .Values.vvp.license | default dict -}}
  {{- if .Values.acceptCommunityEditionLicense -}}
    {{- $_ := set $licenseConfig "acceptCommunityEditionLicense" .Values.acceptCommunityEditionLicense -}}
  {{- end -}}
  {{- if not (or $licenseConfig.data (eq (toString $licenseConfig.acceptCommunityEditionLicense) "true")) -}}
    {{- $licenseWarning := (include "vvp.private.license.warning" .) -}}
    {{- $communityEditionLicenseAgreement := (include "vvp.private.community.edition.license.agreement" .) -}}
    {{- printf "\n\n%s\n\n\n%s\n\n%s" $licenseWarning $communityEditionLicenseAgreement $licenseWarning | fail -}}
  {{- end -}}
  {{- $_ := mergeOverwrite .applicationConfig.vvp (dict "license" $licenseConfig) -}}
{{- end -}}

{{/*
Validate and print the provided or computed persistence type.

Should only be called after `vvp.private.inject.community.edition.presets` has run.

It must be one of:
- jdbc
- local
*/}}
{{- define "vvp.private.persistence.type" -}}
{{- $pt := required "vvp.persistence.type is required" .applicationConfig.vvp.persistence.type -}}
{{- if has $pt (list "jdbc" "local") -}}
  {{- $pt -}}
{{- else -}}
  {{- printf "Invalid persistence type '%s'" $pt | fail -}}
{{- end -}}
{{- end -}}

{{/*
local persistence requires a PVC.
*/}}
{{- define "vvp.private.persistence.type.requires.pvc" -}}
{{- $pt := include "vvp.private.persistence.type" . -}}
{{- eq $pt "local" -}}
{{- end -}}

{{/*
Validate the provided persistence and datasource values, merging them into the application config
as appropriate.

Since we expect users to provide Spring datasource config under `vvp.persistence.datasource`, this
template removes it from that key since it re-writes it to `spring.datasource`.

Must be applied after `vvp.private.inject.community.edition.presets`.
*/}}
{{- define "vvp.private.inject.datasource.config" -}}
{{- $pt := include "vvp.private.persistence.type" . -}}
{{- $dataSourceConfig := .applicationConfig.vvp.persistence.datasource | default dict -}}
{{- $_ := unset .applicationConfig.vvp.persistence "datasource" -}}
{{- if (eq $pt "local") -}}
  {{- if $dataSourceConfig.url -}}
    {{- printf "vvp.persistence.datasource.url may not be set when using '%s' persistence" $pt | fail -}}
  {{- end -}}
  {{- $_ := set $dataSourceConfig "url" "jdbc:sqlite:/vvp/data/vvp.db?journal_mode=WAL&synchronous=FULL&busy_timeout=10000" -}}

  {{/* See VVP-2838 */}}
  {{- $hikariConfig := $dataSourceConfig.hikari | default dict -}}
  {{- $_ = set $hikariConfig "maximum-pool-size" 1 -}}
  {{- $_ = set $dataSourceConfig "hikari" $hikariConfig -}}
{{- else -}}
  {{- if not $dataSourceConfig.url -}}
    {{- printf "'%s' persistence requires vvp.persistence.datasource.url to be set" $pt | fail -}}
  {{- end -}}
{{- end -}}
{{- $springConfig := dict "datasource" $dataSourceConfig -}}
{{- $_ = mergeOverwrite .applicationConfig (dict "spring" $springConfig) -}}
{{- end -}}

{{/*
Inject discretionary configuration defaults, such as filling in `vvp.registry` where required if an
alternate registry was otherwise unspecified.
*/}}
{{- define "vvp.private.inject.discretionary.defaults" -}}
  {{- $applicationConfigDefaults := (dict) -}}

  {{- $flinkDeploymentDefaultsDefaults := (dict) -}} {{/* sigh */}}
  {{- $_ := set $flinkDeploymentDefaultsDefaults "registry" .applicationConfig.vvp.registry -}}
  {{- $_ := set $applicationConfigDefaults "flinkDeploymentDefaults" $flinkDeploymentDefaultsDefaults -}}

  {{- $resultFetcher := (dict) -}}
  {{- $_ := set $resultFetcher "registry" .applicationConfig.vvp.registry -}}
  {{- $_ := set $applicationConfigDefaults "resultFetcher" $resultFetcher -}}

  {{- $_ := merge .applicationConfig.vvp $applicationConfigDefaults -}}
{{- end }}

{{/*
Inject prescribed configuration defaults, such as the Kubernetes namespace in which vvP is running
and the artifact fetcher config for appmanager.
*/}}
{{- define "vvp.private.inject.prescribed.defaults" -}}
  {{- $applicationConfigDefaults := (dict) -}}

  {{- $_ := set $applicationConfigDefaults "platformKubernetesNamespace" .Release.Namespace -}}

  {{- $appmanagerClusterConfig := (dict) -}}
  {{- $_ := set $appmanagerClusterConfig "kubernetes.artifact-fetcher.image-registry" .applicationConfig.vvp.registry -}}
  {{- $_ := set $appmanagerClusterConfig "kubernetes.artifact-fetcher.image-repository" "vvp-artifact-fetcher" -}}
  {{- $_ := set $appmanagerClusterConfig "kubernetes.artifact-fetcher.image-tag" .Values.appmanager.artifactFetcherTag -}}
  {{- $_ := set $applicationConfigDefaults "appmanager" (dict "cluster" $appmanagerClusterConfig) -}}

  {{- $_ := mergeOverwrite .applicationConfig.vvp $applicationConfigDefaults -}}
{{- end }}
