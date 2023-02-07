# Drill ElasticSearch Plugin

Drill ElasticSearch storage plugin allows you to perform SQL queries against ElasticSearch indices.
This storage plugin implementation is based on [Apache Calcite adapter for ElasticSearch](https://calcite.apache.org/docs/elasticsearch_adapter.html).

For more details about supported versions please refer to [Supported versions](https://calcite.apache.org/docs/elasticsearch_adapter.html#supported-versions) page.

### Supported optimizations and features

This storage plugin supports the following optimizations:

- Project pushdown
- Filter pushdown (only expressions supported by Calcite adapter for ElasticSearch. Filter with unsupported expressions 
  wouldn't be pushed to ElasticSearch but will be produced by Drill)
- Limit pushdown
- Aggregation pushdown
- Sort pushdown

Besides these optimizations, ElasticSearch storage plugin supports the schema provisioning feature.
For more details please refer to [Specifying the Schema as Table Function Parameter](https://drill.apache.org/docs/plugin-configuration-basics/#specifying-the-schema-as-table-function-parameter).

### Plugin registration

The plugin can be registered in Apache Drill using the drill web interface by navigating to the `storage` page.
Following is the default registration configuration.

```json
{
  "type": "elastic",
  "hosts": [
    "http://localhost:9200"
  ],
  "username": null,
  "password": null,
  "enabled": false
}
```

### Developer notes

Most of the common classes required for creating storage plugins based on Calcite adapters are placed in the 
`java-exec` module, so they can be reused in future plugin implementations.

Here is the list of the classes that may be useful:

- `VertexDrelConverterRule` with `VertexDrel` - used to hold plugin-specific part of the plan at the end of the 
  `LOGICAL` planning phase.
- `EnumerableIntermediatePrelConverterRule` with `EnumerableIntermediatePrel` - the same as above, but for the 
  `PHYSICAL` planning phase.
- `EnumerablePrel` - responsible for generating java code that will be executed to query the storage plugin data source.
- `EnumerableRecordReader` - executes java code generated in `EnumerablePrel` and transforms obtained results to Drill internal representation.
