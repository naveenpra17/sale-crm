package com.example.acres.service;

import com.example.acres.dto.ProjectDtos.ProjectRequest;
import com.example.acres.dto.ProjectDtos.ProjectResponse;
import com.example.acres.entity.ProjectSettings;
import com.example.acres.entity.User;
import com.example.acres.repository.ProjectSettingsRepository;
import com.example.acres.util.ProjectTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectSettingsRepository repo;
    private final AuditService audit;

    public ProjectService(ProjectSettingsRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public ProjectSettings get() {
        return repo.getSettings();
    }

    public ProjectResponse dto(ProjectSettings p) {
        return new ProjectResponse(
                p.getId(),
                p.getProjectName(),
                p.getTotalAcres(),
                p.getStartDate(),
                p.getDeadline(),
                ProjectTimeUtil.formatDeadlineLocal(p.getDeadline(), p.getTimezone()),
                p.getTimezone(),
                p.getUpdatedAt());
    }

    @Transactional
    public ProjectResponse update(ProjectRequest r, User actor, String ip) {
        ProjectSettings p = get();
        String old = projectJson(p);
        p.setProjectName(r.projectName().trim());
        p.setTotalAcres(r.totalAcres());
        p.setStartDate(r.startDate());
        p.setTimezone(r.timezone().trim());
        p.setDeadline(ProjectTimeUtil.parseDeadlineLocal(r.deadlineLocal(), p.getTimezone()));
        p = repo.save(p);
        audit.log(actor, "UPDATE_PROJECT", "PROJECT", p.getId().toString(), old, projectJson(p), ip);
        return dto(p);
    }

    private String projectJson(ProjectSettings p) {
        return "{\"projectName\":\"" + escape(p.getProjectName()) + "\",\"totalAcres\":" + p.getTotalAcres()
                + ",\"startDate\":\"" + p.getStartDate() + "\",\"deadline\":\"" + p.getDeadline()
                + "\",\"timezone\":\"" + escape(p.getTimezone()) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
