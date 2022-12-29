import { Quill } from 'react-quill';

const Embed = Quill.import('blots/embed');
class TagBlot extends Embed {
  static blotName = 'tag';
  static tagName = 'span';
  static className = 'tag-container';

  static create(data): any {
    const node = super.create(data);

    const { name, background, color } = data;

    // 将 delta 数据中的 insert 值存储在 dom 中，以便 static value 中能拿到
    node.setAttribute('data-name', name);
    node.setAttribute('data-background', background);
    node.setAttribute('data-color', color);
    node.setAttribute('contenteditable', 'false');

    var nodeSpan = document.createElement('span');

    nodeSpan.textContent = name;
    node.appendChild(nodeSpan);

    node.addEventListener('click', event => {});
    return node;
  }
  static value(node): any {
    return {
      name: node.getAttribute('data-name'),
      background: node.getAttribute('data-background'),
      color: node.getAttribute('data-color'),
    };
  }
}
export default TagBlot;
