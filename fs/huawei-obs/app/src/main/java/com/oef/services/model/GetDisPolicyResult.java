package com.oef.services.model;

import com.obs.services.model.HeaderResponse;

public class GetDisPolicyResult extends HeaderResponse {
    private DisPolicy policy;

    public GetDisPolicyResult() {

    }

    public GetDisPolicyResult(DisPolicy policy) {
        this.policy = policy;
    }

    public DisPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(DisPolicy policy) {
        this.policy = policy;
    }
}
