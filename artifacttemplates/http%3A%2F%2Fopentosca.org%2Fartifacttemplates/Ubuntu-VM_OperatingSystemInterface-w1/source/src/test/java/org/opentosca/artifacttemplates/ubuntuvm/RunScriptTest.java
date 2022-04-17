package org.opentosca.artifacttemplates.ubuntuvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunScriptTest extends AbstractRequestTest {

    @Test
    public void shouldSucceed() {
        String input = "input";
        String output = "output";

        // Set SSH commands
        sshd.setCommandFactory((channel, command) -> {
            if (command.equals("echo VM availability check")) {
                return new ExpectedCommand(command, "VM availability check", 0);
            }

            if (command.equals(input)) {
                return new ExpectedCommand(command, output, 0);
            }

            return new UnexpectedCommand(command);
        });

        // Create request
        RunScriptRequest request = new RunScriptRequest();
        request.setVMIP(sshd.getHost());
        request.setVMPort(sshd.getPort());
        request.setVMUserName(user);
        request.setVMPrivateKey(key);
        request.setScript(input);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.runScript(request, null);

        // Assert response
        assertEquals(output, getResponse().getScriptResult());
    }
}
