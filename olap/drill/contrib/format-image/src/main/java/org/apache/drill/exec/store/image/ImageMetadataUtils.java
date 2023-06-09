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

import java.util.HashMap;

import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.PanasonicRawIFD0Directory;
import com.drew.metadata.exif.makernotes.FujifilmMakernoteDirectory;
import com.drew.metadata.exif.makernotes.NikonType2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusCameraSettingsMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusEquipmentMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusFocusInfoMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusImageProcessingMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawDevelopment2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawDevelopmentMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawInfoMakernoteDirectory;
import com.drew.metadata.exif.makernotes.PanasonicMakernoteDirectory;
import com.drew.metadata.exif.makernotes.SamsungType2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.SonyType6MakernoteDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.photoshop.PhotoshopDirectory;
import com.drew.metadata.png.PngDirectory;

public class ImageMetadataUtils {

  public static boolean isVarchar(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // Format,Color Mode,Video Codec,Audio Codec
    return name.equals(tags.get(GenericMetadataDirectory.TAG_FORMAT))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_COLOR_MODE))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_VIDEO_CODEC))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_AUDIO_CODEC));
  }

  public static boolean isInt(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // Pixel Width,Pixel Height,Orientation,Bits Per Pixel,Audio Sample Size
    return name.equals(tags.get(GenericMetadataDirectory.TAG_PIXEL_WIDTH))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_PIXEL_HEIGHT))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_ORIENTATION))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_BITS_PER_PIXEL))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_AUDIO_SAMPLE_SIZE));
  }

  public static boolean isLong(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // File Size,Duration
    return name.equals(tags.get(GenericMetadataDirectory.TAG_FILE_SIZE))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_DURATION));
  }

  public static boolean isDouble(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // DPI Width,DPI Height,Frame Rate,Audio Sample Rate
    return name.equals(tags.get(GenericMetadataDirectory.TAG_DPI_WIDTH))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_DPI_HEIGHT))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_FRAME_RATE))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_AUDIO_SAMPLE_RATE));
  }

  public static boolean isBoolean(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // Has Alpha
    return name.equals(tags.get(GenericMetadataDirectory.TAG_HAS_ALPHA));
  }

  public static boolean isDate(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    // File Date Time
    return name.equals(tags.get(GenericMetadataDirectory.TAG_FILE_DATE_TIME));
  }

  /**
   * Format the tag name (remove the spaces and special characters)
   * @param tagName
   * @return
   */
  public static String formatName(final String tagName) {
    StringBuilder builder = new StringBuilder();
    boolean upperCase = true;
    for (char c : tagName.toCharArray()) {
      if (c == ' ' || c == '-' || c == '/') {
        upperCase = true;
      } else {
        builder.append(upperCase ? Character.toUpperCase(c) : c);
        upperCase = false;
      }
    }
    return builder.toString();
  }

  /**
   * Skip the tag if parameter (fileSystemMetadata) is false
   * @param name
   * @return
   */
  public static boolean isSkipTag(String name) {
    HashMap<Integer, String> tags = GenericMetadataDirectory._tagNameMap;
    return name.equals(tags.get(GenericMetadataDirectory.TAG_FILE_SIZE))
        || name.equals(tags.get(GenericMetadataDirectory.TAG_FILE_DATE_TIME));
  }

  public static boolean isDescriptionTag(final Directory directory, final int tagType) {
    return directory instanceof IccDirectory
        && tagType > 0x20202020
        && tagType < 0x7a7a7a7a
        || directory instanceof PhotoshopDirectory;
  }

  public static boolean isVersionTag(final Directory directory, final int tagType) {
    return directory instanceof ExifSubIFDDirectory &&
        (tagType == ExifSubIFDDirectory.TAG_EXIF_VERSION || tagType == ExifSubIFDDirectory.TAG_FLASHPIX_VERSION) ||
        directory instanceof ExifInteropDirectory &&
        tagType == ExifInteropDirectory.TAG_INTEROP_VERSION ||
        directory instanceof FujifilmMakernoteDirectory &&
        tagType == FujifilmMakernoteDirectory.TAG_MAKERNOTE_VERSION ||
        directory instanceof NikonType2MakernoteDirectory &&
        tagType == NikonType2MakernoteDirectory.TAG_FIRMWARE_VERSION ||
        directory instanceof OlympusCameraSettingsMakernoteDirectory &&
        tagType == OlympusCameraSettingsMakernoteDirectory.TagCameraSettingsVersion ||
        directory instanceof OlympusEquipmentMakernoteDirectory &&
        tagType == OlympusEquipmentMakernoteDirectory.TAG_EQUIPMENT_VERSION ||
        directory instanceof OlympusFocusInfoMakernoteDirectory &&
        tagType == OlympusFocusInfoMakernoteDirectory.TagFocusInfoVersion ||
        directory instanceof OlympusImageProcessingMakernoteDirectory &&
        tagType == OlympusImageProcessingMakernoteDirectory.TagImageProcessingVersion ||
        directory instanceof OlympusMakernoteDirectory &&
        tagType == OlympusMakernoteDirectory.TAG_MAKERNOTE_VERSION ||
        directory instanceof OlympusRawDevelopment2MakernoteDirectory &&
        tagType == OlympusRawDevelopment2MakernoteDirectory.TagRawDevVersion ||
        directory instanceof OlympusRawDevelopmentMakernoteDirectory &&
        tagType == OlympusRawDevelopmentMakernoteDirectory.TagRawDevVersion ||
        directory instanceof OlympusRawInfoMakernoteDirectory &&
        tagType == OlympusRawInfoMakernoteDirectory.TagRawInfoVersion ||
        directory instanceof PanasonicMakernoteDirectory &&
        (tagType == PanasonicMakernoteDirectory.TAG_FIRMWARE_VERSION
         || tagType == PanasonicMakernoteDirectory.TAG_MAKERNOTE_VERSION
         || tagType == PanasonicMakernoteDirectory.TAG_EXIF_VERSION) ||
        directory instanceof SamsungType2MakernoteDirectory &&
        tagType == SamsungType2MakernoteDirectory.TagMakerNoteVersion ||
        directory instanceof SonyType6MakernoteDirectory &&
        tagType == SonyType6MakernoteDirectory.TAG_MAKERNOTE_THUMB_VERSION ||
        directory instanceof PanasonicRawIFD0Directory &&
        tagType == PanasonicRawIFD0Directory.TagPanasonicRawVersion;
  }

  public static boolean isDateTag(final Directory directory, final int tagType) {
    return directory instanceof IccDirectory && tagType == IccDirectory.TAG_PROFILE_DATETIME
             || directory instanceof PngDirectory && tagType == PngDirectory.TAG_LAST_MODIFICATION_TIME;
  }
}