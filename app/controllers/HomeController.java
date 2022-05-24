package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import javax.inject.Inject;
import play.mvc.*;
import services.DataSource;
import services.User;

//import views.html.*;  
/**
 *
 * @author myfear
 */
public class HomeController extends Controller {
    
    @Inject
    private DataSource dataSource;

    /**
     * An action that renders an HTML page with a welcome message. The
     * configuration in the <code>routes</code> file means that this method will
     * be called when the application receives a <code>GET</code> request with a
     * path of <code>/</code>.
     *
     * @return
     */
    public Result index() {
        StringBuilder sb = new StringBuilder();
        dataSource.listLastUsers()
                .forEach((user) -> sb
                        .append("\n").append(user.name())
                        .append(",").append(user.timestamp())
                );
        return ok("It works!" + sb.toString());
    }

    /**
     *
     * @return
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Result sayHello() {
        JsonNode json = request().body().asJson();
        String name = json.findPath("name").textValue();
        if (name == null) {
            return badRequest("Missing parameter [name]");
        } else {
            dataSource.newUser(new User(name, Instant.now()));
            return ok("Hello " + name);
        }
    }

}
