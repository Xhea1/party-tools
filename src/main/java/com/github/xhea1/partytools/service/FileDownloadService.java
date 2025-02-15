package com.github.xhea1.partytools.service;

import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service class for downloading files with a limit for the amount of concurrent downloads.
 */
class FileDownloadService implements AutoCloseable{
    private final OkHttpClient client;
    private final ExecutorService executor;
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     *
     * @param maxConcurrentDownloads maximum amount of concurrent downloads
     */
    FileDownloadService(int maxConcurrentDownloads) {
        this.client = new OkHttpClient();
        this.executor = Executors.newFixedThreadPool(maxConcurrentDownloads);
    }

    /**
     * Create a completable future for downloading a single file.
     *
     * @param url url to download
     * @param outputPath path to save the file to
     * @return {@link CompletableFuture} for the download
     */
    private CompletableFuture<Path> downloadFile(String url, Path outputPath) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Downloading {} to {}", url, outputPath);
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to download file: " + url + " - " + response);
                }

                assert response.body() != null;
                try (BufferedSink sink = Okio.buffer(Okio.sink(outputPath));
                     InputStream inputStream = response.body().byteStream()) {
                    sink.writeAll(Okio.source(inputStream));
                }

                return outputPath;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Download several files at once
     *
     * @param urls map of url to file name
     * @param outputDir directory to save files to
     * @return {@link CompletableFuture} for the downloads. The amount of concurrent downloads is limited to the global limit.
     */
    CompletableFuture<Void> downloadFiles(Map<String, String> urls, Path outputDir) {

        List<CompletableFuture<Path>> futures = urls.entrySet().stream()
                .map(url -> downloadFile(url.getKey(), outputDir.resolve(url.getValue())))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}

