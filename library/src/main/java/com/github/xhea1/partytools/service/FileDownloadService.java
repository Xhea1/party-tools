package com.github.xhea1.partytools.service;

import com.github.xhea1.partytools.service.listener.DownloadListener;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Service class for downloading files with a limit for the amount of concurrent downloads.
 */
@NullMarked
class FileDownloadService implements AutoCloseable {
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final DownloadListener listener;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DownloadListener NO_OP_LISTENER = new DownloadListener() {
        @Override
        public void onFailure(String url, Path outputPath) {
            // no op
        }

        @Override
        public void onSuccess(String url, Path outputPath) {
           // no op
        }
    };

    /**
     *
     * @param maxConcurrentDownloads maximum amount of concurrent downloads
     * @param listener optional listener for download events
     */
    FileDownloadService(int maxConcurrentDownloads, @Nullable DownloadListener listener) {
        this.listener = Optional.ofNullable(listener).orElse(NO_OP_LISTENER);
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

                listener.onSuccess(url, outputPath);
                return outputPath;
            } catch (IOException e) {
                listener.onFailure(url, outputPath);
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

