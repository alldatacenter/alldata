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

import java.util.Date;
import java.util.TimeZone;

import org.apache.hadoop.fs.FileStatus;

import com.drew.imaging.FileType;
import com.drew.imaging.png.PngChunkType;
import com.drew.imaging.png.PngColorType;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.eps.EpsDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.gif.GifControlDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.ico.IcoDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.media.QuickTimeSoundDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4SoundDirectory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.wav.WavDirectory;
import com.drew.metadata.webp.WebpDirectory;

public class GenericMetadataReader
{
  public void read(@NotNull FileType fileType, @NotNull FileStatus fileStatus, @NotNull Metadata metadata)
  {
    GenericMetadataDirectory directory = new GenericMetadataDirectory();
    boolean skipEPSPreview = false;

    directory.setLong(GenericMetadataDirectory.TAG_FILE_SIZE, fileStatus.getLen());
    // Add local time zone offset to store the last modified time as local time
    // just like TO_TIMESTAMP(UNIX_TIMESTAMP()) returns local time
    directory.setDate(GenericMetadataDirectory.TAG_FILE_DATE_TIME,
        new Date(fileStatus.getModificationTime() + TimeZone.getDefault().getRawOffset()));
    directory.setString(GenericMetadataDirectory.TAG_FORMAT, fileType.name().toUpperCase());

    for (Directory dir : metadata.getDirectories()) {

      if (dir instanceof JpegDirectory) {
        final JpegDirectory jpegDir = (JpegDirectory)dir;
        directory.setPixelWidth(jpegDir, JpegDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(jpegDir, JpegDirectory.TAG_IMAGE_HEIGHT);
        directory.setBitPerPixel(jpegDir, JpegDirectory.TAG_DATA_PRECISION, JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
        continue;
      }

      if (dir instanceof JfifDirectory) {
        final JfifDirectory jfifDir = (JfifDirectory)dir;
        try {
          final int unit = jfifDir.getResUnits();
          if (unit == 1 || unit == 2) {
            directory.setDPIWidth(jfifDir, JfifDirectory.TAG_RESX, unit == 1 ? 1.0 : 2.54);
            directory.setDPIHeight(jfifDir, JfifDirectory.TAG_RESY, unit == 1 ? 1.0 : 2.54);
          }
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        continue;
      }

      if (dir instanceof ExifIFD0Directory) {
        if (skipEPSPreview) {
          skipEPSPreview = false;
          continue;
        }

        final ExifIFD0Directory ifd0Dir = (ExifIFD0Directory)dir;
        directory.setPixelWidth(ifd0Dir, ExifIFD0Directory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(ifd0Dir, ExifIFD0Directory.TAG_IMAGE_HEIGHT);
        directory.setOrientation(ifd0Dir, ExifIFD0Directory.TAG_ORIENTATION);
        try {
          final int unit = ifd0Dir.getInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT);
          if (unit == 2 || unit == 3) {
            directory.setDPIWidth(ifd0Dir, ExifIFD0Directory.TAG_X_RESOLUTION, unit == 2 ? 1.0 : 2.54);
            directory.setDPIHeight(ifd0Dir, ExifIFD0Directory.TAG_Y_RESOLUTION, unit == 2 ? 1.0 : 2.54);
          }
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        int[] bitPerSample = ifd0Dir.getIntArray(ExifIFD0Directory.TAG_BITS_PER_SAMPLE);
        if (bitPerSample != null) {
          int bitsPerPixel = 0;
          for (int n : bitPerSample) {
            bitsPerPixel += n;
          }
          directory.setBitPerPixel(bitsPerPixel);
        }
        continue;
      }

      if (dir instanceof ExifSubIFDDirectory) {
        final ExifSubIFDDirectory subIFDDir = (ExifSubIFDDirectory)dir;
        directory.setPixelWidth(subIFDDir, ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
        directory.setPixelHeight(subIFDDir, ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
        continue;
      }

      if (dir instanceof PsdHeaderDirectory) {
        final PsdHeaderDirectory psdDir = (PsdHeaderDirectory)dir;
        directory.setPixelWidth(psdDir, PsdHeaderDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(psdDir, PsdHeaderDirectory.TAG_IMAGE_HEIGHT);
        directory.setBitPerPixel(
            psdDir, PsdHeaderDirectory.TAG_BITS_PER_CHANNEL, PsdHeaderDirectory.TAG_CHANNEL_COUNT);
        directory.setColorMode(psdDir, PsdHeaderDirectory.TAG_COLOR_MODE);
        continue;
      }

      if (dir instanceof PngDirectory) {
        final PngDirectory pngDir = (PngDirectory)dir;

        if (pngDir.getPngChunkType() == PngChunkType.IHDR) {
          directory.setPixelWidth(pngDir, PngDirectory.TAG_IMAGE_WIDTH);
          directory.setPixelHeight(pngDir, PngDirectory.TAG_IMAGE_HEIGHT);
          try {
            int numOfComponent = 1;
            int colorType = pngDir.getInt(PngDirectory.TAG_COLOR_TYPE);
            if (colorType == PngColorType.INDEXED_COLOR.getNumericValue()) {
              directory.setColorMode("Indexed");
            } else if (colorType == PngColorType.GREYSCALE.getNumericValue()) {
              directory.setColorMode("Grayscale");
            } else if (colorType == PngColorType.GREYSCALE_WITH_ALPHA.getNumericValue()) {
              numOfComponent = 2;
              directory.setColorMode("Grayscale");
              directory.setAlpha(true);
            } else if (colorType == PngColorType.TRUE_COLOR.getNumericValue()) {
              numOfComponent = 3;
            } else if (colorType == PngColorType.TRUE_COLOR_WITH_ALPHA.getNumericValue()) {
              numOfComponent = 4;
              directory.setAlpha(true);
            }
            directory.setBitPerPixel(pngDir.getInt(PngDirectory.TAG_BITS_PER_SAMPLE) * numOfComponent);
          } catch (MetadataException e) {
            // Nothing needs to be done
          }
          continue;
        }

        if (pngDir.getPngChunkType() == PngChunkType.pHYs) {
          try {
            final int unit = pngDir.getInt(PngDirectory.TAG_UNIT_SPECIFIER);
            if (unit == 1) {
              directory.setDPIWidth(pngDir, PngDirectory.TAG_PIXELS_PER_UNIT_X, 0.0254);
              directory.setDPIHeight(pngDir, PngDirectory.TAG_PIXELS_PER_UNIT_Y, 0.0254);
            }
          } catch (MetadataException e) {
            // Nothing needs to be done
          }
          continue;
        }

        if (pngDir.getPngChunkType() == PngChunkType.tRNS) {
          directory.setAlpha(true);
          continue;
        }

        continue;
      }

      if (dir instanceof BmpHeaderDirectory) {
        final BmpHeaderDirectory bmpDir = (BmpHeaderDirectory)dir;
        directory.setPixelWidth(bmpDir, BmpHeaderDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(bmpDir, BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
        directory.setDPIWidth(bmpDir, BmpHeaderDirectory.TAG_X_PIXELS_PER_METER, 0.0254);
        directory.setDPIHeight(bmpDir, BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER, 0.0254);
        try {
          final int bitsPerPixel = bmpDir.getInt(BmpHeaderDirectory.TAG_BITS_PER_PIXEL);
          if (bitsPerPixel <= 8) {
            directory.setColorMode("Indexed");
          }
          directory.setBitPerPixel(bitsPerPixel);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        continue;
      }

      if (dir instanceof GifHeaderDirectory) {
        final GifHeaderDirectory gifDir = (GifHeaderDirectory)dir;
        directory.setPixelWidth(gifDir, GifHeaderDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(gifDir, GifHeaderDirectory.TAG_IMAGE_HEIGHT);
        directory.setColorMode("Indexed");
        directory.setBitPerPixel(gifDir, GifHeaderDirectory.TAG_BITS_PER_PIXEL);
        continue;
      }

      if (dir instanceof GifControlDirectory) {
        final GifControlDirectory gifControlDir = (GifControlDirectory)dir;
        directory.setAlpha(gifControlDir, GifControlDirectory.TAG_TRANSPARENT_COLOR_FLAG);
        continue;
      }

      if (dir instanceof IcoDirectory) {
        final IcoDirectory icoDir = (IcoDirectory)dir;
        directory.setPixelWidth(icoDir, IcoDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(icoDir, IcoDirectory.TAG_IMAGE_HEIGHT);
        try {
          if (icoDir.getInt(IcoDirectory.TAG_COLOUR_PALETTE_SIZE) != 0) {
            directory.setColorMode("Indexed");
          }
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        directory.setBitPerPixel(icoDir, IcoDirectory.TAG_BITS_PER_PIXEL);
        directory.setAlpha(true);
        continue;
      }

      if (dir instanceof PcxDirectory) {
        final PcxDirectory pcxDir = (PcxDirectory)dir;
        try {
          directory.setPixelWidth(pcxDir.getInt(PcxDirectory.TAG_XMAX) - pcxDir.getInt(PcxDirectory.TAG_XMIN) + 1);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        try {
          directory.setPixelHeight(pcxDir.getInt(PcxDirectory.TAG_YMAX) - pcxDir.getInt(PcxDirectory.TAG_YMIN) + 1);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        directory.setDPIWidth(pcxDir, PcxDirectory.TAG_HORIZONTAL_DPI);
        directory.setDPIHeight(pcxDir, PcxDirectory.TAG_VERTICAL_DPI);
        directory.setBitPerPixel(pcxDir, PcxDirectory.TAG_BITS_PER_PIXEL, PcxDirectory.TAG_COLOR_PLANES);
        try {
          int colorPlanes = pcxDir.getInt(PcxDirectory.TAG_COLOR_PLANES);
          if (colorPlanes == 1) {
            if (pcxDir.getInt(PcxDirectory.TAG_PALETTE_TYPE) == 2) {
              directory.setColorMode("Grayscale");
            } else {
              directory.setColorMode("Indexed");
            }
          }
          directory.setAlpha(colorPlanes == 4);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        continue;
      }

      if (dir instanceof WavDirectory) {
        final WavDirectory wavDir = (WavDirectory)dir;
        directory.setColorMode("N/A");
        directory.setDuration(wavDir, WavDirectory.TAG_DURATION);
        directory.setAudioCodec(wavDir, WavDirectory.TAG_FORMAT);
        directory.setAudioSampleSize(wavDir, WavDirectory.TAG_BITS_PER_SAMPLE);
        directory.setAudioSampleRate(wavDir, WavDirectory.TAG_SAMPLES_PER_SEC);
      }

      if (dir instanceof AviDirectory) {
        final AviDirectory aviDir = (AviDirectory)dir;
        directory.setPixelWidth(aviDir, AviDirectory.TAG_WIDTH);
        directory.setPixelHeight(aviDir, AviDirectory.TAG_HEIGHT);
        directory.setDuration(aviDir, AviDirectory.TAG_DURATION);
        directory.setVideoCodec(aviDir, AviDirectory.TAG_VIDEO_CODEC);
        directory.setFrameRate(aviDir, AviDirectory.TAG_FRAMES_PER_SECOND);
        directory.setAudioCodec(aviDir, AviDirectory.TAG_AUDIO_CODEC);
        directory.setAudioSampleRate(aviDir, AviDirectory.TAG_SAMPLES_PER_SECOND);
        continue;
      }

      if (dir instanceof WebpDirectory) {
        final WebpDirectory webpDir = (WebpDirectory)dir;
        directory.setPixelWidth(webpDir, WebpDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(webpDir, WebpDirectory.TAG_IMAGE_HEIGHT);
        directory.setAlpha(webpDir, WebpDirectory.TAG_HAS_ALPHA);
        continue;
      }

      if (dir instanceof QuickTimeVideoDirectory) {
        final QuickTimeVideoDirectory qtVideoDir = (QuickTimeVideoDirectory)dir;
        directory.setPixelWidth(qtVideoDir, QuickTimeVideoDirectory.TAG_WIDTH);
        directory.setPixelHeight(qtVideoDir, QuickTimeVideoDirectory.TAG_HEIGHT);
        directory.setDPIWidth(qtVideoDir, QuickTimeVideoDirectory.TAG_HORIZONTAL_RESOLUTION);
        directory.setDPIHeight(qtVideoDir, QuickTimeVideoDirectory.TAG_VERTICAL_RESOLUTION);
        try {
          int bitsPerPixel = qtVideoDir.getInt(QuickTimeVideoDirectory.TAG_DEPTH) % 32;
          directory.setBitPerPixel(bitsPerPixel);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        directory.setDuration(qtVideoDir, QuickTimeVideoDirectory.TAG_DURATION);
        directory.setVideoCodec(qtVideoDir, QuickTimeVideoDirectory.TAG_COMPRESSION_TYPE);
        directory.setFrameRate(qtVideoDir, QuickTimeVideoDirectory.TAG_FRAME_RATE);
        continue;
      }

      if (dir instanceof QuickTimeSoundDirectory) {
        final QuickTimeSoundDirectory qtSoundDir = (QuickTimeSoundDirectory)dir;
        directory.setAudioCodec(qtSoundDir, QuickTimeSoundDirectory.TAG_AUDIO_FORMAT);
        directory.setAudioSampleSize(qtSoundDir, QuickTimeSoundDirectory.TAG_AUDIO_SAMPLE_SIZE);
        directory.setAudioSampleRate(qtSoundDir, QuickTimeSoundDirectory.TAG_AUDIO_SAMPLE_RATE);
        continue;
      }

      if (dir instanceof QuickTimeDirectory) {
        final QuickTimeDirectory qtDir = (QuickTimeDirectory)dir;
        directory.setDuration(qtDir, QuickTimeDirectory.TAG_DURATION);
        continue;
      }

     if (dir instanceof Mp4VideoDirectory) {
        final Mp4VideoDirectory mp4VideoDir = (Mp4VideoDirectory)dir;
        directory.setPixelWidth(mp4VideoDir, Mp4VideoDirectory.TAG_WIDTH);
        directory.setPixelHeight(mp4VideoDir, Mp4VideoDirectory.TAG_HEIGHT);
        directory.setDPIWidth(mp4VideoDir, Mp4VideoDirectory.TAG_HORIZONTAL_RESOLUTION);
        directory.setDPIHeight(mp4VideoDir, Mp4VideoDirectory.TAG_VERTICAL_RESOLUTION);
        try {
          int bitsPerPixel = mp4VideoDir.getInt(Mp4VideoDirectory.TAG_DEPTH) % 32;
          directory.setBitPerPixel(bitsPerPixel);
        } catch (MetadataException e) {
          // Nothing needs to be done
        }
        directory.setDuration(mp4VideoDir, Mp4VideoDirectory.TAG_DURATION);
        directory.setVideoCodec(mp4VideoDir, Mp4VideoDirectory.TAG_COMPRESSION_TYPE);
        directory.setFrameRate(mp4VideoDir, Mp4VideoDirectory.TAG_FRAME_RATE);
        continue;
      }

      if (dir instanceof Mp4SoundDirectory) {
        final Mp4SoundDirectory mp4SoundDir = (Mp4SoundDirectory)dir;
        directory.setAudioCodec(mp4SoundDir, Mp4SoundDirectory.TAG_AUDIO_FORMAT);
        directory.setAudioSampleSize(mp4SoundDir, Mp4SoundDirectory.TAG_AUDIO_SAMPLE_SIZE);
        directory.setAudioSampleRate(mp4SoundDir, Mp4SoundDirectory.TAG_AUDIO_SAMPLE_RATE);
        continue;
      }

      if (dir instanceof Mp4Directory) {
        final Mp4Directory mp4Dir = (Mp4Directory)dir;
        directory.setDuration(mp4Dir, Mp4Directory.TAG_DURATION);
        continue;
      }

      if (dir instanceof EpsDirectory) {
        final EpsDirectory epsDir = (EpsDirectory)dir;
        directory.setPixelWidth(epsDir, EpsDirectory.TAG_IMAGE_WIDTH);
        directory.setPixelHeight(epsDir, EpsDirectory.TAG_IMAGE_HEIGHT);
        try {
          int bitsPerPixel = 24;
          int colorType = epsDir.getInt(EpsDirectory.TAG_COLOR_TYPE);
          if (colorType == 1) {
            String imageData = epsDir.getString(EpsDirectory.TAG_IMAGE_DATA);
            if (imageData != null && imageData.split(" ")[2].equals("1")) {
              bitsPerPixel = 1;
              directory.setColorMode("Bitmap");
            } else {
              bitsPerPixel = 8;
              directory.setColorMode("Grayscale");
            }
          } else if (colorType == 2) {
            directory.setColorMode("Lab");
          } else if (colorType == 4) {
            bitsPerPixel = 32;
            directory.setColorMode("CMYK");
          }
          directory.setBitPerPixel(bitsPerPixel);
          skipEPSPreview = epsDir.containsTag(EpsDirectory.TAG_TIFF_PREVIEW_SIZE);
         } catch (MetadataException e) {
          // Nothing needs to be done
        }
        continue;
      }
    }

    // Set default value if empty
    directory.setPixelWidth(0);
    directory.setPixelHeight(0);
    directory.setOrientation(0);
    directory.setDPIWidth(0.0);
    directory.setDPIHeight(0.0);
    directory.setColorMode("RGB");
    directory.setBitPerPixel(0);
    directory.setAlpha(false);
    directory.setDuration(0);
    directory.setVideoCodec("Unknown");
    directory.setFrameRate(0.0);
    directory.setAudioCodec("Unknown");
    directory.setAudioSampleSize(0);
    directory.setAudioSampleRate(0.0);

    metadata.addDirectory(directory);
  }
}
