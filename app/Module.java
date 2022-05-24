import com.google.inject.AbstractModule;
import services.DataSource;
import services.YdbDataSource;

/**
 *
 * @author alex268
 */
public class Module extends AbstractModule {

    @Override
    public void configure() {
        bind(DataSource.class).to(YdbDataSource.class);
    }
}
