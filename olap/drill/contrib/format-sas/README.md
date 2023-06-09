# Format Plugin for SAS Files
This format plugin enables Drill to read SAS files. (sas7bdat)

## Data Types
The SAS format supports the `VARCHAR`, `LONG`, `DOUBLE`, `TIME` and `DATE` types.

## Implicit Fields (Metadata)
The SAS reader provides the following file metadata fields:
* `_compression_method`
* `_encoding`
* `_file_label`
* `_file_type`
* `_os_name`
* `_os_type`
* `_sas_release`
* `_session_encoding`
* `_date_created`
* `_date_modified`

## Schema Provisioning
Drill will infer the schema of your data.  

## Configuration Options 
This function has no configuration options other than the file extension.

```json
  "sas": {
  "type": "sas",
  "extensions": [
    "sas7bdat"
  ]
}
```
This plugin is enabled by default. 
