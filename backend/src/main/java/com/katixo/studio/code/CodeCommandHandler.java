package com.katixo.studio.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace.CommandResult;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

/** Executes a queued coding-agent command and streams output over the job socket. */
@Component
public class CodeCommandHandler implements JobHandler {

    private final CodeWorkspace workspace;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final WorkspaceCheckStatus checkStatus;

    public CodeCommandHandler(CodeWorkspace workspace, JobService jobService, ObjectMapper objectMapper,
                              WorkspaceCheckStatus checkStatus) {
        this.workspace = workspace;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.checkStatus = checkStatus;
    }

    @Override
    public JobType type() {
        return JobType.CODE_COMMAND;
    }

    @Override
    public void handle(Job job) throws Exception {
        CodeCommandRequest request = objectMapper.readValue(job.getParamsJson(), CodeCommandRequest.class);
        jobService.updateProgress(job.getId(), 5);
        CommandResult result = workspace.runCommandForJob(
                request.command(),
                request.directory(),
                line -> jobService.publishLog(job.getId(), line));
        checkStatus.recordCommandResult(request.command(), result.exitCode() == 0);
        if (result.exitCode() == 0) {
            jobService.markDone(job.getId(), null);
        } else {
            throw new IllegalStateException("Command exited " + result.exitCode());
        }
    }
}
