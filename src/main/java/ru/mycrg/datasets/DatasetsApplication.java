package ru.mycrg.datasets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import ru.mycrg.geoserver_client.DbInfo;
import ru.mycrg.geoserver_client.GeoserverClient;
import ru.mycrg.geoserver_client.GeoserverInfo;

import javax.transaction.Transactional;

@SpringBootApplication
public class DatasetsApplication {

    @Autowired
    ConfigurableApplicationContext ctx;

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

        projectHandler.handle(718);

        System.exit(0);
    }

    private void initGeoserverClient() {
        final String serverHost = environment.getRequiredProperty("crg.serverUrl");

        GeoserverInfo geoserverInfo = GeoserverInfo
                .builder()
                .host(serverHost)
                .port(Integer.parseInt(environment.getRequiredProperty("crg.geoserver.port")))
                .userServiceName(environment.getRequiredProperty("crg.geoserver.userServerName"))
                .build();

        DbInfo dbInfo = DbInfo.builder()
                              .dbHost(serverHost)
                              .dbPort(Integer.parseInt(environment.getRequiredProperty("crg.db.port")))
                              .dbOwnerUser(environment.getRequiredProperty("spring.datasource.username"))
                              .dbOwnerPassword(environment.getRequiredProperty("spring.datasource.password"))
                              .build();

        GeoserverClient.initialize(geoserverInfo, dbInfo);
    }
}
