# Security

This example deploy Kibana 7.10.2 using authentication and TLS to connect to
Elasticsearch (see [values][]).


## Usage

* Deploy [Elasticsearch Helm chart][].

* Deploy Kibana chart with security: `make install`

* You can now setup a port forward to query Kibana indices:

  ```
  kubectl port-forward svc/security-master 9200
  curl -u elastic:changeme https://localhost:9200/_cat/indices
  ```


## Testing

You can also run [goss integration tests][] using `make test`


[elasticsearch helm chart]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/examples/security/
[goss integration tests]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples/security/test/goss.yaml
[values]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples/security/values.yaml
