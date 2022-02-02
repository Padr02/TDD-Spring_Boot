package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.implementeringar.TransactionImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;
    TransactionsRepository transactionsRepository;

    @Override
    public Transaction createTransaction(String created, String userId, String accountId, double amount) throws UseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        Date date = null;
        try {
            date = dateFormatter.parse(created);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
        Account account = accountsRepository.getEntityById(accountId).get();
        User user = usersRepository.getEntityById(userId).get();

        double sum = transactionsRepository.all()
                .filter(x -> x.getAccount().getId()
                        .equals(accountId))
                .mapToDouble(Transaction::getAmount)
                .sum();

        if ((amount + sum) < 0) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_FUNDED);
        }
        if (account.getUsers().anyMatch(u -> u.equals(user))) {
            return transactionsRepository.save(new TransactionImpl(date, user, account, amount));
        }
        if (!account.getOwner().equals(user)) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }

        return transactionsRepository.save(new TransactionImpl(date, usersRepository.getEntityById(userId).get(), accountsRepository.getEntityById(accountId).get(), amount));
    }

    @Override
    public double sum(String created, String userId, String accountId) throws UseException {
        return 0;
    }

    @Override
    public void addMonitor(Consumer<Transaction> monitor) {

    }
}
