package com.timetrack.integration.model;

public class Author extends AbstractDto {

    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public Author setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }
}
