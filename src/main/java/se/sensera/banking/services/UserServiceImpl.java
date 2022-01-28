package se.sensera.banking.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.User;
import se.sensera.banking.UserService;
import se.sensera.banking.UsersRepository;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.implementeringar.UserImpl;
import se.sensera.banking.utils.ListUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    UsersRepository usersRepository;

    @Override
    public User createUser(String name, String personalIdentificationNumber) throws UseException {
        if (usersRepository.all()
                .anyMatch(user -> user.getPersonalIdentificationNumber().equals(personalIdentificationNumber)))
            throw new UseException(Activity.CREATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);

        UserImpl user = new UserImpl(UUID.randomUUID().toString(), name, personalIdentificationNumber, true);
        return usersRepository.save(user);
    }

    @Override
    public User changeUser(String userId, Consumer<ChangeUser> changeUser) throws UseException {
        if (usersRepository.getEntityById(userId).isEmpty()) {
            throw new UseException(Activity.UPDATE_USER, UseExceptionType.NOT_FOUND);
        }

        User user = usersRepository.getEntityById(userId).get();

        changeUser.accept(new ChangeUser() {
            @Override
            public void setName(String name) {
                user.setName(name);
                usersRepository.save(user);
            }

            @Override
            public void setPersonalIdentificationNumber(String personalIdentificationNumber) throws UseException {
                boolean notUnique = usersRepository.all()
                        .map(User::getPersonalIdentificationNumber)
                        .anyMatch(personalIdentificationNumber::equals);

                if (notUnique) {
                    throw new UseException(Activity.UPDATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);
                }

                user.setPersonalIdentificationNumber(personalIdentificationNumber);
                usersRepository.save(user);
            }
        });
        return user;
    }

    @Override
    public User inactivateUser(String userId) throws UseException {
        User user = usersRepository
                .getEntityById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UseException(Activity.UPDATE_USER, UseExceptionType.NOT_FOUND));

        user.setActive(false);
        return usersRepository.save(user);
    }

    @Override
    public Optional<User> getUser(String userId) {
        return usersRepository.getEntityById(userId);
    }

    @Override
    public Stream<User> find(String searchString, Integer pageNumber, Integer pageSize, SortOrder sortOrder) {
        String nameUpper = searchString.toUpperCase();

        return switch (sortOrder) {
            case None -> searchString.isEmpty()
                    ? ListUtils.applyPage(usersRepository.all(), pageNumber, pageSize).filter(User::isActive)
                    : usersRepository.all().filter(user -> user.getName().toUpperCase().contains(nameUpper));
            case Name -> usersRepository.all().sorted(Comparator.comparing(User::getName));
            case PersonalId -> usersRepository.all().sorted(Comparator.comparing(User::getPersonalIdentificationNumber));

        };
    }
}
