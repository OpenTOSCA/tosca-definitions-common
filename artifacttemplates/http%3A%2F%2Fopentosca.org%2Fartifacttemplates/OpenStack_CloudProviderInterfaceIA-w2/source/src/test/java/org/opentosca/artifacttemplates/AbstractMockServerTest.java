package org.opentosca.artifacttemplates;

import java.io.IOException;
import java.util.Objects;

import org.bouncycastle.util.io.Streams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public abstract class AbstractMockServerTest {

    protected static ClientAndServer mockServer;
    protected static int mockServerPort = 45008;

    @BeforeAll
    static void before() {
        mockServer = startClientAndServer(mockServerPort);
    }

    @AfterAll
    static void after() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    protected String getServerEndpoint(String path) {
        return "http://localhost:" + mockServerPort + path;
    }

    protected String getStringFromResourceFile(String resource) throws IOException {
        return new String(
                Streams.readAll(
                        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource))
                )
        );
    }
}
