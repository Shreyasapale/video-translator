package heyGen.server.controller;

import heyGen.server.serializer.StatusResponse;
import heyGen.server.service.JobStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    private final JobStatusService jobStatusService;

    @Autowired
    public StatusController(JobStatusService jobStatusService) {
        this.jobStatusService = jobStatusService;
    }

    @GetMapping("status/{videoId}")
    public StatusResponse getStatus(@PathVariable String videoId) {
        String status = jobStatusService.getJobStatus(videoId);
        StatusResponse response = new StatusResponse();
        response.setVideoId(videoId);
        response.setResult(status);
        return response;
    }
}
