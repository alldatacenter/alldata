/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import {Injectable} from '@angular/core';

import 'rxjs/add/operator/toPromise';
import {CurrentCollection, WSMessage} from './basic.form.component';
import {Observable, Observer, Subject} from "rxjs";
// @ts-ignore
import * as NProgress from 'nprogress/nprogress.js';
import {NzNotificationService} from "ng-zorro-antd/notification";
import {HttpClient, HttpErrorResponse, HttpHeaders, HttpParams} from "@angular/common/http";
import {SavePluginEvent, TisResponseResult} from "./tis.plugin";
import {NzDrawerService} from "ng-zorro-antd/drawer";
import {TerminalComponent} from "./terminal.component";
import {ErrorDetailComponent} from "../base/error.detail.component";

declare var TIS: any;

export const WS_CLOSE_MSG = 'event_close_ws';

// @ts-ignore
@Injectable()
export class TISService {
  // 导航栏头部的应用是否可以选择？
  // private appSelectable: boolean = false;
  // https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_client_applications
  // https://medium.com/@lwojciechowski/websockets-with-angular2-and-rxjs-8b6c5be02fac
  // private socket: Subject<MessageEvent>;
  private currApp: CurrentCollection;
  public execId: string;

  public static openSysErrorDetail(drawerService: NzDrawerService, showErrlistLink: boolean, logFileName: string) {
    const drawerRef = drawerService.create<ErrorDetailComponent, {}, {}>({
      nzWidth: "70%",
      nzPlacement: "right",
      nzTitle: "系统异常",
      nzContent: ErrorDetailComponent,
      nzWrapClassName: 'get-gen-cfg-file',
      nzContentParams: {logFileName: logFileName, showErrlistLink: showErrlistLink}
    });
  }

  constructor(protected http: HttpClient
              // , private modalService: NgbModal
    , public notification: NzNotificationService, private drawerService: NzDrawerService) {
  }

  // 一个websocket的例子 https://tutorialedge.net/post/typescript/angular/angular-websockets-tutorial/
  // 创建websocket
  public wsconnect(url: string): Subject<MessageEvent> {
    // if (!this.socket) {
    let socket = this.wscreate(url);
    // }
    return socket;
  }

  private wscreate(url: string): Subject<MessageEvent> {
    let ws = new WebSocket(url);
    let observable = Observable.create(
      (obs: Observer<MessageEvent>) => {
        ws.onmessage = obs.next.bind(obs);
        // ws.onerror = obs.error.bind(obs);
        // TODO： 奇怪 这里接收不到onError的消息，这个后期需要查一下
        //       ws.onerror = function () {
        // console.log("error");
        //       };
        ws.onclose = obs.complete.bind(obs);
        return ws.close.bind(ws);
      }
    );
    let observer = {
      next: (data: WSMessage) => {
        if (data.logtype === WS_CLOSE_MSG) {
          ws.close();
          return;
        }
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      },
    };
    return Subject.create(observer, observable);
  }


  // 是否是日常环境
  public get daily(): boolean {
    return TIS.daily;
  }

  // 通过部门id
  public getIndexListByDptId(dptid: number): Promise<any> {

    return this.http.get<TisResponseResult>('/tjs' + '/runtime/changedomain.ajax?event_submit_do_select_change=y&action=change_domain_action&bizid=' + dptid)
      .toPromise()
      .then(response => response.bizresult as any)
      .catch(this.handleError);
  }

  public set currentApp(currApp: CurrentCollection) {
    // console.log("currentApp");
    //  let err = new Error();
    //  console.log(err.stack);
    this.currApp = currApp;
  }

  public get currentApp(): CurrentCollection {
    return this.currApp;
  }

// 发送http post请求
  public httpPost(url: string, body: string, e?: SavePluginEvent): Promise<TisResponseResult> {

    let headers = new HttpHeaders();
    headers = headers.append('content-type', 'application/x-www-form-urlencoded; charset=UTF-8');
    headers = this.appendHeaders(headers);
    let params: HttpParams = new HttpParams();
    let indexOf = url.indexOf('?');
    if (indexOf > -1) {
      let ps = url.substr(indexOf + 1);
      let pArray: string[] = ps.split("&");
      pArray.forEach((r) => {
        let kv: string[] = r.split("=");
        params = params.append(kv[0], kv[1]);
      });
    }
    let opts = {'headers': headers, 'params': params};
    return this.http.post<TisResponseResult>('/tjs' + url, body, opts)
      .toPromise()
      .then((response) => {
        let result = this.processResult(response, e);
        if (result) {
          return result;
        }
        return Promise.reject(response);
      }).catch(this.handleError);
  }

