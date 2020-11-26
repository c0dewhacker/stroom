package stroom.test;

import stroom.content.ContentPack;
import stroom.content.ContentPackCollection;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class ContentPackDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPackDownloader.class);
    public static final String CONTENT_PACK_DOWNLOAD_DIR = "~/.stroom/contentPackDownload";
//    private static final String CONTENT_PACK_IMPORT_DIR = "~/.stroom/contentPackImport";

    private static void download(final String url, final Path file) throws IOException {
        try (final InputStream in = new URL(url).openStream()) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void download(final ContentPack contentPack,
                                 final Path contentPackDownloadDir,
                                 final Path contentPackImportDir) {
        try {
            final String url = contentPack.getUrl();
            final String filename = Paths.get(new URI(url).getPath()).getFileName().toString();
            final Path downloadFile = contentPackDownloadDir.resolve(filename);
            final Path importFile = contentPackImportDir.resolve(filename);
            if (Files.isRegularFile(downloadFile)) {
                LOGGER.info(url + " has already been downloaded");
            } else {
                LOGGER.info("Downloading " + url + " into " + FileUtil.getCanonicalPath(contentPackDownloadDir));
                download(url, downloadFile);
            }

            if (!Files.isRegularFile(importFile)) {
                LOGGER.info("Copying from " + downloadFile + " to " + importFile);
                StreamUtil.copyFile(downloadFile, importFile);
            }
        } catch (final IOException | URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void downloadPacks(final Path contentPacksDefinition,
                                     final Path contentPackDownloadDir,
                                     final Path contentPackImportDir) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ContentPackCollection contentPacks = mapper.readValue(contentPacksDefinition.toFile(), ContentPackCollection.class);
            contentPacks.getContentPacks().forEach(contentPack ->
                    download(contentPack, contentPackDownloadDir, contentPackImportDir));
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Path downloadContentPack(final ContentPack contentPack, final Path destDir) {
        return downloadContentPack(contentPack, destDir, ConflictMode.KEEP_EXISTING);
    }

    public static Path downloadContentPack(final ContentPack contentPack,
                                           final Path destDir,
                                           final ConflictMode conflictMode) {
        Preconditions.checkNotNull(contentPack);
        Preconditions.checkNotNull(destDir);
        Preconditions.checkNotNull(conflictMode);
        Preconditions.checkArgument(Files.isDirectory(destDir));

        Path destFilePath = buildDestFilePath(contentPack, destDir);
        boolean destFileExists = Files.isRegularFile(destFilePath);

        if (destFileExists && conflictMode.equals(ConflictMode.KEEP_EXISTING)) {
            LOGGER.debug("Requested contentPack {} already exists in {}, keeping existing",
                    contentPack.getName(),
                    FileUtil.getCanonicalPath(destFilePath));
            return destFilePath;
        }

        if (destFileExists && conflictMode.equals(ConflictMode.OVERWRITE_EXISTING)) {
            LOGGER.debug("Requested contentPack {} already exists in {}, overwriting existing",
                    contentPack.getName(),
                    FileUtil.getCanonicalPath(destFilePath));
            try {
                Files.delete(destFilePath);
            } catch (final IOException e) {
                throw new UncheckedIOException(String.format("Unable to remove existing content pack %s",
                        FileUtil.getCanonicalPath(destFilePath)), e);
            }
        }

        final URL fileUrl = getUrl(contentPack);
        LOGGER.info("Downloading contentPack {} from {} to {}",
                contentPack.getName(),
                fileUrl.toString(),
                FileUtil.getCanonicalPath(destFilePath));

        downloadFile(fileUrl, destFilePath);

        return destFilePath;
    }

    private static URL getUrl(final ContentPack contentPack) {
        try {
            return new URL(contentPack.getUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Url " +
                    contentPack.getUrl() +
                    " for content pack " +
                    contentPack.getName() +
                    " and version " +
                    contentPack.getVersion() +
                    " is badly formed", e);
        }
    }

    static Path buildDestFilePath(final ContentPack contentPack, final Path destDir) {
        final String filename = contentPack.toFileName();
        return destDir.resolve(filename);
    }

    private static boolean isRedirected(Map<String, List<String>> header) {
        for (String hv : header.get(null)) {
            if (hv.contains(" 301 ")
                    || hv.contains(" 302 ")) return true;
        }
        return false;
    }

    static void downloadFile(final URL fileUrl, final Path destFilename) {
        URL effectiveUrl = fileUrl;
        try {
            HttpURLConnection http = (HttpURLConnection) effectiveUrl.openConnection();
            Map<String, List<String>> header = http.getHeaderFields();
            while (isRedirected(header)) {
                effectiveUrl = new URL(header.get("Location").get(0));
                http = (HttpURLConnection) effectiveUrl.openConnection();
                header = http.getHeaderFields();
            }

            // Create a temp file as the download destination to avoid overwriting an existing file.
            final Path tempFile = Files.createTempFile("stroom", "download");
            try (final OutputStream fos = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                StreamUtil.streamToStream(http.getInputStream(), fos);
            }

            // Atomically move the downloaded file to the destination so that concurrent tests don't overwrite the file.
            Files.move(tempFile, destFilename);
        } catch (final IOException e) {
            throw new UncheckedIOException(String.format("Error downloading url %s to %s",
                    fileUrl.toString(), FileUtil.getCanonicalPath(destFilename)), e);
        }
    }

    public enum ConflictMode {
        OVERWRITE_EXISTING,
        KEEP_EXISTING
    }
}
