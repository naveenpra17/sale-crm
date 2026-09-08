package com.example.acres.repository;

import com.example.acres.entity.ProjectSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSettingsRepository extends JpaRepository<ProjectSettings, Long> {
    long SINGLETON_ID = 1L;

    default ProjectSettings getSettings() {
        return findById(SINGLETON_ID).orElseThrow(() -> new IllegalStateException("Project settings are not configured"));
    }
}
