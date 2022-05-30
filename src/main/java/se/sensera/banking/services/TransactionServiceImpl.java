package se.sensera.banking.services;

import lombok.Data;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.implementeringar.TransactionImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;


@Data
public class TransactionServiceImpl implements TransactionService {

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;
    TransactionsRepository transactionsRepository;
    static final List<Consumer<Transaction>> consumerList = new ArrayList<>();
    Date date = null;


    public TransactionServiceImpl(UsersRepository usersRepository, AccountsRepository accountsRepository, TransactionsRepository transactionsRepository) {
        this.usersRepository = usersRepository;
        this.accountsRepository = accountsRepository;
        this.transactionsRepository = transactionsRepository;
    }

    @Override
    public Transaction createTransaction(String created, String userId, String accountId, double amount) throws UseException {
        getDate(created);
        Account account = getAccount(accountId);
        User user = getUser(userId);
        double sum = getSumCurrentDate(account);
        isFunded(amount, sum);
        isTransactionAllowed(account, user);
        var transaction = new TransactionImpl(UUID.randomUUID().toString(), date, user, account, amount);
        consumerList.forEach(tra -> tra.accept(transaction));

        return transactionsRepository.save(transaction);
    }

    private void isFunded(double amount, double sum) throws UseException {
        if ((amount + sum) < 0) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_FUNDED);
        }
    }

    private double getSumCurrentDate(Account account) {
        return transactionsRepository.all()
                .filter(x -> x.getAccount()
                        .equals(account))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private void isTransactionAllowed(Account account, User user) throws UseException {
        if (!account.getOwner().equals(user) && account.getUsers().noneMatch(u -> u.equals(user))) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }
    }

    @Override
    public double sum(String created, String userId, String accountId) throws UseException {
        getDate(created);
        Account account = getAccount(accountId);
        User user = getUser(userId);
        isSumAllowed(account, user);
        Date otherDate = date;
        return getSumBeforeDate(account, otherDate);
    }

    private double getSumBeforeDate(Account account, Date otherDate) {
        return transactionsRepository.all()
                .filter(x -> x.getAccount()
                        .equals(account))
                .filter(u -> u.getCreated().before(otherDate))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private void isSumAllowed(Account account, User user) throws UseException {
        if (!account.getOwner().getId().equals(user.getId()) && account.getUsers().noneMatch(x -> x.equals(user))) {
            throw new UseException(Activity.SUM_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }
    }

    private void getDate(String created) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            synchronized (consumerList) {
                date = dateFormatter.parse(created);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    private Account getAccount(String accountId) {
        return accountsRepository.getEntityById(accountId).get();
    }

    private User getUser(String userId) {
        return usersRepository.getEntityById(userId).get();
    }

    @Override
    public void addMonitor(Consumer<Transaction> monitor) {
        consumerList.add(monitor);
    }
}