package com.youai.crm.account;

/** Login was attempted for an employee account disabled by an administrator. */
public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException() {
        super("该账号已被冻结");
    }
}
