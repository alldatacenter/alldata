# Upgrade

This example will deploy Kibana chart using an old chart version,
then upgrade it.


## Usage

* Add the Elastic Helm charts repo: `helm repo add elastic https://helm.elastic.co`

* Deploy [Elasticsearch Helm chart][]: `helm install elasticsearch elastic/elasticsearch`

* Deploy and upgrade Kibana chart with the default values: `make install`


## Testing

You can also run [goss integration tests][] using `make test`.


[goss integration tests]: https://github.com/elastic/helm-charts/tree/master/kibana/examples/upgrade/test/goss.yaml
