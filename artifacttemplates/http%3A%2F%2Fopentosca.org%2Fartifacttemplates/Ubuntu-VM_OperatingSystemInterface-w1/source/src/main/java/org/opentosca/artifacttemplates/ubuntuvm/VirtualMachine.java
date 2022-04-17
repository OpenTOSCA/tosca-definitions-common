package org.opentosca.artifacttemplates.ubuntuvm;

import java.io.File;
import java.io.InputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualMachine {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachine.class);

    private final String host;
    private final int port;
    private final String user;
    private final String key;
    private Session session = null;

    public VirtualMachine(String host, int port, String user, String key) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.key = key;
    }

    public void connect() throws Exception {
        LOG.info("Creating session on VM '{}'", this);

        JSch jsch = new JSch();
        File file = File.createTempFile("key", "tmp", FileUtils.getTempDirectory());
        FileUtils.write(file, key, "UTF-8");
        LOG.info("Created temporary key '{}'", file);

        jsch.addIdentity(file.getAbsolutePath());
        this.session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");

        LOG.info("Checking if VM '{}' is available", this);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 1200000;
        Exception error = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                if (!session.isConnected()) {
                    LOG.info("Connecting to '{}'", this);
                    session.connect();
                }
                execCommand("echo VM availability check");
                LOG.info("VM '{}' is available", this);
                return;
            } catch (Exception e) {
                LOG.warn("Could not check if VM '{}' is available since '{}'", this, e.getMessage());
                error = e;
                sleep();
            }
        }
        if (error != null) {
            throw error;
        }

        FileUtils.forceDelete(file);
        LOG.info("Deleted temporary key '{}'", file);
    }

    public void disconnect() {
        LOG.info("Disconnecting from VM '{}'", this);
        try {
            if (session != null) {
                session.disconnect();
            }
        } catch (Exception e) {
            LOG.warn("Error when disconnecting from VM '{}'", this, e);
        }
    }

    public String execCommand(String command) throws Exception {
        ChannelExec channel = null;
        try {
            LOG.info("Executing command '{}' on VM '{}'", command, this);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            StringBuilder outputBuffer = new StringBuilder();
            InputStream outputStream = channel.getInputStream();

            StringBuilder errorBuffer = new StringBuilder();
            InputStream errorStream = channel.getExtInputStream();

            channel.connect();
            byte[] tmp = new byte[1024];
            int timer = 0;
            while (true) {
                while (outputStream.available() > 0) {
                    int i = outputStream.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i));
                }
                while (errorStream.available() > 0) {
                    int i = errorStream.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errorBuffer.append(new String(tmp, 0, i));
                }
                if (channel.isClosed() && outputStream.available() == 0 && errorStream.available() == 0) {
                    break;
                }
                if (timer++ % 5 == 0) {
                    LOG.info("Still executing command ...");
                }
                //noinspection BusyWait
                Thread.sleep(1000);
            }

            if (!errorBuffer.toString().isEmpty()) {
                LOG.error(errorBuffer.toString());
            }
            if (!outputBuffer.toString().isEmpty()) {
                LOG.info(outputBuffer.toString());
            }

            LOG.info("Command '{}' exited with code '{}' on VM '{}'", command, channel.getExitStatus(), this);

            return outputBuffer.toString().trim();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void uploadFile(String source, String target) throws Exception {
        ChannelSftp channel = null;
        try {
            LOG.info("Uploading file {} to '{}/{}'", source, this, target);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(source, target);

            LOG.info("Successfully uploaded file '{}' to '{}/{}'", source, this, target);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
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

            LOG.info("Replaced '~' in '{}' with '{}' which results in '{}' on VM '{}'", command, pwd, replaced, this);
            return replaced;
        }
        return command;
    }

    public void convertToUnix(String target) throws Exception {
        LOG.info("Converting file '{}' to unix on VM '{}'", target, this);

        if (!target.endsWith(".sh")) {
            LOG.info("Skipping converting file to unix since file '{}' does not end with .sh", target);
            return;
        }

        installPackages("dos2unix");
        execCommand("dos2unix " + target + " " + target);

        LOG.info("Successfully converted file '{}' to unix on VM '{}'", target, this);
    }

    public void installPackages(String packages) throws Exception {
        String command = "(sudo apt-get update && sudo apt-get -y install " + packages + ") || (sudo yum update && sudo yum -y install " + packages + ")";
        String output = execCommand(command);
        boolean success = output.endsWith("Complete!") || output.endsWith("Nothing to do");
        if (!success) throw new Exception("Could not install packages!");
    }

    private void sleep() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            LOG.error("Could not sleep on VM '{}'", this, e);
        }
    }

    public String toString() {
        return user + "@" + host + ":" + port;
    }
}
