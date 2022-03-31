package org.opentosca.artifacttemplates.ubuntuvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualMachine {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachine.class);

    private final String hostname;
    private final String user;
    private final String credentials;

    public VirtualMachine(String hostname, String user, String credentials) {
        this.hostname = hostname;
        this.user = user;
        this.credentials = credentials;
    }

    public void awaitAvailability() throws InterruptedException {
        // TODO: testMode
        // TODO: isSSHServiceUp
        // TODO: isSSHLoginPossible

    }

    public String execCommand(String command) throws InterruptedException {
        // TODO: execCommand
        return "".trim();
    }

    public String replaceHome(String command) throws InterruptedException {
        if (command.contains("~/")) {
            String pwd = execCommand("pwd").trim();

            String replaced;
            if (pwd.endsWith("/")) {
                replaced = command.replaceAll("~/", pwd);
            } else {
                replaced = command.replaceAll("~", pwd);
            }

            LOG.info("Replaced '~' in '{}' with '{}' which results in '{}'", command, pwd, replaced);
            return replaced;
        }
        return command;
    }

    public void uploadFile(String source, String target) throws InterruptedException {
        // TODO: upload file
    }

    public void convertToUnix(String target) throws InterruptedException {
        LOG.info("Converting file '{}' to unix on vm", target);

        if (!target.endsWith(".sh")) {
            LOG.info("Skipping converting file to unix since file '{}' does not end with .sh", target);
            return;
        }

        installPackages("dos2unix");
        execCommand("dos2unix " + target + " " + target);

        LOG.info("Successfully converted file '{}' to unix on vm", target);
    }

    public boolean installPackages(String packages) throws InterruptedException {
        String command = "(sudo apt-get update && sudo apt-get -y install " + packages + ") || (sudo yum update && sudo yum -y install " + packages + ")";
        String output = execCommand(command);
        return output.endsWith("Complete!") || output.endsWith("Nothing to do");
    }
}
