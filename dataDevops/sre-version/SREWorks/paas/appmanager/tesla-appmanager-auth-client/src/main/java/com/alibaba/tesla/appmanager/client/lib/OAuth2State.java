package com.alibaba.tesla.appmanager.client.lib;

class OAuth2State {

    protected static final int ACCESS_TOKEN = 0;
    protected static final int REFRESH_TOKEN = 1;

    private static final int NO_AUTH = 0;
    private static final int BASIC_AUTH = 1;
    private static final int AUTHORIZATION_AUTH = 2;
    private static final int FINAL_AUTH = 3;

    private static final int[] ACCESS_STATES = new int[]{
        NO_AUTH,
        BASIC_AUTH,
        AUTHORIZATION_AUTH,
        FINAL_AUTH
    };

    private static final int[] REFRESH_STATES = new int[]{
        NO_AUTH,
        AUTHORIZATION_AUTH,
        FINAL_AUTH
    };

    private int[] state;

    private int position;

    OAuth2State(int tokenType) {
        switch (tokenType) {
            default:
            case ACCESS_TOKEN:
                state = ACCESS_STATES;
                break;
            case REFRESH_TOKEN:
                state = REFRESH_STATES;
                break;
        }
    }

    protected void nextState() {
        position++;
    }

    protected boolean isFinalAuth() {
        return state[position] == FINAL_AUTH;
    }

    protected boolean isBasicAuth() {
        return state[position] == BASIC_AUTH;
    }

    protected boolean isAuthorizationAuth() {
        return state[position] == AUTHORIZATION_AUTH;
    }
}