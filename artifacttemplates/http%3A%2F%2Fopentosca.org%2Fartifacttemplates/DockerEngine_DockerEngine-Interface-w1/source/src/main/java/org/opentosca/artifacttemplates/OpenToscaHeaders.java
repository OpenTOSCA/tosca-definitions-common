package org.opentosca.artifacttemplates;

import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public record OpenToscaHeaders(String messageId,
                               String replyTo,
                               Map<QName, Map<String, String>> deploymentArtifacts) {

    @Override
    public String toString() {
        String daString = deploymentArtifacts == null ? "" :
                deploymentArtifacts.entrySet().stream()
                        .map(this::getDaTypeString)
                        .collect(Collectors.joining(","));

        return """
                OpenToscaHeaders {
                  messageId = '%s',
                  replyTo   = '%s',
                  deploymentArtifacts = {%s
                  }
                }""".formatted(messageId, replyTo, daString);
    }

    private String getDaTypeString(Map.Entry<QName, Map<String, String>> entry) {
        return "\n    '" + entry.getKey() + "': {" + getDaMapString(entry.getValue()) + "\n    }";
    }

    private String getDaMapString(Map<String, String> map) {
        return map.entrySet().stream()
                .map(values -> "\n      '" + values.getKey() + "': '" + values.getValue() + "'")
                .collect(Collectors.joining(","));
    }
}
