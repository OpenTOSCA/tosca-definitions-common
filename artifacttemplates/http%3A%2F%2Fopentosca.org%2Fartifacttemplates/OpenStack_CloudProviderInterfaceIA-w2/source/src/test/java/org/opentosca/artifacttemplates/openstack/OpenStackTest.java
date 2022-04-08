package org.opentosca.artifacttemplates.openstack;

import org.junit.jupiter.api.Test;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.HttpStatusCode;
import org.openstack4j.api.OSClient;
import org.opentosca.artifacttemplates.AbstractMockServerTest;
import org.opentosca.artifacttemplates.openstack.model.TerminateVMRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

// See https://www.mock-server.com/mock_server/getting_started.html for test documentation
public class OpenStackTest extends AbstractMockServerTest {

    private static final String projectId = "ba1b1964356a4d90a1398ff8efc42a60";
    private static final String username = "user@example.org";

    @Test
    void testAuthenticationWithUsernameAndPassword() throws Exception {
        String password = "test";

        mockServer.when(
                request()
                        .withPath("/v3/auth/tokens")
                        .withBody(
                                json(
                                        String.format(getStringFromResourceFile("authentication/expectedUsernamePasswordBody.json"),
                                                username, password, projectId),
                                        MatchType.STRICT
                                )
                        )
        ).respond(
                response()
                        .withStatusCode(HttpStatusCode.CREATED_201.code())
                        .withBody(String.format(
                                getStringFromResourceFile("authentication/successfulUsernamePasswordResponse.json"),
                                username, projectId
                        ))
        );

        OpenStackCloudProviderInterfaceEndpoint openStackIA = new OpenStackCloudProviderInterfaceEndpoint();

        TerminateVMRequest request = new TerminateVMRequest();
        request.setHypervisorUserName(username);
        request.setHypervisorUserPassword(password);
        request.setHypervisorTenantID(projectId);
        request.setHypervisorEndpoint(getServerEndpoint("/v3"));

        OSClient<?> osClient = openStackIA.authenticate(request);

        assertNotNull(osClient);
    }

    @Test
    void testAuthenticationWithApplicationIdAndSecret() throws Exception {
        String applicationSecret = "applicationSecret";
        String applicationID = "applicationID";
        mockServer.when(
                request()
                        .withPath("/v3/auth/tokens")
                        .withBody(
                                json(
                                        String.format(getStringFromResourceFile("authentication/expectedApplicationCredentialsBody.json"),
                                                applicationID, applicationSecret),
                                        MatchType.STRICT
                                )
                        )
        ).respond(
                response()
                        .withStatusCode(HttpStatusCode.CREATED_201.code())
                        .withBody(
                                String.format(
                                        getStringFromResourceFile("authentication/successfulApplicationAuthenticationResponse.json"),
                                        username, projectId
                                )
                        )
        );

        OpenStackCloudProviderInterfaceEndpoint openStackIA = new OpenStackCloudProviderInterfaceEndpoint();

        TerminateVMRequest request = new TerminateVMRequest();
        request.setHypervisorApplicationID(applicationID);
        request.setHypervisorApplicationSecret(applicationSecret);
        request.setHypervisorEndpoint(getServerEndpoint("/v3"));

        OSClient<?> osClient = openStackIA.authenticate(request);

        assertNotNull(osClient);
    }
}
