## Arctic Site

This directory contains the source for the Arctic site.

* Site structure is maintained in mkdocs.yml
* Pages are maintained in markdown in the `docs/` folder
* Links use bare page names: `[link text](target-page)`

### Style
* Proper nouns should start with a capital letter, like Hadoop、Hive、Iceberg、Arctic
* A space is required at both ends of mixed English and Chinese

### Installation

The site is built using mkdocs. To install mkdocs and the theme, run:

```
pip install mkdocs
pip install mkdocs-cinder
pip install mkdocs-material 
pip install pymdown-extensions
```

### Local Changes

To see changes locally before committing, use mkdocs to run a local server from this directory.

```
mkdocs serve
```

### Publishing

After site changes are committed, you can publish the site with this command:

```
mkdocs gh-deploy
```

It will push to the `gh-pages` branch, and published by GitHub Pages. 