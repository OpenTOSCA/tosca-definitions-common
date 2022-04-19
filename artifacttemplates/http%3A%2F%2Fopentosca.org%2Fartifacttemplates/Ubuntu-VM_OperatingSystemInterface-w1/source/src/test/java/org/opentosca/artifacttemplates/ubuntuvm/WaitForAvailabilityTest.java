package org.opentosca.artifacttemplates.ubuntuvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaitForAvailabilityTest extends AbstractRequestTest {

    @Test
    public void shouldSucceed() {
        // Set SSH commands
        sshd.setCommandFactory((channel, command) -> {
            if (command.equals("echo VM availability check")) {
                return new SucceedingCommand(command, "VM availability check");
            }

            return new UnexpectedCommand(command);
        });

        // Create request
        WaitForAvailabilityRequest request = new WaitForAvailabilityRequest();
        request.setVMIP(host);
        request.setVMPort(port);
        request.setVMUserName(user);
        request.setVMPrivateKey(key);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.waitForAvailabilityRequest(request, null);

        // Assert response
        assertEquals("Success", getResponse().getWaitResult());
    }

}
