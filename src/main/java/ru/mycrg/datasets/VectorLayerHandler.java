package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dao.DataSourceFactory;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.repository.LayerRepository;
import ru.mycrg.geoserver_client.services.feature_types.FeatureTypeService;
import ru.mycrg.geoserver_client.services.styles.StyleService;
import ru.mycrg.http_client.exceptions.HttpClientException;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static ru.mycrg.datasets.DatasetsApplication.ACCESS_KEY;

@Service
@Transactional
public class VectorLayerHandler {

    public static final Logger log = LoggerFactory.getLogger(VectorLayerHandler.class);

    final LayerRepository layerRepository;
    final DataSourceFactory dataSourceFactory;

    public VectorLayerHandler(DataSourceFactory dataSourceFactory,
                              LayerRepository layerRepository) {
        this.layerRepository = layerRepository;
        this.dataSourceFactory = dataSourceFactory;
    }

    public void handle(List<Layer> layers, long orgId, long projectId, String storageName) {
        FeatureTypeService featureTypeService = new FeatureTypeService(ACCESS_KEY);
        StyleService styleService = new StyleService(ACCESS_KEY);

        layers.forEach(layer -> {
            if (isLayerAlreadyHandled(layer.getInternalName(), projectId)) {
                log.info("SKIP LAYER: {}/{}", layer.getInternalName(), layer.getTitle());
            } else {
                log.info("Handle layer: {}/{}", layer.getInternalName(), layer.getTitle());

                final String oldWorkspaceName = "workspace_" + projectId;
                final String newWorkspaceName = "scratch_database_" + orgId;
                final String currentLayerName = layer.getInternalName();
                final String featureName = String.format("%s_%d_%s", currentLayerName, projectId,
                                                         UUID.randomUUID().toString().substring(0, 4));
                final int srs = extractCRSNumber(layer.getNativeCRS());

                // 1. rename exist table to new_name
                try {
                    log.info("1. try rename tables in DB");
                    final JdbcTemplate jdbcTemplate = dataSourceFactory.getJdbcTemplate("database_" + orgId);

                    final String resourceId = oldWorkspaceName + "." + currentLayerName;
                    jdbcTemplate.execute("ALTER TABLE " + resourceId + " RENAME TO " + featureName);

                    final String extId = resourceId + "_extension";
                    jdbcTemplate.execute("ALTER TABLE " + extId + " RENAME TO " + featureName + "_extension");
                } catch (Exception e) {
                    log.error("Не удалось переименовать таблицу: {} в БД", currentLayerName);
                }

                // 2. create feature on geoserver
                try {
                    log.info("2. try create feature on geoserver");
                    featureTypeService.create(newWorkspaceName, storageName, featureName, srs);
                } catch (HttpClientException e) {
                    log.error("Не удалось создать слой на геосервере: {}", e.getMessage());
                }

                // 3. join style
                try {
                    log.info("3. try join style");
                    styleService.associate(newWorkspaceName + ":" + featureName, layer.getStyleName());
                } catch (HttpClientException e) {
                    log.error("Не удалось связать стиль со слоем на геосервере: {}", e.getMessage());
                }

                // 4. Обновить нашу обертку
                try {
                    log.info("4. update our layer info");
                    final String dataSourceUri = layer.getDataSourceUri();
                    final String[] splited = dataSourceUri.split("tables/");
                    String newDataSourceUri = splited[0] + "tables/" + featureName;

                    layer.setInternalName(featureName);
                    layer.setDataSourceUri(newDataSourceUri);
                    layerRepository.save(layer);
                } catch (Exception e) {
                    log.error("Не удалось сохранить новый слой: {}", e.getMessage(), e.getCause());
                }
            }
        });
    }

    private boolean isLayerAlreadyHandled(String internalName, long projectId) {
        return internalName.contains("_" + projectId + "_");
    }

    private int extractCRSNumber(String crs) {
        return Integer.parseInt(crs.split(":")[1]);
    }
}
