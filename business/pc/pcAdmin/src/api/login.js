import request from '@/utils/request'
import Logan from 'logan-web';
let logContent = 'Your log content';
let logType = 1;

Logan.log(logContent, logType);

export function login(username, password) {
  return request({
    url: '/admin/login',
    method: 'post',
    data: {
      username,
      password
    }
  })
}

export function getInfo() {
  return request({
    url: '/admin/info',
    method: 'get',
  })
}

export function logout() {
  return request({
    url: '/admin/logout',
    method: 'post'
  })
}
