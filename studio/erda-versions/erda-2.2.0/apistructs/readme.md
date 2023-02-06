这个 pkg 下是各个组件对外 API 的类型定义

### 结构体命名

-   request 和 response 的类型结构体名字统一为
    -   &lt;Resource&gt;&lt;Action&gt;\[Version\]Request

        e.g. ApplicationCreateRequest
    -   &lt;Resource&gt;&lt;Action&gt;\[Version\]Response

        e.g. ApplicationCreateResponse

-   事件(event)结构体命名为 &lt;EventName&gt;&lt;Version&gt;Event

### 请求参数在URL的Query中

请求参数在 url 的 query 中，则结构体定义如下：

    type WebhookListRequest  struct {
        OrgID     string `query:"orgID"`
        ProjectID string `query:"projectID"`
    }

    type WebhookListRequest struct {
        OrgID     string `query:"orgID"`
        ProjectID string `query:"projectID"`
    }

### 请求参数在URL的Path中

    type WebhookInspectRequest struct {
        ID string `path`
    }
