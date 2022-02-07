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
    final List<Consumer<Transaction>> consumerList = new ArrayList<>();
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

        double sum = transactionsRepository.all()
                .filter(x -> x.getAccount()
                        .equals(account))
                .mapToDouble(Transaction::getAmount)
                .sum();

        if ((amount + sum) < 0) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_FUNDED);
        }

        if (account.getUsers().anyMatch(u -> u.equals(user))) {

            return transactionsRepository.save(new TransactionImpl(UUID.randomUUID().toString(),date, user, account, amount));
        }

        if (!account.getOwner().equals(user)) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }

        var transaction = new TransactionImpl(UUID.randomUUID().toString(),date, user, account, amount);

        synchronized (transaction) {
            consumerList.forEach(tra -> tra.accept(transaction));
        }

        return transactionsRepository.save(transaction);
    }

    @Override
    public double sum(String created, String userId, String accountId) throws UseException {

        getDate(created);

        Account account = getAccount(accountId);
        User user = getUser(userId);

        if (account.getOwner().getId().equals(user.getId()) || account.getUsers().anyMatch(x -> x.equals(user))) {

            Date date1 = date;

            return transactionsRepository.all()
                    .filter(x -> x.getAccount()
                            .equals(account))
                    .filter(u -> u.getCreated().before(date1))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

        }
        throw new UseException(Activity.SUM_TRANSACTION, UseExceptionType.NOT_ALLOWED);
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