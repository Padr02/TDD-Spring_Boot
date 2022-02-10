package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.implementeringar.AccountImpl;
import se.sensera.banking.utils.ListUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
        User user = getUser(userId);
        Account account = getAccountIfActive(accountId);
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
    //testlösning
    public Account changeAccount2(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer) throws UseException {
        User user = getUser(userId);
        Account account = getAccountIfActive(accountId);
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_OWNER);
        }
        UpdateAccount updateAccount = new UpdateAccount(account, name -> accountsRepository.all().anyMatch(u -> u.getName().equals(name)));
        changeAccountConsumer.accept(updateAccount);
        if (!updateAccount.shouldSave){
            return account;
        }
        return accountsRepository.save(account);
    }

    private Account getAccountIfActive(String accountId) throws UseException {
        return accountsRepository
                .getEntityById(accountId)
                .filter(Account::isActive)
                .orElseThrow(() -> new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_ACTIVE));
    }

    @Override
    public Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        User accountOwner = getUser(userId);
        User userToBeAssigned = getUser(userIdToBeAssigned);
        emptyAccount(accountId);
        inactiveAccount(accountId);
        Account account = getAccount(accountId);
        isOwner(accountOwner, userToBeAssigned, account);
        isAssigned(userIdToBeAssigned, account);
        account.addUser(userToBeAssigned);
        accountsRepository.save(account);
        return account;
    }

    private Account getAccount(String accountId) {
        return accountsRepository.getEntityById(accountId).get();
    }

    private User getUser(String userId) {
        return usersRepository.getEntityById(userId).get();
    }

    private void emptyAccount(String accountId) throws UseException {
        if (accountsRepository.getEntityById(accountId).isEmpty()) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);
        }
    }

    private void inactiveAccount(String accountId) throws UseException {
        if (!accountsRepository.getEntityById(accountId).get().isActive()) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_ACTIVE);
        }
    }

    private void isOwner(User accountOwner, User userToBeAssigned, Account account) throws UseException {
        if (account.getOwner().equals(userToBeAssigned)) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.CANNOT_ADD_OWNER_AS_USER);
        }

        if (!account.getOwner().getId().equals(accountOwner.getId())) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_OWNER);
        }
    }

    private void isAssigned(String userIdToBeAssigned, Account account) throws UseException {
        if (account.getUsers().anyMatch(user2 -> user2.getId().equals(userIdToBeAssigned))) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT);
        }
    }

    @Override
    public Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        Account account = getAccount(accountId);
        User user = getUser(userId);
        User userToBeAssigned = getUser(userIdToBeAssigned);
        isOwner(user, userToBeAssigned, account);
        if (account.getUsers().noneMatch(user2 -> user2.getId().equals(userIdToBeAssigned))) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_NOT_ASSIGNED_TO_THIS_ACCOUNT);
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
    public Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) {
        if (userId == null) {
            return switch (sortOrder) {
                case None -> searchValue.isEmpty()
                        ? ListUtils.applyPage(accountsRepository.all(), pageNumber, pageSize)
                        : accountsRepository.all().filter(u -> u.getName().contains(searchValue));
                case AccountName -> ListUtils.applyPage(accountsRepository.all().sorted(Comparator.comparing(Account::getName)), pageNumber, pageSize);
            };
        }
        return accountsRepository.all()
                .filter(u -> u.getUsers()
                        .anyMatch(a -> a.getId().equals(userId))
                        || u.getOwner().getId().equals(userId));
    }
    //testlösning
    private static class UpdateAccount implements ChangeAccount {
        boolean shouldSave = false;
        Account account;
        Predicate<String> checkUniqueName;

        public UpdateAccount(Account account, Predicate<String> checkUniqueName) {
            this.account = account;
            this.checkUniqueName = checkUniqueName;
        }

        @Override
        public void setName(String name) throws UseException {
            if (checkUniqueName.test(name)) {
                throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);
            }
            if (!account.getName().equals(name)) {
                account.setName(name);
                shouldSave=true;
            }
        }
    }
}
