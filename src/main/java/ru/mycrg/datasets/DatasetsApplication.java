package ru.mycrg.datasets;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import ru.mycrg.datasets.entity.Project;
import ru.mycrg.datasets.repository.ProjectRepository;
import ru.mycrg.geoserver_client.DbInfo;
import ru.mycrg.geoserver_client.GeoserverClient;
import ru.mycrg.geoserver_client.GeoserverInfo;
import ru.mycrg.http_client.HttpClient;
import ru.mycrg.http_client.handlers.BaseRequestHandler;

import javax.transaction.Transactional;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class DatasetsApplication {

    public static final Logger log = LoggerFactory.getLogger(DatasetsApplication.class);

    public static final String ACCESS_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX25hbWUiOiJhZG1pbkBtYWlsLnJ1Iiwic2NvcGUiOlsiY3JnIl0sIm9yZ2FuaXphdGlvbnMiOltdLCJncm91cHMiOltdLCJleHAiOjE2MDY1ODQ1NzIsImF1dGhvcml0aWVzIjpbIkdMT0JBTF9BRE1JTiJdLCJqdGkiOiIxOTRlYjFjNi0xZDM0LTQxMmQtOGM0MS04MGZhNDUwMDI1NjgiLCJjbGllbnRfaWQiOiJhZG1pbiJ9.ZvBMi4RIyJ_Ou5jUJuqVnYBlg2gwD8yzC_4aAqQRDp4";

    private final Environment environment;
    private final ProjectHandler projectHandler;
    private final ProjectRepository projectRepository;

    public DatasetsApplication(Environment environment,
                               ProjectRepository projectRepository,
                               ProjectHandler projectHandler) {
        this.environment = environment;
        this.projectHandler = projectHandler;
        this.projectRepository = projectRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(DatasetsApplication.class, args);
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void appReady() {
        initGeoserverClient();

        final int orgId = 1;

        final List<Project> projects = projectRepository.findAll();
        log.info("START HANDLE ORGANIZATION: {}", orgId);
        AtomicInteger projectCount = new AtomicInteger(projects.size());
        log.info("There are {} projects", projectCount);

        projects.forEach(project -> {
//            if (project.getId() == 457) {
                final Long projectId = project.getId();
                log.info("HANDLE Project: {} {} / {}", projectId, project.getInternalName(), project.getName());

                projectHandler.handle(orgId, projectId);

                projectCount.getAndDecrement();
                log.info("DONE HANDLE PROJECT: {}", projectId);
                log.info("****************************************************************************************************");
                log.info("Left: {}", projectCount);
//            }
        log.info("****************************************************************************************************");
        });
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