  protected appendHeaders(headers: HttpHeaders): HttpHeaders {
    let result = headers;
    // console.log(this.currApp);
    if (this.currApp) {
      result = result.set('appname', this.currApp.appName);
      result = result.set('appid', '' + this.currApp.appid);
    }
    if (this.execId) {
      result = result.set("execId", this.execId);
    }
    return result;
    // return headers;
  }


  // 发送json表单
  public jsonPost(url: string, body: any, e?: SavePluginEvent): Promise<TisResponseResult> {
    let headers = new HttpHeaders();
    headers = headers.set('content-type', 'text/json; charset=UTF-8');
    let opts = {'headers': this.appendHeaders(headers)};
    return this.http.post<TisResponseResult>('/tjs' + url, body, opts).pipe()
      .toPromise()
      // @ts-ignore
      .then((response) => {
        let result = this.processResult(response, e);
        if (result) {
          return result;
        } else {
          return response;
        }
        // return () => Promise.reject(response);
      }).catch(this.handleError);
  }

  public jsonp(url: string): Promise<TisResponseResult> {

    return this.http.jsonp('/tjs' + url, "callback").pipe().toPromise()
      .then(response => {
        let tisResult: TisResponseResult = {success: true, bizresult: response};
        return tisResult;
      }).catch(this.handleError)
  }

  public jPost(url: string, o: any): Promise<TisResponseResult> {
    return this.jsonPost(url, JSON.stringify(o));
  }

  private processResult(result: TisResponseResult, e?: SavePluginEvent): TisResponseResult {
    if (result.success) {
      if (result.msg && result.msg.length > 0 && (!e || !e.notShowBizMsg)) {
        let msgContent = '<ul class="list-ul-msg">' + result.msg.map((r) => `<li>${r}</li>`).join('') + '</ul>';
        this.notification.create('success', '成功', msgContent, {nzDuration: 6000});
      }
      return result;
    } else {
      // faild
      let errs: Array<any> = result.errormsg;

      // 在页面上显示错误
      if (!!result.action_error_page_show) {
        return result;
      }
      let logFileName: string[];
      let errContent = '<ul class="list-ul-msg">' + errs.map((r) => {
        if (typeof r === 'string') {
          return `<li>${r}</li>`
        } else if (typeof r === 'object') {
          // {
          //   "logFileName":"20220504130948886",
          //   "message":"IllegalStateException: xxxxxxxxxxxxxxxxxxxxx"
          // }
          logFileName = [r.logFileName];
          return `<li><a>详细</a> ${r.message}  </li>`
        } else {
          throw new Error('illegal type:' + r);
        }
      }).join('') + '</ul>';
      // console.log(errContent);
      let nref = this.notification.create('error', '错误', errContent, {nzDuration: 6000, nzStyle: {width: "600px"}})
      nref.onClick.subscribe((ee) => {
        let pe = <PointerEvent>ee;
        let target: any = pe.target;
        if (target.nodeName === 'A') {
          // console.log(logFileName);
          // this.drawerService.create()
          TISService.openSysErrorDetail(this.drawerService, true, logFileName[0]);
        }
      })
      if (result.errorfields && result.errorfields.length > 0) {
        return result;
      }
      // return result;
    }
  }


  protected handleError = (error: any): Promise<any> => {
    // console.log(error);
    if (error instanceof HttpErrorResponse) {
      let err: HttpErrorResponse = error;
      this.notification.create('error', '错误', `系统发生错误，请联系系统管理员<br> ${err.message} <br> ${err.error} `, {nzPlacement: 'topLeft', nzDuration: 60000, nzStyle: {width: "800px"}});
    }
    // console.log(this);

    NProgress.done();
    return Promise.reject(error.message || error);
  }

}



