package ru.mycrg.datasets.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.mycrg.datasets.entity.Project;

import java.util.List;

@RepositoryRestResource
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAll();

    List<Project> findAllByOrganizationId(Long orgId);

}
