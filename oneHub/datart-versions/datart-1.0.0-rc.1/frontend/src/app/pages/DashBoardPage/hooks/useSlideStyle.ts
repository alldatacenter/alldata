/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import gridSvg from 'app/assets/images/grid.svg';
import { useCallback, useEffect, useMemo, useState } from 'react';

function useSlideStyle(
  autoFit: boolean | undefined,
  editing: boolean,
  rect: DOMRect,
  boardWidth: number,
  boardHeight: number,
  scaleMode: string,
) {
  // 基础1：1 不缩放
  const padNum = useMemo(() => {
    return editing ? 64 : 0;
  }, [editing]);
  const [scale, setScale] = useState<[number, number]>([1, 1]);
  const [zoomRatio, setZoomRatio] = useState(1);
  const [sliderValue, setSliderValue] = useState(20);
  const [slideTranslate, setSlideTranslate] = useState<[number, number]>([
    0, 0,
  ]);
  useEffect(() => {
    if (!editing) {
      setZoomRatio(1);
    }
  }, [editing]);
  const sliderChange = useCallback((newSliderValue: number) => {
    const newZoomRatio = newSliderValue / 40 + 0.5;
    setSliderValue(newSliderValue);
    setZoomRatio(newZoomRatio);
  }, []);

  const zoomIn = useCallback(() => {
    sliderChange(Math.max(sliderValue - 10, 0));
  }, [sliderChange, sliderValue]);

  const zoomOut = useCallback(() => {
    sliderChange(Math.max(sliderValue + 10, 0));
  }, [sliderChange, sliderValue]);

  const scaleChange = useCallback(
    (nextScale: [number, number]) => {
      if (nextScale.join() !== scale.join()) {
        setScale(nextScale);
      }
    },
    [scale],
  );

  const slideTranslateChange = useCallback(
    (nextSlideTranslate: [number, number]) => {
      if (nextSlideTranslate !== slideTranslate) {
        setSlideTranslate(nextSlideTranslate);
      }
    },
    [slideTranslate],
  );

  const [containerWidth, containerHeight] = useMemo(() => {
    if (!rect) {
      return [];
    }
    const width: number = rect.width;
    const height: number = rect.height;
    return [width, height];
  }, [rect]);
  let nextScale = useMemo<[number, number]>(() => {
    if (!containerWidth || !containerHeight) {
      return [0, 0];
    }

    const landscapeScale = ((containerWidth - padNum) / boardWidth) * zoomRatio;
    const portraitScale =
      ((containerHeight - padNum) / boardHeight) * zoomRatio;
    const newScale: [number, number] = new Array<number>(2) as [number, number];

    if (autoFit) {
      const fitScale =
        boardWidth / boardHeight > containerWidth / containerHeight
          ? landscapeScale
          : portraitScale;
      newScale.fill(fitScale);
      return newScale;
    }
    switch (scaleMode) {
      case 'noScale':
        newScale.fill(zoomRatio);
        break;
      case 'scaleWidth':
        newScale.fill(landscapeScale);
        break;
      case 'scaleHeight':
        newScale.fill(portraitScale);
        break;
      case 'scaleFull':
        newScale[0] = landscapeScale;
        newScale[1] = portraitScale;

        break;
    }
    return newScale;
  }, [
    containerWidth,
    containerHeight,
    padNum,
    boardWidth,
    zoomRatio,
    boardHeight,
    autoFit,
    scaleMode,
  ]);
  nextScale = nextScale || scale;
  scaleChange(nextScale);
  let nextTranslate = useMemo<[number, number]>(() => {
    if (!rect) {
      return [0, 0];
    }

    let width: number;
    let height: number;
    const fullscreen = false;
    if (fullscreen) {
      width = window.document.documentElement.clientWidth;
      height = window.document.documentElement.clientHeight;
    } else {
      width = rect.width;
      height = rect.height;
    }
    const translateX = Math.max(
      (Math.max(width - boardWidth * nextScale[0], padNum) / (2 * boardWidth)) *
        100,
      0,
    );
    const translateY = Math.max(
      (Math.max(height - boardHeight * nextScale[1], padNum) /
        (2 * boardHeight)) *
        100,
      0,
    );
    return [translateX, translateY];
  }, [rect, boardWidth, nextScale, padNum, boardHeight]);
  nextTranslate = nextTranslate || slideTranslate;
  slideTranslateChange(nextTranslate);

  const nextBackgroundStyle = useMemo(() => {
    const newBackgroundStyle: React.CSSProperties = { overflow: 'hidden' };
    if (!containerWidth || !containerHeight) {
      return newBackgroundStyle;
    }
    if (
      boardWidth * nextScale[0] + padNum >= containerWidth ||
      boardHeight * nextScale[1] + padNum >= containerHeight
    ) {
      newBackgroundStyle.overflow = 'auto';
    }
    if (editing) {
      newBackgroundStyle.backgroundImage = `url(${gridSvg})`;
    }

    return newBackgroundStyle;
  }, [
    containerWidth,
    containerHeight,
    boardWidth,
    nextScale,
    padNum,
    boardHeight,
    editing,
  ]);

  return {
    zoomIn,
    zoomOut,
    sliderChange,
    sliderValue,
    scale,
    nextBackgroundStyle,
    slideTranslate,
  };
}
export default useSlideStyle;
