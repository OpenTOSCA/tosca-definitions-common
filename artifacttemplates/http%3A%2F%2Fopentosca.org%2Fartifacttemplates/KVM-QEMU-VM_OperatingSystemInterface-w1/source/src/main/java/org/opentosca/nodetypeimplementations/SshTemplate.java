package org.opentosca.nodetypeimplementations;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentosca.nodetypeimplementations.Exceptions.rethrow;

public final class SshTemplate {

    private static final Logger logger = LoggerFactory.getLogger(SshTemplate.class);

    private final String host;
    private final String username;
    private final String password;

    public SshTemplate(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    private SSHClient createClient() {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            client.connect(this.host);
            client.getConnection().getKeepAlive().setKeepAliveInterval(5); // every 60sec
            client.authPassword(this.username, this.password);
        } catch (Exception e) {
            logger.error("Could not connect with SSH client: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        return client;
    }

    public String executeCommand(String command) {
        logger.info("Execute command: {}", command);
        try (SSHClient ssh = createClient();
             Session session = ssh.startSession()) {
            final Command cmd = session.exec(command);
            InputStream output = cmd.getInputStream();
            return IOUtils.readFully(output)
                    .toString()
                    .replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "")
                    .trim();
        } catch (Exception e) {
            logger.error("Error executing command: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void transferFile(String source, String target) {
        logger.info("Transfer file from \"{}\" to \"{}\"", source, target);
        try (SSHClient ssh = createClient()) {
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.put(new FileSystemFile(source), target);
            }
        } catch (Exception e) {
            logger.error("Error transferring file: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Boolean canConnect() {
        RetryCommand<Void> command = new RetryCommand<>(100, TimeUnit.SECONDS.toMillis(1));
        try {
            command.run(rethrow(() -> {
                new Socket(this.host, 22).close();
                return null;
            }));
            return true;
        } catch (RetryCommandException e) {
            return false;
        }
    }

    public Boolean canLogin() {
        RetryCommand<String> command = new RetryCommand<>(100, TimeUnit.SECONDS.toMillis(1));
        try {
            command.run(rethrow(() -> this.executeCommand("pwd")));
            return true;
        } catch (RetryCommandException e) {
            return false;
        }
    }
}
