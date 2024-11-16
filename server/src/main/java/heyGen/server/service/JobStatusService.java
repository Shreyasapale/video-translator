package heyGen.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class JobStatusService {

    private final ConcurrentHashMap<String, String> jobStatuses = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Value("${job.completionDelay}")
    private long completionDelay;

    public void setJobStatus(String videoId) {
        String finalStatus = Math.random() < 0.9 ? StatusUtil.STATUS_COMPLETED : StatusUtil.STATUS_ERROR;
        jobStatuses.put(videoId, finalStatus);
    }

    public void startJobProcessing(String videoId) {
        jobStatuses.put(videoId, StatusUtil.STATUS_PENDING);
        scheduler.schedule(() -> setJobStatus(videoId), completionDelay, TimeUnit.SECONDS);
    }

    public synchronized String getJobStatus(String videoId) {
        if (!jobStatuses.containsKey(videoId)) {
            startJobProcessing(videoId);
        }
        return jobStatuses.get(videoId);
    }
}
