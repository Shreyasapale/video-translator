package heyGen;

import bootstrap.StatusClient;
import config.ClientConfig;
import heyGen.server.ServerApplication;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StatusClientIntegrationTest {

    @LocalServerPort
    private int port;

    private StatusClient statusClient;
    private static final Logger logger = Logger.getLogger("StatusClientLogger");

    private final String inputFilePath = "src/test/resources/video_ids_test.txt";
    private final String outputFilePath = "src/test/resources/video_status_test.txt";

    @BeforeEach
    public void setUp() throws IOException {
        ClientConfig config = new ClientConfig(2000, 16000, 180000, 10, 5);

        String serverBaseUrl = "http://localhost:" + port;
        statusClient = new StatusClient(serverBaseUrl, config, logger, outputFilePath);

        createTestInputFile();
    }

    @Test
    public void testCheckJobStatusFromFile() throws IOException {
        statusClient.checkJobStatusFromFile(inputFilePath);

        File outputFile = new File(outputFilePath);
        assertTrue(outputFile.exists(), "Output file should be created.");

        String outputContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(outputContent.contains("video1234, completed") || outputContent.contains("video1234, error"),
                "Output should contain status for video1234.");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(new File(inputFilePath).toPath());
        Files.deleteIfExists(new File(outputFilePath).toPath());
    }

    private void createTestInputFile() throws IOException {
        FileWriter writer = new FileWriter(inputFilePath);
        writer.write("video1234\n");
        writer.write("video5678\n");
        writer.close();
    }
}
