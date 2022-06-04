# Tech Team API

This is a sample repository showcasing the capabalities of [Elide](http://elide.io) library.

### **Setup**:

Set up environment variable if you want to connect to postgres

```bash
JDBC_DATABASE_URL="jdbc:postgresql://localhost:5432/db?user={user}&password={pwd}"
```

Running with h2 requires replacing runtime dependency from postgres to h2.

To run:

```bash
.\gradlew.bat build

java -jar .\build\libs\tech-teams-api-1.0-SNAPSHOT-all.jar
```

Once Jetty is up, navigate to http://localhost:8080
