package ru.mycrg.datasets;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import ru.mycrg.datasets.dto.ResourceDescription;

import javax.sql.DataSource;
import java.util.List;

@Service
public class DatasetRepository {

    public static final Logger log = LoggerFactory.getLogger(DatasetRepository.class);

    private final Environment environment;

    private DataSource dataSource;

    public DatasetRepository(Environment environment) {
        this.environment = environment;
    }

    public void writeDatasets(List<ResourceDescription> datasets) {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        datasets.forEach(item -> writeDataset(item, jdbcTemplate));
    }

    public void setNewBd(String dbName) {
        dataSource = getDataSource(dbName);
    }

    private void writeDataset(ResourceDescription dataset, NamedParameterJdbcTemplate jdbcTemplate) {
        try {
            final MapSqlParameterSource source = new MapSqlParameterSource()
                    .addValue("title", dataset.getTitle())
                    .addValue("type", dataset.getType())
                    .addValue("count", dataset.getItemsCount())
                    .addValue("resId", dataset.getResourceIdentifier());

            String sql = "INSERT INTO data.resource_description(" +
                    "title, type, resource_identifier, items_count, created_at, last_modified) " +
                    "VALUES (:title, :type, :resId, :count, now(), now()) ON CONFLICT DO NOTHING";

             jdbcTemplate.update(sql, source);
        } catch (Exception e) {
            log.error("Failed insert row: {}. Reason: {}", dataset.getResourceIdentifier(), e.getMessage());
        }
    }

    private DataSource getDataSource(String dbName) {
        final String url = environment.getRequiredProperty("crg.datasource.target.url");
        final String userName = environment.getRequiredProperty("crg.datasource.target.username");
        final String password = environment.getRequiredProperty("crg.datasource.target.password");

        HikariDataSource targetDataSource = new HikariDataSource();
        targetDataSource.setDriverClassName("org.postgresql.Driver");
        targetDataSource.setJdbcUrl(url + dbName);
        targetDataSource.setSchema("data");
        targetDataSource.setUsername(userName);
        targetDataSource.setPassword(password);

        return targetDataSource;
    }
}
