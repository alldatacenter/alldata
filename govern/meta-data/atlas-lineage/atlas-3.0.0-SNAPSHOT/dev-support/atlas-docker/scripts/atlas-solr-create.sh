#!/bin/bash

# Create Solr collections used by Atlas
precreate-core vertex_index   /opt/solr/server/solr/configsets/atlas/
precreate-core edge_index     /opt/solr/server/solr/configsets/atlas/
precreate-core fulltext_index /opt/solr/server/solr/configsets/atlas/
