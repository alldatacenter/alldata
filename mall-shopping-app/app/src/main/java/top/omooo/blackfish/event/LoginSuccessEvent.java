package top.omooo.blackfish.event;

/**
 * Created by Omooo
 * Date:2019/5/10
 */
public class LoginSuccessEvent {
    private String phoneNumber;

    public LoginSuccessEvent() {
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
