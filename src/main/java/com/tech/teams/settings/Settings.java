package com.tech.teams.settings;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import com.yahoo.elide.ElideSettings;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.common.collect.Lists;
import com.tech.teams.Program;
import com.tech.teams.entity.Profile;
import com.tech.teams.filters.CorsFilter;
import com.tech.teams.hooks.SaveProfileHook;
import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.validator.TemplateConfigValidator;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.datastores.search.SearchDataStore;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.modelconfig.store.ConfigDataStore;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class contains common settings for both test and production.
 */
@Slf4j
public abstract class Settings implements ElideStandaloneSettings {

    protected String jdbcUrl;
    protected String jdbcUser;
    protected String jdbcPassword;

    protected boolean inMemory;

    public Settings(boolean inMemory) {
        jdbcUrl = Optional.ofNullable(System.getenv("JDBC_DATABASE_URL"))
                .orElse("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1");

        jdbcUser = Optional.ofNullable(System.getenv("JDBC_DATABASE_USERNAME"))
                .orElse("sa");

        jdbcPassword = Optional.ofNullable(System.getenv("JDBC_DATABASE_PASSWORD"))
                .orElse("");

        this.inMemory = inMemory;
    }

    @Override
    public int getPort() {
        // Heroku exports port to come from $PORT
        return Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::valueOf)
                .orElse(8080);
    }

    @Override
    public boolean enableSwagger() {
        return true;
    }

    @Override
    public boolean enableGraphQL() {
        return true;
    }

    @Override
    public String getModelPackageName() {
        return "com.tech.teams.entity";
    }

    @Override
    public ElideSettings getElideSettings(EntityDictionary dictionary, DataStore dataStore,
                                          JsonApiMapper mapper) {
        dictionary.bindTrigger(Profile.class, Operation.CREATE, TransactionPhase.POSTCOMMIT, new SaveProfileHook(),
                false);
        return ElideStandaloneSettings.super.getElideSettings(dictionary, dataStore,
                mapper);
    }

    @Override
    public DataStore getDataStore(MetaDataStore metaDataStore, AggregationDataStore aggregationDataStore,
                                  EntityManagerFactory entityManagerFactory) {

        List<DataStore> stores = new ArrayList<>();

        DataStore jpaDataStore = new JpaDataStore(
                entityManagerFactory::createEntityManager,
                em -> new NonJtaTransaction(em, TXCANCEL, DEFAULT_LOGGER, true, true));

        SearchDataStore searchDataStore = new SearchDataStore(jpaDataStore, entityManagerFactory, true, 3, 50);
        stores.add(searchDataStore);

        if (getAnalyticProperties().enableDynamicModelConfigAPI()) {
            stores.add(new ConfigDataStore(getAnalyticProperties().getDynamicConfigPath(),
                    new TemplateConfigValidator(getClassScanner(), getAnalyticProperties().getDynamicConfigPath())));
        }

        stores.add(metaDataStore);
        stores.add(aggregationDataStore);

        return new MultiplexManager(stores.toArray(new DataStore[0]));
    }

    @Override
    public ElideStandaloneAnalyticSettings getAnalyticProperties() {
        return new ElideStandaloneAnalyticSettings() {
            @Override
            public boolean enableDynamicModelConfig() {
                return true;
            }

            @Override
            public boolean enableAggregationDataStore() {
                return true;
            }

            @Override
            public boolean enableMetaDataStore() {
                return false;
            }

            @Override
            public String getDefaultDialect() {
                if (inMemory) {
                    return SQLDialectFactory.getDefaultDialect().getDialectType();
                } else {
                    return SQLDialectFactory.getPostgresDialect().getDialectType();
                }
            }

            @Override
            public String getDynamicConfigPath() {
                return "src/main/resources/analytics";
            }
        };
    }

    @Override
    public ElideStandaloneAsyncSettings getAsyncProperties() {
        return new ElideStandaloneAsyncSettings() {

            @Override
            public boolean enabled() {
                return false;
            }

            @Override
            public boolean enableCleanup() {
                return true;
            }

            @Override
            public boolean enableExport() {
                return false;
            }
        };
    }

    @Override
    public Properties getDatabaseProperties() {
        Properties dbProps;

        if (inMemory) {
            return getInMemoryProps();
        }

        try {
            dbProps = new Properties();
            dbProps.load(
                    Program.class.getClassLoader().getResourceAsStream("dbconfig.properties"));

            dbProps.setProperty("javax.persistence.jdbc.url", jdbcUrl);
            dbProps.setProperty("javax.persistence.jdbc.user", jdbcUser);
            dbProps.setProperty("javax.persistence.jdbc.password", jdbcPassword);
            return dbProps;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Class<?>> getFilters() {
        return Lists.newArrayList(CorsFilter.class);
    }

    @Override
    public void updateServletContextHandler(ServletContextHandler servletContextHandler) {
        ResourceHandler resource_handler = new ResourceHandler();

        try {
            resource_handler.setDirectoriesListed(false);
            resource_handler.setResourceBase(Objects.requireNonNull(Settings.class.getClassLoader()
                    .getResource("META-INF/resources/")).toURI().toString());
            servletContextHandler.insertHandler(resource_handler);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Properties getInMemoryProps() {
        Properties options = new Properties();

        options.put("hibernate.show_sql", "true");
        options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        options.put("hibernate.current_session_context_class", "thread");
        options.put("hibernate.jdbc.use_scrollable_resultset", "true");
        options.put("hibernate.default_batch_fetch_size", 100);

        options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        options.put("javax.persistence.jdbc.url", jdbcUrl);
        options.put("javax.persistence.jdbc.user", jdbcUser);
        options.put("javax.persistence.jdbc.password", jdbcPassword);

        return options;
    }

    public void runLiquibaseMigrations() throws Exception {
        // Run Liquibase Initialization Script
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                new JdbcConnection(DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)));

        try (Liquibase liquibase = new liquibase.Liquibase("db/changelog/changelog.xml",
                new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("db1");
        } catch (Exception e) {
            log.error("Unable to apply migrations {0}", e.getCause());
        }
    }
}