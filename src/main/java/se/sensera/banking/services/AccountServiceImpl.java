package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.Account;
import se.sensera.banking.AccountService;
import se.sensera.banking.AccountsRepository;
import se.sensera.banking.UsersRepository;
import se.sensera.banking.exceptions.UseException;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;

    @Override
    public Account createAccount(String userId, String accountName) throws UseException {
        return null;
    }

    @Override
    public Account changeAccount(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer) throws UseException {
        return null;
    }

    @Override
    public Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        return null;
    }

    @Override
    public Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        return null;
    }

    @Override
    public Account inactivateAccount(String userId, String accountId) throws UseException {
        return null;
    }

    @Override
    public Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) throws UseException {
        return null;
    }
}
