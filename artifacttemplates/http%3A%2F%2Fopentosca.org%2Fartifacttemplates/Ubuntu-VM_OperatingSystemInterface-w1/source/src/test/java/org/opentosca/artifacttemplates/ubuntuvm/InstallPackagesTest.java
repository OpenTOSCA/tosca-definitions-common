package org.opentosca.artifacttemplates.ubuntuvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstallPackagesTest extends AbstractRequestTest {

    @Test
    public void shouldSucceed() {
        String packages = "packageA packageB";
        String expected = "(sudo apt-get update && sudo apt-get -y install " + packages + ") || (sudo yum update && sudo yum -y install " + packages + ")";

        // Set SSH commands
        sshd.setCommandFactory((channel, command) -> {
            if (command.equals("echo VM availability check")) {
                return new SucceedingCommand(command, "VM availability check");
            }

            if (command.equals(expected)) {
                return new SucceedingCommand(command, "Complete!");
            }

            return new UnexpectedCommand(command);
        });

        // Create request
        InstallPackageRequest request = new InstallPackageRequest();
        request.setVMIP(sshd.getHost());
        request.setVMPort(sshd.getPort());
        request.setVMUserName(user);
        request.setVMPrivateKey(key);
        request.setPackageNames(packages);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.installPackages(request, null);

        // Assert response
        assertEquals("1", getResponse().getInstallResult());
    }

    @Test
    public void shouldFail() {
        String packages = "packageA packageB";
        String expected = "(sudo apt-get update && sudo apt-get -y install " + packages + ") || (sudo yum update && sudo yum -y install " + packages + ")";

        // Set SSH commands
        sshd.setCommandFactory((channel, command) -> {
            if (command.equals("echo VM availability check")) {
                return new SucceedingCommand(command, "VM availability check");
            }

            if (command.equals(expected)) {
                return new FailingCommand(command, "Something went wrong");
            }

            return new UnexpectedCommand(command);
        });

        // Create request
        InstallPackageRequest request = new InstallPackageRequest();
        request.setVMIP(sshd.getHost());
        request.setVMPort(sshd.getPort());
        request.setVMUserName(user);
        request.setVMPrivateKey(key);
        request.setPackageNames(packages);

        // Send request
        UbuntuVMOperatingSystemInterfaceEndpoint endpoint = new UbuntuVMOperatingSystemInterfaceEndpoint();
        endpoint.installPackages(request, null);

        // Assert response
        assertEquals("0", getResponse().getInstallResult());
    }
}
