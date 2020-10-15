package ru.mycrg.datasets.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.mycrg.datasets.entity.Layer;

import java.util.List;

@RepositoryRestResource
public interface LayerRepository extends PagingAndSortingRepository<Layer, Long> {

    List<Layer> findAllByProjectId(Long id);

}
