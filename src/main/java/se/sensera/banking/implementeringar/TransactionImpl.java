package se.sensera.banking.implementeringar;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.*;

import java.util.*;

@Data
@AllArgsConstructor
public class TransactionImpl implements Transaction {

    String id;
    Date created;
    User user;
    Account account;
    double amount;

}
