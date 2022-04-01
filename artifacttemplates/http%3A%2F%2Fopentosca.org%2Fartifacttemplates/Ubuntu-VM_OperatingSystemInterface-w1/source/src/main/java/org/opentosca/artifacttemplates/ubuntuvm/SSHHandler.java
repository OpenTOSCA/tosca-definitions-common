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

public class SSHHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FileHandler.class);

    public static void uploadFile(String host, int port, String user, String key, String source, String target) throws Exception {
        ChannelSftp channel = null;
        Session session = null;
        try {
            session = createSession(host, port, user, key);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.put(source, target);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }

            if (session != null) {
                session.disconnect();
            }
        }
    }

    public static String execCommand(String host, int port, String user, String key, String command) throws Exception {
        ChannelExec channel = null;
        Session session = null;
        try {
            LOG.info("Executing command: '{}'", command);
            session = createSession(host, port, user, key);
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

            LOG.info("Command '{}' exited with code '{}'", command, channel.getExitStatus());

            return outputBuffer.toString().trim();
        } catch (Exception e) {
            throw e;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }

            if (session != null) {
                session.disconnect();
            }
        }
    }

    private static Session createSession(String host, int port, String user, String key) {
        try {
            JSch jsch = new JSch();
            File file = File.createTempFile("key", "tmp", FileUtils.getTempDirectory());
            FileUtils.write(file, key, "UTF-8");
            LOG.info("Tmp key file created: {}", file.exists());

            jsch.addIdentity(file.getAbsolutePath());

            Session session = jsch.getSession(host, user, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            FileUtils.forceDelete(file);
            LOG.info("tmp key file deleted: {}", file.exists());

            return session;
        } catch (Exception e) {
            LOG.error("Failed to connect to {} using user {}.", host, user, e);
            throw new RuntimeException(e);
        }
    }
}
