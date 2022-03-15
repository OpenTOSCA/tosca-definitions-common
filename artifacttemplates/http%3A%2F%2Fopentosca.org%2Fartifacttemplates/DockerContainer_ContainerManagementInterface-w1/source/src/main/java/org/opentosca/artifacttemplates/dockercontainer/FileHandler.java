package org.opentosca.artifacttemplates.dockercontainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FileHandler.class);

    static URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static File getFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    static String downloadFile(URL url, String directory, String filename) throws IOException {
        HttpURLConnection connection = null;
        InputStream stream = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/octet-stream");
            stream = connection.getInputStream();

            File file = new File(directory, filename);
            FileUtils.copyInputStreamToFile(stream, file);
            LOG.info("Downloaded file '{}' to temporary file '{}'. Didn't I? {}", url, file, file.exists());

            return file.toString();
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
