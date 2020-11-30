package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.entity.Project;
import ru.mycrg.datasets.repository.ProjectRepository;
import ru.mycrg.geoserver_client.services.storage.vector.VectorStorage;
import ru.mycrg.http_client.exceptions.HttpClientException;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.mycrg.datasets.DatasetsApplication.ROOT_ACCESS_KEY;

@Service
public class ProjectHandler {

    public static final Logger log = LoggerFactory.getLogger(ProjectHandler.class);

    private final RasterLayerHandler rasterLayerHandler;
    private final VectorLayerHandler vectorLayerHandler;
    private final ProjectRepository projectRepository;

    public ProjectHandler(ProjectRepository projectRepository,
                          RasterLayerHandler rasterLayerHandler,
                          VectorLayerHandler vectorLayerHandler) {
        this.projectRepository = projectRepository;
        this.vectorLayerHandler = vectorLayerHandler;
        this.rasterLayerHandler = rasterLayerHandler;
    }

    public void handle(int orgId, Long projectId) {
        final Project foundProject = projectRepository
                .findAll().stream()
                .filter(project -> projectId.equals(project.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Not fount project by id: " + projectId));

        log.info("Handle project: {} / {}", foundProject.getInternalName(), projectId);

        final List<Layer> vectorLayers = foundProject.getLayers().stream()
                                                     .filter(layer -> layer.getType().equals("vector"))
                                                     .collect(Collectors.toList());
        log.info("There are vectorLayers: {}", vectorLayers.size());

        final String storageName = createVectorStorage(orgId, projectId);
        vectorLayerHandler.handle(vectorLayers, orgId, projectId, storageName);


//        final List<Layer> rasterLayers = foundProject.getLayers().stream()
//                                                     .filter(layer -> layer.getType().equals("raster"))
//                                                     .collect(Collectors.toList());
//        log.info("There are rasterLayers: {}", rasterLayers.size());
//        rasterLayerHandler.handle(rasterLayers, orgId, projectId);
    }

    private String createVectorStorage(int orgId, Long projectId) {
        VectorStorage vectorStorage = new VectorStorage(ROOT_ACCESS_KEY);
        try {
            final String databaseName = "database_" + orgId;
            final String schemaName = "workspace_" + projectId;
            final String workspaceName = "scratch_database_" + orgId;
            final String datasetName = String.format("dataset_%s", UUID.randomUUID().toString().substring(0, 6));

            log.info("create storage: {} in {}", datasetName, workspaceName);

            vectorStorage.create(databaseName, schemaName, workspaceName, datasetName);

            return datasetName;
        } catch (HttpClientException e) {
            log.error("Не удалось создать хранилище на геосервере: {}", e.getMessage());

            throw new RuntimeException("Не удалось создать хранилище на геосервере", e.getCause());
        }
    }
}
