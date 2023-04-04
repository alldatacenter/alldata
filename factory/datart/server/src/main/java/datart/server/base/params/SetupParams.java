package datart.server.base.params;

import lombok.Data;

import javax.validation.Valid;

@Data
public class SetupParams {

    @Valid
    private UserRegisterParam user;
}
