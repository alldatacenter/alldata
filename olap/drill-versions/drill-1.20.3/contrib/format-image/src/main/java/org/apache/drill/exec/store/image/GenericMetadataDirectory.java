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

import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Directory;
import com.drew.metadata.MetadataException;

public class GenericMetadataDirectory extends Directory
{
  public static final int TAG_FILE_SIZE = 1;
  public static final int TAG_FILE_DATE_TIME = 2;
  public static final int TAG_FORMAT = 3;
  public static final int TAG_PIXEL_WIDTH = 4;
  public static final int TAG_PIXEL_HEIGHT = 5;
  public static final int TAG_ORIENTATION = 6;
  public static final int TAG_DPI_WIDTH = 7;
  public static final int TAG_DPI_HEIGHT = 8;
  public static final int TAG_COLOR_MODE = 9;
  public static final int TAG_BITS_PER_PIXEL = 10;
  public static final int TAG_HAS_ALPHA = 11;
  public static final int TAG_DURATION = 12;
  public static final int TAG_VIDEO_CODEC = 13;
  public static final int TAG_FRAME_RATE = 14;
  public static final int TAG_AUDIO_CODEC = 15;
  public static final int TAG_AUDIO_SAMPLE_SIZE = 16;
  public static final int TAG_AUDIO_SAMPLE_RATE = 17;

  @NotNull
  protected static final HashMap<Integer, String> _tagNameMap = new HashMap<>();

  static {
    _tagNameMap.put(TAG_FILE_SIZE, "File Size");
    _tagNameMap.put(TAG_FILE_DATE_TIME, "File Date Time");
    _tagNameMap.put(TAG_FORMAT, "Format");
    _tagNameMap.put(TAG_PIXEL_WIDTH, "Pixel Width");
    _tagNameMap.put(TAG_PIXEL_HEIGHT, "Pixel Height");
    _tagNameMap.put(TAG_ORIENTATION, "Orientation");
    _tagNameMap.put(TAG_DPI_WIDTH, "DPI Width");
    _tagNameMap.put(TAG_DPI_HEIGHT, "DPI Height");
    _tagNameMap.put(TAG_COLOR_MODE, "Color Mode");
    _tagNameMap.put(TAG_BITS_PER_PIXEL, "Bits Per Pixel");
    _tagNameMap.put(TAG_HAS_ALPHA, "Has Alpha");
    _tagNameMap.put(TAG_DURATION, "Duration");
    _tagNameMap.put(TAG_VIDEO_CODEC, "Video Codec");
    _tagNameMap.put(TAG_FRAME_RATE, "Frame Rate");
    _tagNameMap.put(TAG_AUDIO_CODEC, "Audio Codec");
    _tagNameMap.put(TAG_AUDIO_SAMPLE_SIZE, "Audio Sample Size");
    _tagNameMap.put(TAG_AUDIO_SAMPLE_RATE, "Audio Sample Rate");
  }

  public GenericMetadataDirectory()
  {
    this.setDescriptor(new GenericMetadataDescriptor(this));
  }

  @Override
  @NotNull
  public String getName()
  {
    return "Generic Metadata";
  }

  @Override
  @NotNull
  protected HashMap<Integer, String> getTagNameMap()
  {
    return _tagNameMap;
  }

  private void setIntIfEmpty(int tagType, int value) {
    if (!containsTag(tagType)) {
      setInt(tagType, value);
    }
  }

  private void setLongIfEmpty(int tagType, long value) {
    if (!containsTag(tagType)) {
      setLong(tagType, value);
    }
  }

  private void setBooleanIfEmpty(int tagType, boolean value) {
    if (!containsTag(tagType)) {
      setBoolean(tagType, value);
    }
  }

  private void setDoubleIfEmpty(int tagType, double value) {
    if (!containsTag(tagType)) {
      setDouble(tagType, value);
    }
  }

  private void setStringIfEmpty(int tagType, @NotNull String value) {
    if (!containsTag(tagType)) {
      setString(tagType, value);
    }
  }

  public void setPixelWidth(int pixelWidth) {
    setIntIfEmpty(TAG_PIXEL_WIDTH, pixelWidth);
  }

