# Ververica Platform Helm Chart

This is the official Helm Chart of Ververica Platform, the enterprise stream processing platform by
the original creators of Apache Flink.


## Quick Start

### Community Edition

```
$ helm install vvp ververica/ververica-platform --acceptCommunityEditionLicense=true
```

Please read the Community Edition License Agreement carefully before accepting it. It is printed
during installation if you do not set ``acceptCommunityEditionLicense=true``.


### Enterprise Editions

```
$ helm install vvp ververica/ververica-platform --values values-license.yaml --vvp.persistence.type=local
```

We are happy to provide you with a free 30 day trial license for the Ververica Platform Stream
Edition. Just reach out to us via [https://www.ververica.com/enterprise-trial](
https://www.ververica.com/enterprise-trial).


## References

* [Getting Started Guide](https://docs.ververica.com/getting_started/index.html)
* [Release Notes](https://docs.ververica.com/release_notes/index.html)
* [Documentation](https://docs.ververica.com/)
* [Knowledge Base](https://ververica.zendesk.com/hc/en-us)


## Feedback & Questions

Please do not hesitate to reach out to us at [community-edition@ververica.com](
mailto:community-edition@ververica.com) with any questions or feedback you might have or if you run
into any issues using this chart or Ververica Platform.
