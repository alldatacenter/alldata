The documents will tell how to config the `service discovery` config file `cnch_config.xml` and the FoundationDB cluster file `fdb.config`

## Config `cnch_config.xml`
The `cnch_config.xml` file contains 2 parts that have to be configured by user,  the `service_discovery` tag and the `hdfs_nnproxy` tags.
In ByConity there are 3 ways component can discover each other. The `mode` tag is used to specify the way. There are 3 modes: `local`, `dns` and `consul`
In the `local` mode, the user have to specify the ip address or host name of all components in this config file, by replace the place holder `{your_xxx_address}`, for example `{your_server_address}`, which the actually ip address of the component, for example, `10.0.2.72`. 

For the `hdfs_nnproxy` tags contains the address of HDFS name node 

## Config `fdb.config`
The `fdb.config` is the file for foundation db client to connect to FoundationDB server, you will have it after you config FoundationDB server. Read more in [here](https://github.com/ByConity/ByConity/tree/master/docker/executable_wrapper/FDB_installation.md)
