# README

This directory has the basic configuration files of ByConity services which could be used in the Debian package for Debian OS and rpm packages for Centos OS.

## Package deployment

One way to deploy ByConity to physical machines is using package manager. For example, install Debian package for Debian OS and rpm packages for Centos OS

ByConity using FoundationDB as meta store, and HDFS as datastore. So before starting to deploy ByConity, we need to deploy FoundationDB and HDFS first.

* First you need to [installthe FoundationDB and HDFS](https://byconity.github.io/docs/deployment/package-deployment#install-foundationdb-and-hdfs).

* Second [installing the FoundationDB client](https://byconity.github.io/docs/deployment/package-deployment#install-foundationdb-client).

* Next deploying ByConity packages, you can find them in release [page](https://github.com/ByConity/ByConity/releases). Or you can build the package by yourself, in that case follow this [guide](https://github.com/ByConity/ByConity/tree/master/docker/packager).

* Please checkout the detail deployment instruction [here](https://byconity.github.io/docs/deployment/package-deployment#deploy-byconity-packages).
