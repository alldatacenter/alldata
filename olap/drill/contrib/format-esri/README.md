# Format Plugin for ESRI Shape Files
This format plugin allows Drill to read ESRI Shape files. You can read about the shapefile format here: https://en.wikipedia.org/wiki/Shapefile. 

## Configuration Options
Other than the file extensions, there are no configuration options for this plugin. To use, simply add the following to your configuration:

```
    "shp": {
      "type": "shp",
      "extensions": [
        "shp"
      ]
    }
```

## Usage Notes:
This plugin will return the following fields:

* `gid`: Integer
* `srid`: Integer
* `shapeType`: String
* `name`: Plain text 
* `geom`: A geometric point or path. This field is returned as a `VARBINARY`.

This plugin is best used with the suite of GIS functions in Drill which include the following:

<h1>Geospatial Functions</h1>

<p>As of version 1.14, Drill contains a suite of<a contenteditable="false" data-primary="geographic information systems (GIS) functionality" data-secondary="reference of geo-spatial functions in Drill" data-type="indexterm">&nbsp;</a> geographic information system (GIS) functions. Most of the functionality follows that of PostGIS.<a contenteditable="false" data-primary="Well-Known Text (WKT) representation format for spatial data" data-type="indexterm">&nbsp;</a> To use these functions, your spatial data must be defined in the <a href="https://en.wikipedia.org/wiki/Well-known_text">Well-Known Text (WKT) representation format</a>.<a contenteditable="false" data-primary="ST_AsText function" data-type="indexterm">&nbsp;</a><a contenteditable="false" data-primary="ST_GeoFromText function" data-type="indexterm">&nbsp;</a> The WKT format allows you to represent points, lines, polygons, and other geometric shapes. Following are two example WKT strings:</p>

<pre data-type="programlisting">
POINT (30 10)
POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))</pre>

<p>Drill stores points as binary, so to read or plot these points, you need to convert them using the <code>ST_AsText()</code> and<code> ST_GeoFromText()</code> functions. <a data-type="xref" href="#table0a05">#table0a05</a> lists the GIS functions.</p>
