package com.qcloud.cos.transfer;

import com.qcloud.cos.exception.PauseException;

import java.io.IOException;

public interface CIPostJob extends Transfer {

    /**
     * Cancels this download.
     *
     * @throws IOException
     */
    public void abort() throws IOException;

}
