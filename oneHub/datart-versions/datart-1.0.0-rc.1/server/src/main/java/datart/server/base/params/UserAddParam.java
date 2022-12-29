package datart.server.base.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Data
public class UserAddParam {

    @NotBlank(message = "Username can not be empty")
    private String username;

    private String password;

    private String email;

    private String name;

    private String description;

    private Set<String> roleIds = new HashSet<>();

}
