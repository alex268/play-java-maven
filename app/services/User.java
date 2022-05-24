package services;

import java.time.Instant;

/**
 *
 * @author alex268
 */
public class User {
    private final String name;
    private final Instant timestamp;

    public User(String name, Instant timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }
    
    public String name() { return this.name; }
    public Instant timestamp() { return this.timestamp; }
}
