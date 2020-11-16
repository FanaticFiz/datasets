package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dao.DataSourceFactory;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.geoserver_client.services.feature_types.FeatureTypeService;
import ru.mycrg.geoserver_client.services.styles.StyleService;
import ru.mycrg.http_client.exceptions.HttpClientException;

import java.util.List;
import java.util.UUID;

import static ru.mycrg.datasets.DatasetsApplication.ACCESS_KEY;

@Service
public class VectorLayerHandler {

    public static final Logger log = LoggerFactory.getLogger(VectorLayerHandler.class);

    final DataSourceFactory dataSourceFactory;

    public VectorLayerHandler(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public void handle(List<Layer> layers, long orgId, long projectId, String storageName) {
        FeatureTypeService featureTypeService = new FeatureTypeService(ACCESS_KEY);
        StyleService styleService = new StyleService(ACCESS_KEY);

        layers.forEach(layer -> {
            final String oldWorkspaceName = "workspace_" + projectId;
            final String newWorkspaceName = "scratch_database_" + orgId;
            final String currentLayerName = layer.getInternalName();
            final String featureName = String.format("%s_%d_%s", currentLayerName, projectId,
                                                     UUID.randomUUID().toString().substring(0, 4));
            // TODO: get correct srs
            final int srs = 314314;

            log.info("try create vector layer: {} / {}", featureName, currentLayerName);

            // 1. rename exist table to new_name
            try {
                final JdbcTemplate jdbcTemplate = dataSourceFactory.getJdbcTemplate("database_" + orgId);

                final String resourceId = oldWorkspaceName + "." + currentLayerName;
                jdbcTemplate.execute("ALTER TABLE " + resourceId + " RENAME TO " + featureName);

//                final String extId = resourceId + "_extension";
//                jdbcTemplate.execute("ALTER TABLE " + extId + " RENAME TO " + featureName + "_extension");
            } catch (Exception e) {
                log.error("Не удалось переименовать таблицу: {} в БД", currentLayerName);
            }

            // 2. create feature on geoserver
            try {
                featureTypeService.create(newWorkspaceName, storageName, featureName, srs);
            } catch (HttpClientException e) {
                log.error("Не удалось создать слой на геосервере: {}", e.getMessage());
            }

            // 3. join style
            try {
                styleService.associate(newWorkspaceName + ":" + featureName, layer.getStyleName());
            } catch (HttpClientException e) {
                log.error("Не удалось связать стиль со слоем на геосервере: {}", e.getMessage());
            }

            // 4. Обновить нашу обертку
        });
    }
}
