package se.sensera.banking.implementeringar;


import lombok.Data;
import se.sensera.banking.Account;
import se.sensera.banking.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
public class AccountImpl implements Account {

    String id;
    User owner;
    String name;
    boolean active;
    private List<User> userList = new ArrayList<>();

    public AccountImpl(String id, User owner, String name, boolean active) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.active = active;
    }


    @Override
    public Stream<User> getUsers() {
        return userList.stream();
    }

    @Override
    public void addUser(User user) {
        userList.add(user);
    }

    @Override
    public void removeUser(User user) {
        userList.remove(user);
    }

}
