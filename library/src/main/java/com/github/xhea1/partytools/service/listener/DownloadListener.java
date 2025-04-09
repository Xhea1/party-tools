package com.github.xhea1.partytools.service.listener;

import java.nio.file.Path;

/**
 * Listener for download events.
 *
 * @author xhea1
 */
public interface DownloadListener {

    /**
     * Called when a download fails.
     *
     * @param url the URL of the file being downloaded
     * @param outputPath the path where the file will be saved
     */
    void onFailure(String url, Path outputPath);
    /**
     * Called when a download succeeds.
     *
     * @param url the URL of the file being downloaded
     * @param outputPath the path where the file will be saved
     */
    void onSuccess(String url, Path outputPath);
}
