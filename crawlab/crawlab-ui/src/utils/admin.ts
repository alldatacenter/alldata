export const sendPv = (page: any) => {
  if (localStorage.getItem('useStats') !== '0') {
    // baidu tongji
    window._hmt?.push(['_trackPageview', page]);

    // umeng
    window.aplus_queue?.push({
      action: 'aplus.sendPV',
      arguments: [{is_auto: true}, {}]
    });
  }
};

export const sendEv = (category: string, eventName: string, optLabel: string, optValue: string) => {
  if (localStorage.getItem('useStats') !== '0') {
    window._hmt?.push(['_trackEvent', category, eventName, optLabel, optValue]);
  }
};
