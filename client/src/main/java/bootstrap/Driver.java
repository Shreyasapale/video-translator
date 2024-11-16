package bootstrap;

import config.ClientConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.*;

public class Driver {

    private static final Logger logger = Logger.getLogger("StatusClientLogger");

    public static void main(String[] args) {
        configureLogging();

        Properties properties = loadProperties("src/main/resources/config.properties");
        if (properties == null) {
            logger.severe("Closing the program due to missing configurations.");
            return;
        }
        String inputFilePath = properties.getProperty("input.file.path"," src/main/resources/video_ids.txt");
        String outputFilePath = properties.getProperty("output.file.path"," src/main/resources/video_status.txt");
        String baseUri = properties.getProperty("server.baseUri", "http://localhost:8080");

        if (!checkFileStatus(inputFilePath, outputFilePath)) {
            logger.severe("Closing the program due to missing or inaccessible files.");
            return;
        }

        ClientConfig config = loadClientConfig(properties);
        if (config == null) {
            logger.severe("Closing the program due to configuration format errors.");
            return;
        }

        StatusClient client = new StatusClient(baseUri, config, logger,outputFilePath);
        client.checkJobStatusFromFile(inputFilePath);
    }

    private static Properties loadProperties(String configFilePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.severe("Failed to load configurations: " + e.getMessage());
            return null;
        }
        return properties;
    }

    private static ClientConfig loadClientConfig(Properties properties) {
        try {
            int initialPollingInterval = Integer.parseInt(properties.getProperty("polling.interval.initial.milliseconds"));
            int maxPollingInterval = Integer.parseInt(properties.getProperty("polling.interval.max.milliseconds"));
            int timeoutDuration = Integer.parseInt(properties.getProperty("timeout.duration.milliseconds"));
            int maxRetries = Integer.parseInt(properties.getProperty("retries.max"));
            int maxParallelRequests = Integer.parseInt(properties.getProperty("parallel.requests.max"));
            return new ClientConfig(
                    initialPollingInterval,
                    maxPollingInterval,
                    timeoutDuration,
                    maxRetries,
                    maxParallelRequests
            );
        } catch (NumberFormatException e) {
            logger.severe("Invalid format in configuration values: " + e.getMessage());
            return null;
        }
    }

    private static boolean checkFileStatus(String inputFilePath, String outputFilePath) {
        Path inputPath = Paths.get(inputFilePath);
        if (!Files.exists(inputPath)) {
            logger.severe("Input file does not exist: " + inputFilePath);
            return false;
        }

        Path outputPath = Paths.get(outputFilePath);
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
            }
        } catch (IOException e) {
            logger.severe("Could not create output directory or file: " + outputFilePath);
            return false;
        }
        return true;
    }

    private static void configureLogging() {
        try {
            String logDir = "logs/";
            Path logPath = Paths.get(logDir);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }

            Logger rootLogger = Logger.getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            FileHandler fileHandler = new FileHandler(logDir + "status_client.log", true);

            SimpleFormatter formatter = new SimpleFormatter() {
                @Override
                public synchronized String format(LogRecord record) {
                    return String.format(
                            "[%1$tY-%1$tm-%1$td %1$tT] [%2$s] %3$s - %4$s %n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            record.getLoggerName(),
                            record.getMessage()
                    );
                }
            };
            fileHandler.setFormatter(formatter);

            logger.setLevel(Level.ALL);
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);

            logger.info("Logging initialized with custom configuration.");

        } catch (IOException e) {
            System.err.println("Failed to configure logging: " + e.getMessage());
        }
    }
}
