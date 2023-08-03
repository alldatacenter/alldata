#!/bin/bash

# Generate static env.config.js for UI app
envfile=/usr/share/nginx/html/env-config.js
echo "window.environment = {" > $envfile

if [[ -z "${REACT_APP_AZURE_CLIENT_ID}" ]]; then
    echo "Environment variable REACT_APP_AZURE_CLIENT_ID is not defined, skipping"
else
    echo "  \"azureClientId\": \"${REACT_APP_AZURE_CLIENT_ID}\"," >> $envfile
fi

if [[ -z "${REACT_APP_AZURE_TENANT_ID}" ]]; then
    echo "Environment variable REACT_APP_AZURE_TENANT_ID is not defined, skipping"
else
    echo "  \"azureTenantId\": \"${REACT_APP_AZURE_TENANT_ID}\"," >> $envfile
fi

if [[ -z "${REACT_APP_ENABLE_RBAC}" ]]; then
    echo "Environment variable REACT_APP_ENABLE_RBAC is not defined, skipping"
else
    echo "  \"enableRBAC\": \"${REACT_APP_ENABLE_RBAC}\"," >> $envfile
fi

echo "}" >> $envfile

echo "Successfully generated ${envfile} with following content"
cat $envfile

# Start nginx
nginx

# Start API app
LISTENING_PORT="8000"

if [ "$REACT_APP_ENABLE_RBAC" == "true" ]; then
    echo "RBAC flag configured and set to true, launch both rbac and reigstry apps"
    if [ "x$PURVIEW_NAME" == "x" ]; then
        echo "Purview flag is not configured, run SQL registry"
        cd sql-registry
        nohup uvicorn main:app --host 0.0.0.0 --port 18000 &
    else
        echo "Purview flag is configured, run Purview registry"
        cd purview-registry
        nohup uvicorn main:app --host 0.0.0.0 --port 18000 &
    fi
    echo "Run rbac app"
    cd ../access_control
    export RBAC_REGISTRY_URL="http://127.0.0.1:18000/api/v1"
    export RBAC_API_BASE="${API_BASE}"
    export RBAC_AAD_INSTANCE="https://login.microsoftonline.com"
    export RBAC_API_CLIENT_ID="${REACT_APP_AZURE_CLIENT_ID}"
    export RBAC_AAD_TENANT_ID="${REACT_APP_AZURE_TENANT_ID}"
    export RBAC_API_AUDIENCE="${REACT_APP_AZURE_CLIENT_ID}"
    export RBAC_CONNECTION_STR="${CONNECTION_STR}"
    uvicorn main:app --host 0.0.0.0 --port $LISTENING_PORT
else
    echo "RBAC flag not configured or not equal to true, only launch registry app"
    if [ "x$PURVIEW_NAME" == "x" ]; then
        echo "Purview flag is not configured, run SQL registry"
        cd sql-registry
        uvicorn main:app --host 0.0.0.0 --port $LISTENING_PORT
    else
        echo "Purview flag is configured, run Purview registry"
        cd purview-registry
        uvicorn main:app --host 0.0.0.0 --port $LISTENING_PORT
    fi
fi
