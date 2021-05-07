package org.opentosca.nodetypeimplementations;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryCommand<T> {

    private static final Logger logger = LoggerFactory.getLogger(RetryCommand.class);

    private final long maxRetries;
    private final long timeout;

    private int retryCounter;

    public RetryCommand() {
        this(100, TimeUnit.SECONDS.toMillis(5));
    }

    public RetryCommand(long maxRetries, long timeout) {
        this.maxRetries = maxRetries;
        this.timeout = timeout;
    }

    public int getRetryCounter() {
        return retryCounter;
    }

    public T run(Supplier<T> function) {
        try {
            return function.get();
        } catch (Exception e) {
            return retry(function);
        }
    }

    private T retry(Supplier<T> function) throws RetryCommandException {
        retryCounter = 0;
        logger.info("Command failed, will be retried {} times", maxRetries);
        while (retryCounter < maxRetries) {
            try {
                return function.get();
            } catch (Exception e) {
                retryCounter++;
                logger.info("Command failed on retry {} of {} error: {}", retryCounter, maxRetries, e.getMessage());
                if (retryCounter >= maxRetries) {
                    logger.warn("Max retries exceeded");
                    break;
                }
            }
            try {
                Thread.sleep(timeout);
            } catch (final InterruptedException e) {
                // Ignore
            }
        }
        logger.error("Command failed on all of {} retries", maxRetries);
        throw new RetryCommandException("Command failed on all of " + maxRetries + " retries");
    }
}
