# MicroK8S

An example of configuration for deploying Elasticsearch chart on [MicroK8S][].

Note that this configuration should be used for test only and isn't recommended
for production.

## Requirements

The following MicroK8S [addons][] need to be enabled:
- `dns`
- `helm`
- `storage`

[MicroK8S]: https://microk8s.io
[Addons]: https://microk8s.io/docs/addons