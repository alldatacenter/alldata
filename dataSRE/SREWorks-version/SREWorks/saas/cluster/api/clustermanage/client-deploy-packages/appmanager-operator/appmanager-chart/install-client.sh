helm upgrade sreworks-client ./ \
    --create-namespace --namespace sreworks-client \
    --set installMode=client \
    --install
