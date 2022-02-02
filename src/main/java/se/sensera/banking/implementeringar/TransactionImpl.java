package se.sensera.banking.implementeringar;

import lombok.Data;
import se.sensera.banking.*;

import java.util.*;

@Data

public class TransactionImpl implements Transaction {

    Date date;
    User user;
    Account account;
    double amount;


    public TransactionImpl(Date date, User user, Account account, double amount) {
        this.date = date;
        this.user = user;
        this.account = account;
        this.amount = amount;
    }


    @Override
    public String getId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Date getCreated() {
        return date;

    }

    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public Account getAccount() {
        return this.account;
    }

    @Override
    public synchronized double getAmount() {
        return this.amount;
    }
}
