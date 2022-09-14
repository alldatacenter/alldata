var GlobalBackendConf = {
    production: {
        baseUrl:"${URL_PAAS_HOME}/",
        apiEndpoint: "${URL_PAAS_HOME}/",
        docsEndpoint: "${URL_PAAS_HOME}/docs-gitbook/",
        envFlag: "${ENV_TYPE}",
        version:"v3.8",
        apiType: "${DNS_PAAS_HOME}",
        defaultProduct:"desktop",
        defaultNamespace:"${K8S_NAMESPACE}",
        defaultStageId:"prod",
        gateway:"${URL_PAAS_HOME}/gateway/",
        platformName: "${PLATFORM_NAME}",
        platformLogo: "${PLATFORM_LOGO}",
    }
}
