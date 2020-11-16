package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dto.ResourceDescription;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.entity.Project;
import ru.mycrg.datasets.repository.LayerRepository;
import ru.mycrg.datasets.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataHandler {

    public static final Logger log = LoggerFactory.getLogger(DataHandler.class);

    private final LayerRepository layerRepository;
    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;

    public DataHandler(ProjectRepository projectRepository,
                       LayerRepository layerRepository,
                       Environment environment) {
        this.layerRepository = layerRepository;
        this.projectRepository = projectRepository;

        datasetRepository = new DatasetRepository(environment);
    }

    public void handleDatabase(Long orgId) {
        String dbName = "database_" + orgId;
        datasetRepository.setNewBd(dbName);

        log.info("Handle database: {}", dbName);

        final List<Project> projects = projectRepository.findAllByOrganizationId(orgId);

        log.info("  Projects: {}", projects.size());

        projects.forEach(this::handleProject);
    }

    private void handleProject(Project project) {
        log.info("  Handle project: {}", project.getInternalName());

        List<ResourceDescription> datasets = new ArrayList<>();
        final List<Layer> projectsLayers = layerRepository.findAllByProjectId(project.getId());

        datasets.add(new ResourceDescription(project.getName(), "SCHEMA", project.getInternalName(),
                projectsLayers.size()));

        projectsLayers
                .forEach(layer -> {
                    final ResourceDescription rdTable = new ResourceDescription(
                            layer.getTitle(),
                            "TABLE",
                            project.getInternalName() + ":" + layer.getInternalName(),
                            0);

                    datasets.add(rdTable);
                });

        log.info("    Layers: {}", datasets.size() - 1);

        datasetRepository.writeDatasets(datasets);
    }
}