  public void setPixelWidth(Directory directory, int tagType) {
    try {
      setPixelWidth(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setPixelHeight(int pixelHeight) {
    setIntIfEmpty(TAG_PIXEL_HEIGHT, pixelHeight);
  }

  public void setPixelHeight(Directory directory, int tagType) {
    try {
      setPixelHeight(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setOrientation(int orientation) {
    setIntIfEmpty(TAG_ORIENTATION, orientation);
  }

  public void setOrientation(Directory directory, int tagType) {
    try {
      setOrientation(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setDPIWidth(double dpiWidth) {
    setDoubleIfEmpty(TAG_DPI_WIDTH, dpiWidth);
  }

  public void setDPIWidth(Directory directory, int tagType) {
    try {
      setDPIWidth(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setDPIWidth(Directory directory, int tagType, double factor) {
    try {
      setDPIWidth(directory.getInt(tagType) * factor);
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setDPIHeight(double dpiHeight) {
    setDoubleIfEmpty(TAG_DPI_HEIGHT, dpiHeight);
  }

  public void setDPIHeight(Directory directory, int tagType) {
    try {
      setDPIHeight(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setDPIHeight(Directory directory, int tagType, double factor) {
    try {
      setDPIHeight(directory.getInt(tagType) * factor);
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setColorMode(String colorMode) {
    setStringIfEmpty(TAG_COLOR_MODE, colorMode);
  }

  public void setColorMode(Directory directory, int tagType) {
    String colorMode = directory.getDescription(tagType);
    if (colorMode != null) {
      setColorMode(colorMode);
    }
  }

  public void setBitPerPixel(int bitPerPixel) {
    setIntIfEmpty(TAG_BITS_PER_PIXEL, bitPerPixel);
  }

  public void setBitPerPixel(Directory directory, int tagType) {
    try {
      setBitPerPixel(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setBitPerPixel(Directory directory, int tagType1, int tagType2) {
    try {
      setBitPerPixel(directory.getInt(tagType1) * directory.getInt(tagType2));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setAlpha(boolean alpha) {
    setBooleanIfEmpty(TAG_HAS_ALPHA, alpha);
  }

  public void setAlpha(Directory directory, int tagType) {
    try {
      setAlpha(directory.getBoolean(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setDuration(long duration) {
    setLongIfEmpty(TAG_DURATION, duration);
  }

  public void setDuration(Directory directory, int tagType) {
    Object o = directory.getObject(tagType);
    if (o != null) {
      if (o instanceof String) {
        String[] time = ((String) o).split(":");
        setDuration(
          Long.parseLong(time[0]) * 3600 +
          Long.parseLong(time[1]) * 60 +
          Long.parseLong(time[2]));
      } else if (o instanceof Number) {
        setDuration(((Number) o).longValue());
      }
    }
  }

  public void setVideoCodec(String videoCodec) {
    setStringIfEmpty(TAG_VIDEO_CODEC, videoCodec);
  }

  public void setVideoCodec(Directory directory, int tagType) {
    String videoCodec = directory.getString(tagType);
    if (videoCodec != null) {
      setVideoCodec(videoCodec);
    }
  }

  public void setFrameRate(double frameRate) {
    setDoubleIfEmpty(TAG_FRAME_RATE, frameRate);
  }

  public void setFrameRate(Directory directory, int tagType) {
    try {
      setFrameRate(directory.getDouble(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setAudioCodec(String audioCodec) {
    setStringIfEmpty(TAG_AUDIO_CODEC, audioCodec);
  }

  public void setAudioCodec(Directory directory, int tagType) {
    String audioCodec = directory.getString(tagType);
    if (audioCodec != null) {
      setAudioCodec(audioCodec);
    }
  }

  public void setAudioSampleSize(int audioSampleSize) {
    setIntIfEmpty(TAG_AUDIO_SAMPLE_SIZE, audioSampleSize);
  }

  public void setAudioSampleSize(Directory directory, int tagType) {
    try {
      setAudioSampleSize(directory.getInt(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }

  public void setAudioSampleRate(double audioSampleRate) {
    setDoubleIfEmpty(TAG_AUDIO_SAMPLE_RATE, audioSampleRate);
  }

  public void setAudioSampleRate(Directory directory, int tagType) {
    try {
      setAudioSampleRate(directory.getDouble(tagType));
    } catch (MetadataException e) {
      // Nothing needs to be done
    }
  }
}
