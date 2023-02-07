/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.image;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.store.image.ImageBatchReader.ColumnDefn;
import org.apache.drill.exec.store.image.ImageBatchReader.ListColumnDefn;
import org.apache.drill.exec.store.image.ImageBatchReader.MapColumnDefn;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ColumnWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.slf4j.LoggerFactory;

import com.adobe.internal.xmp.XMPException;
import com.adobe.internal.xmp.XMPIterator;
import com.adobe.internal.xmp.XMPMeta;
import com.adobe.internal.xmp.options.IteratorOptions;
import com.adobe.internal.xmp.properties.XMPPropertyInfo;
import com.drew.lang.KeyValuePair;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.StringValue;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * Although each image format can contain different metadata,
 * they also have common basic information. The class handles
 * basic metadata as well as complex tags.
 * @see org.apache.drill.exec.store.image.GenericMetadataDirectory
 * @see com.drew.metadata.Directory
 */
public class ImageDirectoryProcessor {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImageDirectoryProcessor.class);

  protected static void processGenericMetadataDirectory(final GenericMetadataDirectory directory,
                                                        final LinkedHashMap<String, ColumnDefn> genericColumns,
                                                        final ImageFormatConfig config) {
    for (Tag tag : directory.getTags()) {
      final int tagType = tag.getTagType();
      if (!config.hasFileSystemMetadata() && ImageMetadataUtils.isSkipTag(tag.getTagName())) {
        continue;
      }
      genericColumns.get(ImageMetadataUtils.formatName(tag.getTagName())).load(config.isDescriptive()
          ? directory.getDescription(tagType)
          : directory.getObject(tagType));
    }
  }

  protected static void processXmpDirectory(final MapColumnDefn writer, final XmpDirectory directory) {
    XMPMeta xmpMeta = directory.getXMPMeta();
    if (xmpMeta != null) {
      try {
        IteratorOptions iteratorOptions = new IteratorOptions().setJustLeafnodes(true);
        for (final XMPIterator i = xmpMeta.iterator(iteratorOptions); i.hasNext();) {
          try {
            XMPPropertyInfo prop = (XMPPropertyInfo) i.next();
            String path = prop.getPath();
            String value = prop.getValue();
            if (path != null && value != null) {
              // handling lang-alt array items
              if (prop.getOptions().getHasLanguage()) {
                XMPPropertyInfo langProp = (XMPPropertyInfo) i.next();
                if (langProp.getPath().endsWith("/xml:lang")) {
                  String lang = langProp.getValue();
                  path = path.replaceFirst("\\[\\d+\\]$", "") +
                      (lang.equals("x-default") ? "" : "_" + lang);
                }
              }
              ColumnDefn rootColumn = writer;
              ColumnWriter subColumn = null;
              String[] elements = path.replaceAll("/\\w+:", "/").split(":|/|(?=\\[)");
              // 1. lookup and create nested structure
              for (int j = 1; j < elements.length; j++) {
                String parent = elements[j - 1];
                boolean isList = elements[j].startsWith("[");
                if (!parent.startsWith("[")) { // skipped. such as parent is [1] but not the last element
                  final String formatName = ImageMetadataUtils.formatName(parent);
                  if (isList) {
                    if (j + 1 == elements.length) { // for list
                      subColumn = rootColumn.addList(formatName);
                    } else { // for list-map
                      subColumn = rootColumn.addListMap(formatName);
                    }
                    rootColumn = new ListColumnDefn(formatName).builder((ArrayWriter) subColumn);
                  } else { // for map
                    subColumn = ((MapColumnDefn) rootColumn).addMap(formatName);
                    // set up the current writer in nested structure
                    rootColumn = new MapColumnDefn(formatName).builder((TupleWriter) subColumn);
                  }
                }
              }
              // 2. set up the value for writer
              String parent = elements[elements.length - 1];
              if (parent.startsWith("[")) {
                subColumn.setObject(new String[] { value });
              } else {
                rootColumn.addText(ImageMetadataUtils.formatName(parent)).setString(value);
                if (subColumn instanceof ArrayWriter) {
                  ((ArrayWriter) subColumn).save();
                }
              }
            }
          } catch (Exception skipped) { // simply skip this property
            logger.warn("Error in written xmp metadata : {}", skipped.getMessage());
          }
        }
      } catch (XMPException ignored) {
        logger.warn("Error in processing xmp directory : {}", ignored.getMessage());
      }
    }
  }

  protected static void processDirectory(final MapColumnDefn writer,
                                         final Directory directory,
                                         final Metadata metadata,
                                         final ImageFormatConfig config) {
    TimeZone timeZone = (config.getTimeZone() != null)
        ? TimeZone.getTimeZone(config.getTimeZone())
        : TimeZone.getDefault();
    for (Tag tag : directory.getTags()) {
      try {
        final int tagType = tag.getTagType();
        Object value;
        if (config.isDescriptive() || ImageMetadataUtils.isDescriptionTag(directory, tagType)) {
          value = directory.getDescription(tagType);
          if (directory instanceof PngDirectory) {
            if (((PngDirectory) directory).getPngChunkType().areMultipleAllowed()) {
              value = new String[] { (String) value };
            }
          }
        } else {
          value = directory.getObject(tagType);
          if (directory instanceof ExifIFD0Directory && tagType == ExifIFD0Directory.TAG_DATETIME) {
            ExifSubIFDDirectory exifSubIFDDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            String subsecond = null;
            if (exifSubIFDDir != null) {
              subsecond = exifSubIFDDir.getString(ExifSubIFDDirectory.TAG_SUBSECOND_TIME);
            }
            value = directory.getDate(tagType, subsecond, timeZone);
          } else if (directory instanceof ExifSubIFDDirectory) {
            if (tagType == ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL) {
              value = ((ExifSubIFDDirectory) directory).getDateOriginal(timeZone);
            } else if (tagType == ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED) {
              value = ((ExifSubIFDDirectory) directory).getDateDigitized(timeZone);
            }
          } else if (directory instanceof GpsDirectory) {
            if (tagType == GpsDirectory.TAG_LATITUDE) {
              value = ((GpsDirectory) directory).getGeoLocation().getLatitude();
            } else if (tagType == GpsDirectory.TAG_LONGITUDE) {
              value = ((GpsDirectory) directory).getGeoLocation().getLongitude();
            }
          }
          if (ImageMetadataUtils.isVersionTag(directory, tagType)) {
            value = directory.getString(tagType, "US-ASCII");
          } else if (ImageMetadataUtils.isDateTag(directory, tagType)) {
            value = directory.getDate(tagType, timeZone);
          }
        }
        processValue(writer, ImageMetadataUtils.formatName(tag.getTagName()), value);
      } catch (Exception skipped) {
        logger.warn("Error in processing image directory : {}", skipped.getMessage());
      }
    }
  }

  /**
   * Convert the value if necessary
   * @see org.apache.drill.exec.vector.accessor.writer.AbstractScalarWriter#setObject(Object)
   * @param writer MapColumnDefn
   * @param name Tag Name
   * @param value  Tag Value
   */
  protected static void processValue(final MapColumnDefn writer, final String name, final Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof Boolean) {
      writer.addObject(name, MinorType.BIT).setObject(value);
    } else if (value instanceof Byte) {
      writer.addObject(name, MinorType.TINYINT).setObject(value);
    } else if (value instanceof Short) {
      writer.addObject(name, MinorType.SMALLINT).setObject(value);
    } else if (value instanceof Integer) {
      writer.addObject(name, MinorType.INT).setObject(value);
    } else if (value instanceof Long) {
      writer.addObject(name, MinorType.BIGINT).setObject(value);
    } else if (value instanceof Float) {
      writer.addObject(name, MinorType.FLOAT4).setObject(value);
    } else if (value instanceof Double) {
      writer.addObject(name, MinorType.FLOAT8).setObject(value);
    } else if (value instanceof Rational) {
      writer.addDouble(name).setDouble(((Rational) value).doubleValue());
    } else if (value instanceof StringValue) {
      writer.addText(name).setString(((StringValue) value).toString());
    } else if (value instanceof Date) {
      writer.addDate(name).setTimestamp(Instant.ofEpochMilli(((Date) value).getTime()));
    } else if (value instanceof String[]) {
      writer.addList(name).setObject(value);
    } else if (value instanceof byte[]) {
      writer.addListByte(name).setObject(value);
    } else if (value instanceof JpegComponent) {
      JpegComponent v = (JpegComponent) value;
      TupleWriter component = writer.addMap(name);
      writer.addIntToMap(component, TagName.JPEGCOMPONENT_CID).setInt(v.getComponentId());
      writer.addIntToMap(component, TagName.JPEGCOMPONENT_HSF).setInt(v.getHorizontalSamplingFactor());
      writer.addIntToMap(component, TagName.JPEGCOMPONENT_VSF).setInt(v.getVerticalSamplingFactor());
      writer.addIntToMap(component, TagName.JPEGCOMPONENT_QTN).setInt(v.getQuantizationTableNumber());
    } else if (value instanceof List<?>) {
      ArrayWriter listMap = writer.addListMap(name);
      ListColumnDefn list = new ListColumnDefn(name).builder(listMap);
      for (Object v : (List<?>) value) {
        if (v instanceof KeyValuePair) {
          list.addText(TagName.KEYVALUEPAIR_K).setString(((KeyValuePair) v).getKey());
          list.addText(TagName.KEYVALUEPAIR_V).setString(((KeyValuePair) v).getValue().toString());
        } else {
          list.addText(TagName.KEYVALUEPAIR_V).setString(v.toString());
        }
        listMap.save();
      }
    } else {
      writer.addText(name).setString(value.toString());
    }
  }

  private static class TagName {
    public static final String JPEGCOMPONENT_CID = "ComponentId";
    public static final String JPEGCOMPONENT_HSF = "HorizontalSamplingFactor";
    public static final String JPEGCOMPONENT_VSF = "VerticalSamplingFactor";
    public static final String JPEGCOMPONENT_QTN = "QuantizationTableNumber";
    public static final String KEYVALUEPAIR_K = "Key";
    public static final String KEYVALUEPAIR_V = "Value";
  }
}