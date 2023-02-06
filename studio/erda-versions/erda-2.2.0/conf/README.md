# conf

Put Erda's common config here.

Each cmd has a Symbolic link called `common-conf` refer to this directory.

## Usage

See `cmd/erda-server/common-conf` for more details.

An bootstrap.yaml demo:

```yaml
i18n:
  common:
    - common-conf/i18n.yaml # this i18n.yaml locates in common-conf
  files:
    - conf/i18n.yaml # this i18n.yaml locates in erda-server's own conf dir
```
