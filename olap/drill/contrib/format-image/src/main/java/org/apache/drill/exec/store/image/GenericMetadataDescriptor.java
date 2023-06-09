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

import static org.apache.drill.exec.store.image.GenericMetadataDirectory.TAG_DURATION;
import static org.apache.drill.exec.store.image.GenericMetadataDirectory.TAG_FILE_SIZE;
import static org.apache.drill.exec.store.image.GenericMetadataDirectory.TAG_ORIENTATION;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.TagDescriptor;

public class GenericMetadataDescriptor extends TagDescriptor<GenericMetadataDirectory>
{
  public GenericMetadataDescriptor(@NotNull GenericMetadataDirectory directory)
  {
    super(directory);
  }

  @Override
  @Nullable
  public String getDescription(int tagType)
  {
    switch (tagType) {
      case TAG_FILE_SIZE:
        return getFileSizeDescription();
      case TAG_ORIENTATION:
        return getOrientationDescription();
      case TAG_DURATION:
        return getDurationDescription();
      default:
        return super.getDescription(tagType);
    }
  }

  @Nullable
  private String getFileSizeDescription()
  {
    Long size = _directory.getLongObject(TAG_FILE_SIZE);

    if (size == null) {
      return null;
    }
    return Long.toString(size) + " bytes";
  }

  @Nullable
  private String getOrientationDescription() {
    return getIndexedDescription(TAG_ORIENTATION, 1,
        "Top, left side (Horizontal / normal)",
        "Top, right side (Mirror horizontal)",
        "Bottom, right side (Rotate 180)",
        "Bottom, left side (Mirror vertical)",
        "Left side, top (Mirror horizontal and rotate 270 CW)",
        "Right side, top (Rotate 90 CW)",
        "Right side, bottom (Mirror horizontal and rotate 90 CW)",
        "Left side, bottom (Rotate 270 CW)");
  }

  @Nullable
  private String getDurationDescription() {
    Long value = _directory.getLongObject(TAG_DURATION);
    if (value == null) {
      return null;
    }

    Integer hours = (int)(value / (Math.pow(60, 2)));
    Integer minutes = (int)((value / (Math.pow(60, 1))) - (hours * 60));
    Integer seconds = (int)Math.ceil((value / (Math.pow(60, 0))) - (minutes * 60));
    return String.format("%1$02d:%2$02d:%3$02d", hours, minutes, seconds);
  }
}
