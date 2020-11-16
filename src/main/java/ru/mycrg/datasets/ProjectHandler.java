package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.entity.Project;
import ru.mycrg.datasets.repository.ProjectRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectHandler {

    public static final Logger log = LoggerFactory.getLogger(ProjectHandler.class);

    private final ProjectRepository projectRepository;

    public ProjectHandler(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void handle(int projectId) {
        final Project foundProject = projectRepository
                .findAll().stream()
                .filter(project -> projectId == project.getId())
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Not fount project by id: " + projectId));

        final List<Layer> vectorLayers = foundProject.getLayers().stream()
                                                     .filter(layer -> layer.getType().equals("vector"))
                                                     .collect(Collectors.toList());

        final List<Layer> rasterLayers = foundProject.getLayers().stream()
                                                     .filter(layer -> layer.getType().equals("raster"))
                                                     .collect(Collectors.toList());

        log.info("\n****** Vector layers: {}", vectorLayers.size());
        vectorLayers.forEach(layer -> log.info("{}", layer.getTitle()));

        log.info("\n****** Raster layers: {}", rasterLayers.size());
        rasterLayers.forEach(layer -> log.info("{}", layer.getTitle()));
    }
}
