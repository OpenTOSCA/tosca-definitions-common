package org.opentosca.artifacttemplates.dockercontainer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerContainerTest {

    @ParameterizedTest
    // These inputs are used to validate that whitespaces do not break the replacement of ~ with the current directory.
    @ValueSource(strings = {"", "/", " \t            \t ", "    /    ", "\n\t  /"})
    void replaceHome(String pwdResponse) throws InterruptedException {
        DockerContainer dockerContainer = new DockerContainer("tcp://localhost:2375", null, "2a") {
            @Override
            public String execCommand(String command) {
                return pwdResponse;
            }
        };

        String expectedOutput = "/test/new/dir/file.sh";

        String replacedHome = dockerContainer.replaceHome("~" + expectedOutput);

        assertEquals(expectedOutput, replacedHome);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "false;2",
            "\t;2",
            "[installed];1",
            "installed;2",
            "test test test\t installed test\t test;2",
            "test test test\t [not installed] test\t test;2",
            "[not installed];2",
            "test test test\t [installed] test\t test;1",
            "[not installed, local];2"
    }, delimiter = ';')
    void ensurePackage(String packageInstallResult, int expectedExecCommandCalled) throws InterruptedException {
        int[] calledExecCommand = {0};

        DockerContainer dockerContainer = new DockerContainer("tcp://localhost:2375", null, "2a") {
            @Override
            public String execCommand(String command) {
                calledExecCommand[0]++;
                return packageInstallResult == null ? "" : packageInstallResult;
            }
        };

        dockerContainer.ensurePackage("packageToInstall");

        assertEquals(expectedExecCommandCalled, calledExecCommand[0]);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file,false",
            "file.sh,true"
    })
    void convertToUnixTest(String fileName, boolean execCommandShouldBeCalled) throws InterruptedException {
        String[] passedCommand = new String[1];
        boolean[] calledExecCommand = {false};

        DockerContainer dockerContainer = new DockerContainer("tcp://localhost:2375", null, "2a") {
            @Override
            public String execCommand(String command) {
                passedCommand[0] = command;
                calledExecCommand[0] = true;

                return command;
            }
        };

        dockerContainer.convertToUnix(fileName);

        assertEquals(execCommandShouldBeCalled, calledExecCommand[0]);

        if (execCommandShouldBeCalled) {
            assertEquals("dos2unix " + fileName + " " + fileName, passedCommand[0]);
        }
    }
}
