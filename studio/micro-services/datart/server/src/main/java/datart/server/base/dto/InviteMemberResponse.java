package datart.server.base.dto;

import lombok.Data;

import java.util.Set;

@Data
public class InviteMemberResponse {

    private Set<String> success;

    private Set<String> fail;

}
