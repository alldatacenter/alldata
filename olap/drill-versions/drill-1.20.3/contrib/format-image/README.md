## Overview

Drill can query the metadata in various image formats. The metadata format plugin is useful for querying a large number of image files stored in a distributed file system. You do not have to build a metadata repository in advance.

## Attributes

The following table lists configuration attributes:  

Attribute|Default Value|Description
---------|-------------|-----------
fileSystemMetadata|true|Set to true to extract filesystem metadata including the file size and the last modified timestamp, false otherwise. 
descriptive|true|Set to true to extract metadata in a human-readable string format. Set false to extract metadata in a machine-readable typed format.
timeZone|null|Specify the time zone to interpret the timestamp with no time zone information. If the timestamp includes the time zone information, this value is ignored. If null is set, the local time zone is used.

## More
All of the Drill documents are placed in `gh-pages` branch on Github. For more detail about image plugin usage, please see also : `gh-pages/_docs/data-sources-and-file-formats/111-image-metadata-format-plugin.md`