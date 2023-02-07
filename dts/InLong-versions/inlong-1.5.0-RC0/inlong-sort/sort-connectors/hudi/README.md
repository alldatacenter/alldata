# Hudi Sort connector

## Metric reporter settings

```property
hoodie.metrics.on=true
hoodie.metrics.reporter.class=org.apache.inlong.sort.hudi.metric.InLongHudiAuditReporter
hoodie.metrics.reporter.metricsname.prefix={custom metric name prefix}
hoodie.metrics.inlonghudi.report.period.seconds=30
inlong.metric.labels={inlong metric label}
metrics.audit.proxy.hosts={inlong metric hosts}
```

| property                                        | option   | default value                                         | docs                                                                                                        |
|-------------------------------------------------|----------|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| hoodie.metrics.on                               | required | false                                                 | must be 'true'                                                                                              |
| hoodie.metrics.reporter.class                   | required | org.apache.inlong.sort.hudi.metric.InLongHudiAuditReporter | must be 'org.apache.inlong.sort.hudi.metric.InLongHudiAuditReporter'                                             |
| hoodie.metrics.reporter.metricsname.prefix      | option   | -                                                     | The prefix given to the metrics names.                                                                      |
| hoodie.metrics.inlonghudi.report.period.seconds | required | 30                                                    | InLongHudi reporting period in seconds. Default to 30.                                                      |
| inlong.metric.labels                            | required | -                                                     | INLONG metric labels, format is 'key1=value1&key2=value2', default is 'groupId=xxx&streamId=xxx&nodeId=xxx' |
| metrics.audit.proxy.hosts                       | required | -                                                     | Audit proxy host address for reporting audit metrics. e.g. 127.0.0.1:10081,0.0.0.1:10081                    | 

