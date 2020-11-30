package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import ru.mycrg.datasets.entity.Project;
import ru.mycrg.datasets.repository.ProjectRepository;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class DatasetsApplication {

    public static final Logger log = LoggerFactory.getLogger(DatasetsApplication.class);

    private final Environment environment;
    private final DataHandler dataHandler;
    private final ProjectRepository projectRepository;

    public DatasetsApplication(Environment environment, ProjectRepository projectRepository, DataHandler dataHandler) {
        this.environment = environment;
        this.projectRepository = projectRepository;
        this.dataHandler = dataHandler;
    }

    public static void main(String[] args) {
        SpringApplication.run(DatasetsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void appReady() {
        final String initDb = environment.getRequiredProperty("spring.datasource.url");
        final String targetDb = environment.getRequiredProperty("crg.datasource.target.url");

        log.info("Copy from db: {} to: {}", initDb, targetDb);

        final List<Long> allExistOrganizationIds = projectRepository
                .findAll().stream()
                .map(Project::getOrganizationId)
                .distinct()
                .filter(orgId -> orgId > 0)
                .collect(Collectors.toList());
        log.info("Found organizations: {}", allExistOrganizationIds);

        allExistOrganizationIds.forEach(dataHandler::handleDatabase);

        System.exit(0);
    }
}
