package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class StartWorkerMessageConfirmed implements Serializable {

    private Long deliveryId;

    public StartWorkerMessageConfirmed(Long deliveryId) {
        this.deliveryId = deliveryId;
    }
}
