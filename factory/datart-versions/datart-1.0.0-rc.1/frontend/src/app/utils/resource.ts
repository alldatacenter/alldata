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

export const loadResource = (url: string, iframeDocument?: Document) => {
  return new Promise((resolve, reject) => {
    const curDoc = iframeDocument || document;
    const script = curDoc.createElement('script');
    script.src = url;
    script.async = false;
    script.type = 'text/javascript';
    script.onload = resolve;
    script.onerror = reject;
    curDoc.head.appendChild(script);
  });
};

export const loadStyle = (url: string, iframeDocument?: Document) => {
  return new Promise((resolve, reject) => {
    const curDoc = iframeDocument || document;
    const link = curDoc.createElement('link');
    link.rel = 'stylesheet';
    link.href = url;
    link.type = 'text/css';
    link.onload = resolve;
    link.onerror = reject;
    curDoc.head.appendChild(link);
  });
};

export const loadScript = (url: string, iframeDocument?: Document) => {
  if (/^[^.]+(\.[^.]+)*\.js$/.test(url)) {
    return loadResource(url, iframeDocument);
  } else if (/^[^.]+(\.[^.]+)*\.css$/.test(url)) {
    return loadStyle(url, iframeDocument);
  }
  return loadResource(url, iframeDocument);
};
