# Openshift Debezium UI Demo Setup

## Login

You maybe need to update the api endpoint for your Openshift cluster and
provide a valid username.

In order to add Strimzi Operator the user needs to have right to add
CustomResourceDefinition objects.

```console
oc login https://api.cluster-305d.305d.example.opentlc.com:6443 -u <CLUSTER-USER-NAME>
```

## Setup Project `dbz-ui-demo`

```console
oc new-project dbz-ui-demo --display-name="Debezium UI Demo"
oc project dbz-ui-demo
```

## Install Strimzi Operator

```console
oc apply -f 00-strimzi-base.yaml -n dbz-ui-demo
```

## Create Kafka Cluster

```console
oc apply -f 02-kafka.yaml -n dbz-ui-demo
```

## Create Postgres Database

```console
oc apply -f 03-postgres.yaml -n dbz-ui-demo
```

## Create Kafka Connect Cluster

```console
oc apply -f 04-kafka-connect.yaml -n dbz-ui-demo
```

## Create UI Service

Update the cluster addresses for the frontend routes in `05-ui.yaml`!

```console
oc apply -f 05-ui.yaml -n dbz-ui-demo
```
