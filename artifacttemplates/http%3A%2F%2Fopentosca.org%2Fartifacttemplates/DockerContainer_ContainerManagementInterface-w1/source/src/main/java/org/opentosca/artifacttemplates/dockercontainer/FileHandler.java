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
        if (!file.exists()) {
            return null;
        }
        return file;
    }

    static String downloadFile(URL url, String directory, String filename) throws IOException {
        HttpURLConnection httpConnection = null;
        InputStream inputStream = null;

        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestProperty("Accept", "application/octet-stream");
            inputStream = httpConnection.getInputStream();

            File file = new File(directory, filename);
            FileUtils.copyInputStreamToFile(inputStream, file);
            LOG.info("Created temporary file {}", file);

            return file.toString();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }
}
