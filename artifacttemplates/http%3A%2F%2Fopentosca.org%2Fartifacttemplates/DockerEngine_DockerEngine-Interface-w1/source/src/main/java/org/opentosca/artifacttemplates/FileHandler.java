package org.opentosca.artifacttemplates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;

public class FileHandler {

    protected static File downloadFile(final String url) {
        try {
            final URI dockerImageURI = new URI(url);

            final String[] pathSplit = dockerImageURI.getRawPath().split("/");
            final String fileName = pathSplit[pathSplit.length - 1];

            final File tempDir = Files.createTempDir();
            final File tempFile = new File(tempDir, fileName);

            downloadFile(dockerImageURI, tempFile);
            return tempFile;
        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected static void downloadFile(URI dockerImageURI, File tempFile) throws IOException {
        final URLConnection connection = dockerImageURI.toURL().openConnection();
        connection.setRequestProperty("Accept", "application/octet-stream");

        try (final InputStream input = connection.getInputStream()) {
            final byte[] buffer = new byte[4096];
            int n;

            try (final OutputStream output = new FileOutputStream(tempFile)) {
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
        }
    }

    protected static File createTempTarFromFile(final File file) {
        final TarArchiveEntry entry = new TarArchiveEntry(file, file.getName());

        File tarArchive = null;
        try {
            tarArchive = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tar");
            final TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(tarArchive));
            out.putArchiveEntry(entry);
            IOUtils.copy(new FileInputStream(file), out);
            out.closeArchiveEntry();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return tarArchive;
    }
}
