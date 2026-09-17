package com.youai.crm.account;

/** Login was attempted for an account that no longer exists. */
public class AccountUnavailableException extends RuntimeException {
    public AccountUnavailableException() {
        super("该账号已被删除或已被继承");
    }
}
