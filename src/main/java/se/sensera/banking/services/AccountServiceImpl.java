package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;

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
        User user = usersRepository.getEntityById(userId).get();
        User user1 = usersRepository.getEntityById(userIdToBeAssigned).get();
        if  (accountsRepository.getEntityById(accountId).isEmpty()){
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);
        }
        Account account = accountsRepository.getEntityById(accountId).get();

        if (!account.isActive() ) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_ACTIVE);
        }

        if (account.getOwner().equals(user1)){
            throw new UseException(Activity.UPDATE_ACCOUNT,UseExceptionType.CANNOT_ADD_OWNER_AS_USER);
        }

        if (!account.getOwner().getId().equals(user.getId())){
            throw new UseException(Activity.UPDATE_ACCOUNT,UseExceptionType.NOT_OWNER);
        }

        if (account.getUsers().anyMatch(user2 -> user2.getId().equals(userIdToBeAssigned))){
            throw new UseException(Activity.UPDATE_ACCOUNT,UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT);
        }

        account.addUser(user1);
        accountsRepository.save(account);

        return account;
    }

    @Override
    public Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        Account account=accountsRepository.getEntityById(accountId).get();
        User user=usersRepository.getEntityById(userId).get();
        User userToBeAssigned=usersRepository.getEntityById(userIdToBeAssigned).get();

        if (!account.getOwner().getId().equals(user.getId())){
            throw new UseException(Activity.UPDATE_ACCOUNT,UseExceptionType.NOT_OWNER);
        }

        if (account.getUsers().noneMatch(user2 -> user2.getId().equals(userIdToBeAssigned))){
            throw new UseException(Activity.UPDATE_ACCOUNT,UseExceptionType.USER_NOT_ASSIGNED_TO_THIS_ACCOUNT);
        }

        account.removeUser(userToBeAssigned);
        accountsRepository.save(account);
        return account;
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
