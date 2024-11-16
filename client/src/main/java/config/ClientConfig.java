package config;

public class ClientConfig {
    private final long initialPollingIntervalMilliseconds;
    private final long maxPollingIntervalMilliseconds;
    private final long timeoutDurationMilliseconds;
    private final int maxRetries;
    private final int maxParallelRequests;

    public ClientConfig( long initialPollingIntervalMilliseconds, long maxPollingIntervalMilliseconds,
                         long timeoutDurationMilliseconds, int maxRetries, int maxParallelRequests) {
        this.initialPollingIntervalMilliseconds = initialPollingIntervalMilliseconds;
        this.maxPollingIntervalMilliseconds = maxPollingIntervalMilliseconds;
        this.timeoutDurationMilliseconds = timeoutDurationMilliseconds;
        this.maxRetries = maxRetries;
        this.maxParallelRequests = maxParallelRequests;
    }

    public long getInitialPollingIntervalMilliseconds() {
        return initialPollingIntervalMilliseconds;
    }

    public long getMaxPollingIntervalMilliseconds() {
        return maxPollingIntervalMilliseconds;
    }

    public long getTimeoutDurationMilliseconds() {
        return timeoutDurationMilliseconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMaxParallelRequests() {
        return maxParallelRequests;
    }
}
