package services;

import com.yandex.ydb.core.Status;
import com.yandex.ydb.core.UnexpectedResultException;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.SessionRetryContext;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.description.TableDescription;
import com.yandex.ydb.table.query.DataQueryResult;
import com.yandex.ydb.table.query.Params;
import com.yandex.ydb.table.result.ResultSetReader;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import com.yandex.ydb.table.transaction.TxControl;
import com.yandex.ydb.table.values.PrimitiveType;
import com.yandex.ydb.table.values.PrimitiveValue;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alex268
 */
@Singleton
public class YdbDataSource implements AutoCloseable, DataSource {
    private static final Logger log = LoggerFactory.getLogger(YdbDataSource.class);

    private static final String TABLE_NAME = "play_hellos";
    private static final TableDescription TABLE_DESCRIPTION = TableDescription.newBuilder()
            .addNullableColumn("name", PrimitiveType.utf8())
            .addNullableColumn("date", PrimitiveType.timestamp())
            .addNullableColumn("uuid", PrimitiveType.utf8())
            .setPrimaryKey("uuid")
            .build();

    private final TableClient client;
    private final SessionRetryContext retryCtx;
    
    public YdbDataSource() {
        GrpcTransport transport = GrpcTransport.forEndpoint("localhost:2136", "local")
                .withReadTimeout(Duration.ofSeconds(10))
                .build();
        
        this.client = TableClient
                .newClient(GrpcTableRpc.ownTransport(transport))
                .build();
        
        this.retryCtx = SessionRetryContext.create(client).build();
        
        initTable(transport.getDatabase() + "/" + TABLE_NAME);
    }
    
    private void initTable(String tablePath) {
        try {
            Status dropResult = retryCtx.supplyStatus(session -> session.dropTable(tablePath))
                    .join();

            if (!dropResult.isSuccess()) {
                log.info("can't drop table");
            }

            retryCtx.supplyStatus(session -> session.createTable(tablePath, TABLE_DESCRIPTION))
                    .join().expect("can't create table " + tablePath);
        } catch (UnexpectedResultException e) {
            log.error("can't init table", e);
            throw e;
        }
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void newUser(User user) {
        try {
            String query = "\n"
                    + "DECLARE $name AS utf8;\n"
                    + "DECLARE $date AS timestamp;\n"
                    + "DECLARE $uuid AS utf8;\n"
                    + "UPSERT INTO " + TABLE_NAME + "(name, date, uuid) VALUES ($name, $date, $uuid);";

            String uuid = UUID.randomUUID().toString();
            Params params = Params.of(
                "$name", PrimitiveValue.utf8(user.name()),
                "$date", PrimitiveValue.timestamp(user.timestamp()),
                "$uuid", PrimitiveValue.utf8(uuid)
            );

            TxControl txControl = TxControl.serializableRw().setCommitTx(true);

            retryCtx.supplyResult(session -> session.executeDataQuery(query, txControl, params))
                    .join().expect("can't read query result");
        } catch (Exception e) {
            log.error("insert record problem", e);
            throw e;
        }
    }

    @Override
    public List<User> listLastUsers() {
        try {
            String query = "SELECT * FROM " + TABLE_NAME;

            TxControl txControl = TxControl.serializableRw();

            DataQueryResult result = retryCtx
                    .supplyResult(session -> session.executeDataQuery(query, txControl))
                    .join().expect("can't read query result");

            if (result.isEmpty()) {
                return Collections.emptyList();
            }

            // First SELECT from query
            ResultSetReader rs = result.getResultSet(0);
            List<User> list = new ArrayList<>();
            
            while (rs.next()) {
                String rowName = rs.getColumn("name").getUtf8();
                Instant rowDate = rs.getColumn("date").getTimestamp();
                list.add(new User(rowName, rowDate));
            }
            
            return list;
        } catch (UnexpectedResultException e) {
            log.error("select record problem", e);
            throw e;
        }
    }
}

