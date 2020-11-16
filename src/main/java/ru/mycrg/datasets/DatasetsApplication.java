package ru.mycrg.datasets;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import ru.mycrg.geoserver_client.DbInfo;
import ru.mycrg.geoserver_client.GeoserverClient;
import ru.mycrg.geoserver_client.GeoserverInfo;
import ru.mycrg.http_client.HttpClient;
import ru.mycrg.http_client.handlers.BaseRequestHandler;

import javax.transaction.Transactional;

@SpringBootApplication
public class DatasetsApplication {

    public static final String ACCESS_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJ1c2VyX2lkIjoxLCJ1c2VyX25hbWUiOiJhZG1pbkBtYWlsLnJ1Iiwic2NvcGUiOlsiY3JnIl0sIm9yZ2FuaXphdGlvbnMiOltdLCJncm91cHMiOltdLCJleHAiOjE2MDU2MTc5NTYsImF1dGhvcml0aWVzIjpbIkdMT0JBTF9BRE1JTiJdLCJqdGkiOiJiNWRiMjIwOC04MDQyLTRjMmYtOWFlZS0zOGY1N2Y5ZDdiOTkiLCJjbGllbnRfaWQiOiJhZG1pbiJ9.hSM7y56sJRsem2RqQnCGDlY12Dxp1-FEkkrvKQPM8hA";

    private final Environment environment;
    private final ProjectHandler projectHandler;

    public DatasetsApplication(Environment environment,
                               ProjectHandler projectHandler) {
        this.environment = environment;
        this.projectHandler = projectHandler;
    }

    public static void main(String[] args) {
        SpringApplication.run(DatasetsApplication.class, args);
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void appReady() {
        initGeoserverClient();

        projectHandler.handle(1, 718);

        System.exit(0);
    }

    private void initGeoserverClient() {
        final String serverHost = environment.getRequiredProperty("crg.serverHost");

        GeoserverInfo geoserverInfo = GeoserverInfo
                .builder()
                .host(serverHost)
                .port(Integer.parseInt(environment.getRequiredProperty("crg.geoserver.port")))
                .userServiceName(environment.getRequiredProperty("crg.geoserver.userServerName"))
                .build();

        DbInfo dbInfo = DbInfo.builder()
                              .dbHost("postgis")
                              .dbPort(5432)
                              .dbOwnerUser(environment.getRequiredProperty("spring.datasource.username"))
                              .dbOwnerPassword(environment.getRequiredProperty("spring.datasource.password"))
                              .build();

        final HttpClient httpClient = new HttpClient(new BaseRequestHandler(new OkHttpClient()));
        GeoserverClient.initialize(geoserverInfo, dbInfo, httpClient);
    }
}
