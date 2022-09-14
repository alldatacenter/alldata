export const jobStatuses = [
  { name: 'stopping', code: 9, color: '#ffb200' },
  { name: 'starting', code: 8, color: '#ffb200' },
  { name: 'stopped', code: 7, icon: 'md-close-circle', color: '#990033' },
  { name: 'failure', code: 6, icon: 'md-close-circle', color: '#990033' },
  { name: 'running', code: 5, color: '#008000' },
  { name: 'slowTask', code: 4, icon: 'md-help-circle', color: '#6666FF' },
  { name: 'alert', code: 3, icon: 'md-warning', color: '#FF99CC' },
  { name: 'waitRestart', code: 2, icon: 'md-alert', color: '#FF00CC' },
  {
    name: 'success',
    code: 1,
    icon: 'md-checkmark-circle',
    color: '#008000'
  }
]

export const allJobStatuses = [...jobStatuses, { name: 'unstarted', code: 0 }]
