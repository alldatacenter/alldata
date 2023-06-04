/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.common;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageUtils {


    public static void resize(String src, Double toWith, Double toHeight) throws Exception {
        if (toWith == null && toHeight == null) {
            return;
        }
        BufferedImage srcImage = ImageIO.read(new File(src));
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        double factor = 1.0;
        if (toHeight != null) {
            factor = toHeight / height;
        } else if (toWith != null) {
            factor = toWith / width;
        }

        Thumbnails.of(src)
                .scale(factor)
                .outputQuality(1)
                .allowOverwrite(true)
                .toFile(src);

    }

}
