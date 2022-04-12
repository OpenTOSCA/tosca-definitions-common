package org.opentosca.artifacttemplates.ubuntuvm;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {UbuntuVMOperatingSystemInterfaceEndpoint.class, SoapUtil.class})
public class WaitForAvailabilityTest {

    @Test
    public void run() throws Exception {
        // Dummy values
        String host = "host";
        String user = "user";
        String key = "key";
        String messageId = "messageId";
        String replyTo = "replyTo";
        OpenToscaHeaders headers = new OpenToscaHeaders(messageId, replyTo, Collections.emptyMap());

        // Set dummy headers
        mockStatic(SoapUtil.class);
        doReturn(headers).when(SoapUtil.class, "parseHeaders", any());

        // Intercept response
        ArgumentCaptor<InvokeResponse> responseCapture = ArgumentCaptor.forClass(InvokeResponse.class);
        doNothing().when(SoapUtil.class, "sendSoapResponse", responseCapture.capture(), any(), any());

        // Create request
        WaitForAvailabilityRequest request = new WaitForAvailabilityRequest();
        request.setVMIP(host);
        request.setVMUserName(user);
        request.setVMPrivateKey(user);

        // Do nothing on connect
        VirtualMachine vm = spy(new VirtualMachine(host, user, key));
        doNothing().when(vm).connect();
        whenNew(VirtualMachine.class).withAnyArguments().thenReturn(vm);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.waitForAvailabilityRequest(request, null);

        // Assert response
        InvokeResponse response = responseCapture.getValue();
        assertEquals("Success", response.getWaitResult());
    }
}
