# OpenShift

This example deploy Kibana 7.10.2 on [OpenShift][] using [custom values][].

## Usage

* Deploy [Elasticsearch Helm chart][].

* Deploy Kibana chart with the default values: `make install`

* You can now setup a port forward to query Elasticsearch API:

  ```
  kubectl port-forward svc/elasticsearch-master 9200
  curl localhost:9200/_cat/indices
  ```

## Testing

You can also run [goss integration tests][] using `make test`


[custom values]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/examples/openshift/values.yaml
[elasticsearch helm chart]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/examples/openshift/
[goss integration tests]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/examples/openshift/test/goss.yaml
[openshift]: https://www.openshift.com/
