import request from '@/utils/request'

export function pageTodoTask(data) {
  return request({
    url: '/workflow/tasks/pageTodo',
    method: 'get',
    params: data
  })
}

export function pageDoneTask(data) {
  return request({
    url: '/workflow/tasks/pageDone',
    method: 'get',
    params: data
  })
}

export function executeTask(data) {
  return request({
    url: '/workflow/tasks/execute/' + data.taskId,
    method: 'post',
    data: data
  })
}
