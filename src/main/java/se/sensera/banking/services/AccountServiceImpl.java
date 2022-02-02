package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.implementeringar.AccountImpl;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;

    @Override
    public Account createAccount(String userId, String accountName) throws UseException {
        if (accountsRepository.all().anyMatch(u -> u.getName().equals(accountName))) {
            throw new UseException(Activity.CREATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);
        } else if (usersRepository.getEntityById(userId).isEmpty()) {
            throw new UseException(Activity.CREATE_ACCOUNT, UseExceptionType.USER_NOT_FOUND);
        }
        return accountsRepository.save(new AccountImpl(UUID.randomUUID().toString(), usersRepository.getEntityById(userId).get(), accountName, true));
    }

    @Override
    public Account changeAccount(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer) throws UseException {
        User user = usersRepository.getEntityById(userId).get();
        Account account = accountsRepository
                .getEntityById(accountId)
                .filter(Account::isActive)
                .orElseThrow(() -> new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_ACTIVE));


        if (!account.getOwner().getId().equals(user.getId())) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_OWNER);
        }
        changeAccountConsumer.accept(name -> {
            if (accountsRepository.all().anyMatch(u -> u.getName().equals(name))) {
                throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);
            }
            if (!account.getName().equals(name)) {
                account.setName(name);
                accountsRepository.save(account);
            }
        });

        return account;
    }

    @Override
    public Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        User accountOwner = usersRepository.getEntityById(userId).get();
        User userToBeAssigned = usersRepository.getEntityById(userIdToBeAssigned).get();


        emptyAccount(accountId);
        inactiveAccount (accountId);

        Account account = accountsRepository.getEntityById(accountId).get();

        if (account.getOwner().equals(userToBeAssigned)) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.CANNOT_ADD_OWNER_AS_USER);
        }

        if (!account.getOwner().getId().equals(accountOwner.getId())) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_OWNER);
        }

        if (account.getUsers().anyMatch(user2 -> user2.getId().equals(userIdToBeAssigned))) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT);
        }

        account.addUser(userToBeAssigned);
        accountsRepository.save(account);

        return account;
    }
    private void inactiveAccount(String accountId) throws UseException{
        if (!accountsRepository.getEntityById(accountId).get().isActive()){
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_ACTIVE);
        }
    }

    private void emptyAccount(String accountId) throws UseException {
        if (accountsRepository.getEntityById(accountId).isEmpty()) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);
        }
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
        verifyUser(userId);
        var account = accountsRepository.getEntityById(accountId)
                .orElseThrow(() -> new UseException(Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_FOUND));

        verifyAccount(userId, account);

        account.setActive(false);
        return accountsRepository.save(account);
    }


    private void verifyAccount(String userId, Account account) throws UseException {
        if (!account.getOwner().getId().equals(userId)) {
            throw new UseException(Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_OWNER);
        }
        if (!account.isActive()) {
            throw new UseException(Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);
        }
    }

    private void verifyUser(String userId) throws UseException {
        var user = usersRepository.getEntityById(userId)
                .orElseThrow(() -> new UseException(Activity.INACTIVATE_ACCOUNT, UseExceptionType.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new UseException(Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);
        }
    }

    @Override
    public Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) throws UseException {
        return null;
    }
}
