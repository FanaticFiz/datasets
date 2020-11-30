package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dao.DataSourceFactory;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.repository.ProjectRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionsHandler {

    public static final Logger log = LoggerFactory.getLogger(PermissionsHandler.class);

    private final ProjectRepository projectRepository;
    private final DataSourceFactory dataSourceFactory;

    public PermissionsHandler(ProjectRepository projectRepository,
                              DataSourceFactory dataSourceFactory) {
        this.projectRepository = projectRepository;
        this.dataSourceFactory = dataSourceFactory;
    }

    public void handle(int orgId, Long projectId) {
        log.info("Handle project: {}", projectId);

        final List<PermissionFix> permissionFixes = projectRepository
                .findAll().stream()
                .filter(project -> projectId.equals(project.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Not fount project by id: " + projectId))
                .getLayers().stream()
                .filter(layer -> layer.getType().equals("vector"))
                .map(layer -> makePermissionFix(projectId, layer))
                .collect(Collectors.toList());

        final JdbcTemplate jdbcTemplate = dataSourceFactory.getJdbcTemplate("database_" + orgId);

        log.info("There are vectorLayers: {}", permissionFixes.size());
        permissionFixes.forEach(fix -> writeToDB(jdbcTemplate, fix));
    }

    private void writeToDB(JdbcTemplate jdbcTemplate, PermissionFix fix) {
        try {
            log.info("{}", fix);
            jdbcTemplate.update("UPDATE data.resources " +
                                        "SET identifier='" + fix.getNewId() + "' " +
                                        "WHERE identifier='" + fix.getOldId() + "'");
        } catch (Exception e) {
            log.error("Не удалось сменить id: {} в БД", fix);
        }
    }

    private PermissionFix makePermissionFix(Long projectId, Layer layer) {
        final String[] splited = layer.getInternalName().split("_" + projectId);

        String oldId = "workspace_" + projectId + ":" + splited[0];
        String newId = "workspace_" + projectId + "." + layer.getInternalName();

        return new PermissionFix(oldId, newId);
    }
}
