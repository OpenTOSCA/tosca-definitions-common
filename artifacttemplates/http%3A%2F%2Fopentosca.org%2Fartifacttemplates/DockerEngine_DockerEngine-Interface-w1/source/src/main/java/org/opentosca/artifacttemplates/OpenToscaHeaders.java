package org.opentosca.artifacttemplates;

import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public record OpenToscaHeaders(String messageId,
                               String replyTo,
                               Map<QName, Map<String, String>> deploymentArtifacts) {

    @Override
    public String toString() {
        String daString = deploymentArtifacts.entrySet().stream()
                .map(entry -> "\n    " + entry.getKey() + " = {"
                        + entry.getValue().entrySet().stream()
                        .map(values -> "\n      '" + values.getKey() + "': '" + values.getValue() + "'")
                        .collect(Collectors.joining(","))
                ).collect(Collectors.joining(","));

        return """
        OpenToscaHeaders {
          messageId = '%s',
          replyTo   = '%s',
          deploymentArtifacts = {%s
          }
        }""".formatted(messageId, replyTo, daString);
    }
}
