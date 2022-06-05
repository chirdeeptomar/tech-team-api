package com.tech.teams;

import com.tech.teams.settings.Settings;
import com.yahoo.elide.standalone.ElideStandalone;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Program {
    public static void main(String[] args) throws Exception {

        // If JDBC_DATABASE_URL is not set, we'll run with H2 in memory.
        boolean inMemory = System.getenv("JDBC_DATABASE_URL") == null;

        Settings settings = new Settings(inMemory) {
        };

        ElideStandalone elide = new ElideStandalone(settings);

        if (inMemory) {
            settings.runLiquibaseMigrations();
        }

        elide.start();
    }
}
