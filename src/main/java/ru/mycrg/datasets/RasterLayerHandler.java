package ru.mycrg.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dao.DataSourceFactory;
import ru.mycrg.datasets.entity.Layer;
import ru.mycrg.datasets.repository.LayerRepository;
import ru.mycrg.geoserver_client.services.coverages.Coverage;
import ru.mycrg.geoserver_client.services.coverages.Coverages;
import ru.mycrg.geoserver_client.services.storage.raster.RasterStorage;
import ru.mycrg.http_client.ResponseModel;
import ru.mycrg.http_client.exceptions.HttpClientException;

import java.util.List;

import static ru.mycrg.datasets.DatasetsApplication.ACCESS_KEY;

@Service
public class RasterLayerHandler {

    public static final Logger log = LoggerFactory.getLogger(RasterLayerHandler.class);

    final LayerRepository layerRepository;
    final DataSourceFactory dataSourceFactory;

    public RasterLayerHandler(DataSourceFactory dataSourceFactory,
                              LayerRepository layerRepository) {
        this.layerRepository = layerRepository;
        this.dataSourceFactory = dataSourceFactory;
    }

    public void handle(List<Layer> layers, long orgId, long projectId) {
        layers.forEach(layer -> {
            try {
                log.info("========== HANDLE raster layer: {}", layer.getTitle());
                final String workspace = "scratch_database_" + orgId;

                String fileUrl = getCoverageStoreFileUrl(projectId, layer.getDataStoreName());
                String newStoreName = createRasterStore(workspace, layer.getDataStoreName(), fileUrl);
                String layerName = createRasterLayer(workspace, newStoreName, layer);

                log.info("========== Created raster layer: {}.{}", newStoreName, layerName);
            } catch (Exception e) {
                log.error("========== FAILED");
            }
        });
    }

    private String getCoverageStoreFileUrl(long projectId, String storeName) {
        try {
            RasterStorage rasterStorage = new RasterStorage(ACCESS_KEY);

            final var responseModel = rasterStorage.getStorage("workspace_" + projectId, storeName);
            if (!responseModel.isSuccessful()) {
                throw new HttpClientException("Error get storage info: " + responseModel.toString());
            } else {
                return responseModel.getBody().getCoverageStore().getUrl();
            }
        } catch (HttpClientException e) {
            throw new RuntimeException("Error get storage info: " + e.getMessage());
        }
    }

    private String createRasterStore(String workspace, String oldStoreName, String fileUrl) {
        try {
            RasterStorage rasterStorage = new RasterStorage(ACCESS_KEY);

            final ResponseModel<Object> responseModel = rasterStorage.createGeoTIFF(workspace, oldStoreName, fileUrl);
            if (!responseModel.isSuccessful()) {
                throw new HttpClientException("Error create raster storage: " + responseModel.toString());
            } else {
                return responseModel.getBody().toString();
            }
        } catch (HttpClientException e) {
            throw new RuntimeException("Error create raster storage: " + e.getMessage());
        }
    }

    private String createRasterLayer(String workspace, String storeName, Layer layer) {
        try {
            log.info("Try create coverage on: {}", storeName);

            Coverages coverages = new Coverages(ACCESS_KEY);

            final String crs = String.valueOf(extractCRSNumber(layer.getNativeCRS()));
            final String name = layer.getInternalName();
            final String title = layer.getTitle();

            final Coverage coverage = new Coverage(name, title, crs);
            final ResponseModel<Object> responseModel = coverages.create(workspace, storeName, coverage);
            if (!responseModel.isSuccessful()) {
                throw new HttpClientException("Error create raster layer: " + responseModel.toString());
            } else {
                return responseModel.getBody().toString();
            }
        } catch (HttpClientException e) {
            throw new RuntimeException("Error create raster layer: " + e.getMessage());
        }
    }

    private int extractCRSNumber(String crs) {
        return Integer.parseInt(crs.split(":")[1]);
    }
}
