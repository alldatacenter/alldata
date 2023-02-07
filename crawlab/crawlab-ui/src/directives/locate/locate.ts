import {Directive} from 'vue';

const locate: Directive<HTMLElement, Locate> = {
  mounted(el, binding) {
    let name: string;
    if (typeof binding.value === 'string') {
      name = binding.value;
    } else if (typeof binding.value === 'object') {
      name = binding.value.name;
    } else {
      return;
    }
    const className = el.getAttribute('class') || '';
    const cls = className.split(' ');
    if (!cls.includes(name)) {
      el.setAttribute('class', className + ' name');
    }
    el.setAttribute('id', name);
  }
};

export default locate;
