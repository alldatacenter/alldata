export declare global {
  interface NotificationSetting extends BaseModel {
    type?: string;
    name?: string;
    description?: string;
    enabled?: boolean;
    global?: boolean;
    title?: string;
    template?: string;
    triggers?: string[];
    targets?: NotificationSettingTarget[];
    mail?: NotificationSettingMail;
    mobile?: NotificationSettingMobile;
  }

  interface NotificationSettingMail {
    server?: string;
    port?: string;
    user?: string;
    password?: string;
    sender_email?: string;
    sender_identity?: string;
    to?: string;
    cc?: string;
  }

  interface NotificationSettingMobile {
    webhook?: string;
  }

  interface NotificationSettingTarget extends BaseModel {
    model?: string;
  }

  interface NotificationSettingTrigger {
    name?: string;
    event?: string;
  }
}
