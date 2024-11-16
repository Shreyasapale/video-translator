package bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.ClientConfig;
import config.StatusUtil;
import serializer.StatusResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class StatusClient {

    private final HttpClient httpClient;
    private final URI baseUri;
    private final ObjectMapper objectMapper;
    private final ClientConfig config;
    private final Logger logger;
    private final String outputFilePath;

    public StatusClient(String serverBaseUrl, ClientConfig config, Logger logger, String outputFilePath) {
        this.outputFilePath = outputFilePath;
        this.httpClient = HttpClient.newHttpClient();
        this.baseUri = URI.create(serverBaseUrl);
        this.objectMapper = new ObjectMapper();
        this.config = config;
        this.logger = logger;
    }

    public void checkJobStatusFromFile(String inputFilePath) {

        Path inputPath = Paths.get(inputFilePath);
        ExecutorService executor = Executors.newFixedThreadPool(config.getMaxParallelRequests());

        try{
            List<String> videoIds = Files.readAllLines(inputPath);
            for (String videoId : videoIds) {
                executor.submit(() -> checkAndWriteStatus(videoId));
            }
        } catch (IOException e) {
            logger.severe("Error reading input file or writing to output file");
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    private void checkAndWriteStatus(String videoId) {
        long pollingInterval = config.getInitialPollingIntervalMilliseconds();
        long startTime = System.currentTimeMillis();
        int retryCount = 0;

        while (true) {
            String jobStatus = fetchJobStatus(videoId);

            if (StatusUtil.STATUS_COMPLETED.equals(jobStatus)) {
                writeToFile( videoId+", "+StatusUtil.STATUS_COMPLETED);
                logger.info( "Video Translation completed successfully for videoId "+ videoId);
                break;
            } else if (StatusUtil.STATUS_ERROR.equals(jobStatus)) {
                writeToFile( videoId+", "+StatusUtil.STATUS_ERROR);
                logger.warning("Video Translation encountered an error for videoId "+ videoId);
                break;
            } else {
                logger.info(  "Video translation is still pending for videoId "+videoId);
            }

            if ((System.currentTimeMillis() - startTime) >= config.getTimeoutDurationMilliseconds()) {
                logger.warning("Video translation is still pending after max wait time for videoId " + videoId);
                break;
            }

            try {
                Thread.sleep(pollingInterval);
                pollingInterval = Math.min(pollingInterval * 2, config.getMaxPollingIntervalMilliseconds());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("Polling interrupted for video " + videoId);
                break;
            }

            if (jobStatus == null && retryCount < config.getMaxRetries()) {
                retryCount++;
                logger.info( "Retrying... for videoId"+ videoId);
            } else if (retryCount >= config.getMaxRetries()) {
                logger.warning( "Max retries reached for videoId "+ videoId);
                break;
            }
        }
    }

    private synchronized void writeToFile(String videoId) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(videoId);
            writer.newLine();
            writer.flush();
            logger.info("Final status written for videoId " + videoId);
        } catch (IOException e) {
            logger.severe("Error writing to output file for video " + videoId);
        }
    }

    private String fetchJobStatus(String videoId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + "/status/" + videoId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                StatusResponse statusResponse = objectMapper.readValue(response.body(), StatusResponse.class);
                return statusResponse.getResult();
            } else {
                logger.severe("Failed to get job status for video ID: " + videoId);
            }
        } catch (IOException | InterruptedException e) {
            logger.severe("Error fetching job status for video ID " + videoId);
        }
        return null;
    }
}
