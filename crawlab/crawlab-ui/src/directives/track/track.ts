import {Directive, DirectiveBinding} from 'vue';
import {sendEvent} from '@/admin/umeng';

const _eventListenerCache = new Map<string, EventListener>();

const getEventListener = (binding: DirectiveBinding<Track>): EventListener => {
  const {code: eventCode, type: eventType, params: eventParams} = binding.value;
  const cacheKey = JSON.stringify({eventCode, eventType, eventParams});
  if (_eventListenerCache.has(cacheKey)) {
    return _eventListenerCache.get(cacheKey) as EventListener;
  }
  const listener = () => sendEvent(eventCode, eventParams, eventType);
  _eventListenerCache.set(cacheKey, listener);
  return listener;
};

const track: Directive<HTMLElement, Track> = {
  mounted(el, binding) {
    const {events} = binding.value;
    const _events: (keyof HTMLElementEventMap)[] = events ? events : ['click'];
    _events?.forEach(ev => {
      el.addEventListener(ev, getEventListener(binding), false);
    });
  },
  unmounted(el, binding) {
    const {events} = binding.value;
    const _events: (keyof HTMLElementEventMap)[] = events ? events : ['click'];
    _events?.forEach(ev => {
      el.removeEventListener(ev, getEventListener(binding), false);
    });
  },
};

export default track;
