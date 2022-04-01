package org.opentosca.artifacttemplates.ubuntuvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualMachine {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachine.class);

    private final String host;
    private final int port = 22;
    private final String user;
    private final String key;

    public VirtualMachine(String host, String user, String key) {
        this.host = host;
        this.user = user;
        this.key = key;
    }

    @Override
    public String toString() {
        return user + "@" + host + ":" + port;
    }

    public void awaitAvailability() throws Exception {
        // TODO: testMode
        // TODO: isSSHServiceUp
        // TODO: isSSHLoginPossible

    }

    public String execCommand(String command) throws Exception {
        return SSHHandler.execCommand(toString(), host, port, user, key, command);
    }

    public String replaceHome(String command) throws Exception {
        if (command.contains("~/")) {
            String pwd = execCommand("pwd").trim();

            String replaced;
            if (pwd.endsWith("/")) {
                replaced = command.replaceAll("~/", pwd);
            } else {
                replaced = command.replaceAll("~", pwd);
            }

            LOG.info("Replaced '~' in '{}' with '{}' which results in '{}' on vm '{}'", command, pwd, replaced, this);
            return replaced;
        }
        return command;
    }

    public void uploadFile(String source, String target) throws Exception {
        SSHHandler.uploadFile(toString(), host, port, user, key, source, target);
    }

    public void convertToUnix(String target) throws Exception {
        LOG.info("Converting file '{}' to unix on vm '{}'", target, this);

        if (!target.endsWith(".sh")) {
            LOG.info("Skipping converting file to unix since file '{}' does not end with .sh", target);
            return;
        }

        installPackages("dos2unix");
        execCommand("dos2unix " + target + " " + target);

        LOG.info("Successfully converted file '{}' to unix on vm '{}'", target, this);
    }

    public boolean installPackages(String packages) throws Exception {
        String command = "(sudo apt-get update && sudo apt-get -y install " + packages + ") || (sudo yum update && sudo yum -y install " + packages + ")";
        String output = execCommand(command);
        return output.endsWith("Complete!") || output.endsWith("Nothing to do");
    }
}
