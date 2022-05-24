package services;

import java.util.List;

public interface DataSource {
    void newUser(User user);
    List<User> listLastUsers();
}
