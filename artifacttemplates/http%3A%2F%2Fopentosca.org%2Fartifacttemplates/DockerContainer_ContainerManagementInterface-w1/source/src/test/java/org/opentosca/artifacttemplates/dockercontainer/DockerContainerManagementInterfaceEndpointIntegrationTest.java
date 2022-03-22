package org.opentosca.artifacttemplates.dockercontainer;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.opentosca.artifacttemplates.OpenToscaIASpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OpenToscaIASpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DockerContainerManagementInterfaceEndpointIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @Test
    void getWsdlUsingQueryParameter() throws IOException {
        HttpGet request = new HttpGet(getBasePath() + "?wsdl");
        CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);

        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    private String getBasePath() {
        return "http://localhost:" + serverPort + "/" + DockerContainerConstants.PORT_TYPE_NAME;
    }
}
