package org.opentosca.artifacttemplates.ubuntuvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
abstract public class AbstractRequestTest {

    static SshServer sshd;
    static String user = "user";
    static String host = "127.0.0.1";
    static int port;
    static String key;

    static String messageId = "messageId";
    static String replyTo = "replyTo";
    static OpenToscaHeaders headers = new OpenToscaHeaders(messageId, replyTo, Collections.emptyMap());
    static ArgumentCaptor<InvokeResponse> responseCapture;

    static MockedStatic<SoapUtil> soapUtil;

    static InvokeResponse getResponse() {
        return responseCapture.getValue();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        // Mock SoapUtils
        soapUtil = mockStatic(SoapUtil.class);
        soapUtil.when(() -> SoapUtil.encode(any())).thenCallRealMethod();

        // Return current headers
        soapUtil.when(() -> SoapUtil.parseHeaders(any())).then(invocationOnMock -> headers);

        // Intercept response
        responseCapture = ArgumentCaptor.forClass(InvokeResponse.class);
        soapUtil.when(() -> SoapUtil.sendSoapResponse(responseCapture.capture(), any(), any())).then(invocationOnMock -> null);

        // Read private key
        key = IOUtils.toString(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream("key")),
                StandardCharsets.UTF_8
        );

        // Start SSH server
        sshd = SshServer.setUpDefaultServer();
        sshd.setHost(host);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("key.pub")).toURI())));
        sshd.start();

        // Store random allocated port
        port = sshd.getPort();
    }

    @AfterAll
    static void afterAll() throws Exception {
        sshd.stop();
        soapUtil.close();
    }

    public class ExpectedCommand implements Command, Runnable {

        protected InputStream in;
        protected OutputStream out;
        protected OutputStream err;

        private final String command;
        private ExitCallback callback;

        private final String returnValue;
        private final int exitValue;

        public ExpectedCommand(String command, String output, int code) {
            this.command = ValidateUtils.checkNotNullAndNotEmpty(command, "Received command: " + command);
            this.returnValue = output;
            this.exitValue = code;
        }

        public String getCommand() {
            return command;
        }

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            String message = getReturnValue();
            OutputStream output = getStream();
            writeToStream(message, output);
        }

        private void writeToStream(String message, OutputStream output) {
            try {
                try {
                    output.write(message.getBytes(StandardCharsets.UTF_8));
                    output.write('\n');
                } finally {
                    output.flush();
                }
            } catch (IOException e) {
                // ignored
            }

            if (callback != null) {
                callback.onExit(getExitValue());
            }
        }

        @Override
        public void start(ChannelSession channel, Environment env) {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void destroy(ChannelSession channel) {
            // ignored
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getCommand());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            return Objects.equals(this.getCommand(), ((ExpectedCommand) obj).getCommand());
        }

        @Override
        public String toString() {
            return getReturnValue();
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getReturnValue() {
            return this.returnValue;
        }

        public OutputStream getStream() {
            if (exitValue == 1) return this.err;
            return this.out;
        }
    }

    public class UnexpectedCommand extends ExpectedCommand {
        public UnexpectedCommand(String command) {
            super(command, "Unknown command", 1);
        }
    }
}
