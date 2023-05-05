// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import Markdown from 'marked';
import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import css from 'highlight.js/lib/languages/css';
import less from 'highlight.js/lib/languages/less';
import scss from 'highlight.js/lib/languages/scss';
import stylus from 'highlight.js/lib/languages/stylus';
import kotlin from 'highlight.js/lib/languages/kotlin';
import java from 'highlight.js/lib/languages/java';
import go from 'highlight.js/lib/languages/go';
import ruby from 'highlight.js/lib/languages/ruby';
import xml from 'highlight.js/lib/languages/xml';
import sql from 'highlight.js/lib/languages/sql';
import vim from 'highlight.js/lib/languages/vim';
import powershell from 'highlight.js/lib/languages/powershell';
import bash from 'highlight.js/lib/languages/bash';
import DOMPurify from 'dompurify';
import { encodeHtmlTag } from '.';

import 'highlight.js/styles/github.css';
import './marked.scss';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('css', css);
hljs.registerLanguage('less', less);
hljs.registerLanguage('scss', scss);
hljs.registerLanguage('stylus', stylus);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('java', java);
hljs.registerLanguage('go', go);
hljs.registerLanguage('ruby', ruby);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('vim', vim);
hljs.registerLanguage('powershell', powershell);
hljs.registerLanguage('bash', bash);

let inited = false;
const renderer = new Markdown.Renderer();

const escapeHtml = (unsafe: string) => {
  return unsafe
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
};

// add escapeHtml so that html tag will not be rendered in MD, if not under code block
renderer.html = function (html: string) {
  return escapeHtml(html);
};

const removePreview = (el: HTMLElement | Element) => {
  el.remove();
  const originEle = document.getElementsByClassName('md-img-preview')[0];
  if (originEle) {
    (originEle.parentElement as HTMLElement).focus();
  }
};

window.previewFun = function (el: HTMLElement) {
  if (el) {
    const curEle = el.parentElement as HTMLElement;
    const cls = 'img-enlarge-bg';
    if (curEle.classList.contains(cls)) {
      removePreview(document.getElementsByClassName(cls)[0]);
    } else {
      const cloneEle = curEle.cloneNode(true) as HTMLElement;
      cloneEle.classList.add(cls);
      document.body.append(cloneEle);
      cloneEle.focus();
      cloneEle.addEventListener('keydown', (e: KeyboardEvent) => {
        if (e.keyCode === 27) {
          removePreview(cloneEle);
        }
      });
    }
  }
};
window.removeDom = (el: HTMLElement) => {
  if (el.classList.contains('img-enlarge-bg')) {
    removePreview(el);
  }
};

const overrideRenderFns = {
  image(src: string, title: string) {
    return `<div tabindex="-1" onclick="removeDom(this)"><img class="md-img-preview" src="${src}" onclick="previewFun(this)" alt="${
      title || 'preview-image'
    }" /></div>`;
  },
};

const renderMarkdown = (content?: string, renderFns = {}) => {
  if (!content) return content;

  if (!inited) {
    inited = true;
    Markdown.setOptions({
      renderer,
      linkTarget: '__blank',
      breaks: true,
      sanitizer(text: string) {
        // eslint-disable-next-line no-script-url
        if (text.startsWith('<a') && text.includes('javascript:')) {
          return encodeHtmlTag(text);
        }
        if (text.startsWith('<script>')) {
          return encodeHtmlTag(text);
        }
        return DOMPurify.sanitize(text);
      },
      highlight(code: any) {
        return hljs.highlightAuto(code).value;
      },
    });
  }

  const defaultRenderFns = {
    link(href: string, title: string, text: string) {
      return `<a href="${href}" title="${title}" rel="noopener noreferrer" target="_blank">${text}</a>`;
    },
  };

  Object.assign(renderer, overrideRenderFns, { ...defaultRenderFns, ...renderFns });

  return Markdown(content);
};

export default renderMarkdown;
