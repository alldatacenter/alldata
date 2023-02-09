package com.qcloud.cos.exception;

public class ResponseNotCompleteException extends CosClientException {
    
    /**
     * Creates a new ResponseNotCompleteException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public ResponseNotCompleteException(String message, Throwable t) {
        super(message, t);
    }
}
