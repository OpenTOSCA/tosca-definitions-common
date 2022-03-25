package org.opentosca.artifacttemplates.openstack;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.opentosca.artifacttemplates.OpenToscaIASpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@SpringBootTest(classes = OpenToscaIASpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenStackSpringTest {

    @LocalServerPort
    private int serverPort;

    @Test
    void getWsdlUsingQueryParameter() throws IOException {
        HttpGet request = new HttpGet(getBasePath() + "?wsdl");
        CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);

        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    private String getBasePath() {
        return "http://localhost:" + serverPort + "/" + OpenStackConstants.PORT_TYPE_NAME;
    }
}
