## jobManagerAddress

The JobManager is serving the web interface accessible at localhost:8081

## clusterId

Using [native_kubernetes](https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/deployment/resource-providers/native_kubernetes/), when launch the session,
execute the shell script:

```shell script
./bin/kubernetes-session.sh -Dkubernetes.cluster-id=my-first-flink-cluster
```

the control of input textbox shall input the value what system `-Dkubernetes.cluster-id` setted
