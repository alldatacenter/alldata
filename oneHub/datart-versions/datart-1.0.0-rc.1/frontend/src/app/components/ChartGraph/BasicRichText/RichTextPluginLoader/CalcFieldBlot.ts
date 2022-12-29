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
import Quill from 'quill';

const Embed = Quill.import('blots/embed');
class CalcFieldBlot extends Embed {
  static blotName = 'calcfield';
  static tagName = 'span';
  static className = 'calcfield';

  static create(data) {
    const node = super.create();
    node.addEventListener(
      'click',
      e => {
        const event = new Event('mention-clicked', {
          bubbles: true,
          cancelable: true,
        });
        // @ts-ignore
        event.value = data;
        // @ts-ignore
        event.event = e;
        window.dispatchEvent(event);
        e.preventDefault();
      },
      false,
    );
    const denotationChar = document.createElement('span');
    denotationChar.className = 'ql-calcfield-denotation-char';
    denotationChar.innerHTML = data.denotationChar;
    node.appendChild(denotationChar);
    node.innerHTML += data.text;
    return CalcFieldBlot.setDataValues(node, data);
  }

  static setDataValues(element, data) {
    const domNode = element;
    Object.keys(data).forEach(key => {
      domNode.dataset[key] = data[key];
    });
    return domNode;
  }

  static value(domNode) {
    return domNode.dataset;
  }
}
export default CalcFieldBlot;
