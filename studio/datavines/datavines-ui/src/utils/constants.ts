/* eslint-disable no-useless-escape */
export const PWD_REG = /^.*(?=.{6,})(?=.*\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%^&*? ]).*$/;
export const EMAIL_REG = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
export const CODE_REG = /^[a-zA-Z0-9]{4}$/;

export const DV_STORAGE_LOGIN = 'dv-loginInfo';
export const DV_LANGUAGE = 'dv-language';
export const DV_WORKSPACE_ID = 'dv_workspace_id';
