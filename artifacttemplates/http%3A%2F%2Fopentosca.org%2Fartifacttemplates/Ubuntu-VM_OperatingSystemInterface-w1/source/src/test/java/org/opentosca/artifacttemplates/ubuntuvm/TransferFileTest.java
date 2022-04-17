
package org.opentosca.artifacttemplates.ubuntuvm;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransferFileTest extends AbstractRequestTest {

    @Test
    public void shouldSucceed() throws Exception {
        File source = Paths.get(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("file")).toURI()).toFile();
        String target = "~/target";

        // Set SSH commands
        sshd.setCommandFactory((channel, command) -> {
            if (command.equals("echo VM availability check")) {
                return new SucceedingCommand(command, "VM availability check");
            }

            if (command.equals("pwd")) {
                return new SucceedingCommand(command, "/");
            }

            return new UnexpectedCommand(command);
        });

        // Create request
        TransferFileRequest request = new TransferFileRequest();
        request.setVMIP(sshd.getHost());
        request.setVMPort(sshd.getPort());
        request.setVMUserName(user);
        request.setVMPrivateKey(key);
        request.setSourceURLorLocalPath(source.getAbsolutePath());
        request.setTargetAbsolutePath(target);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.transferFile(request, null);

        // Assert response
        assertEquals("successful", getResponse().getTransferResult());

        // Assert copied file
        assertTrue(FileUtils.contentEquals(source, getRemoteFile("/target")));
    }
}
