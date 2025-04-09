package com.github.xhea1.party.app.listener;

import com.github.xhea1.partytools.service.listener.DownloadListener;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener for downloading files using {@link DownloadListener}. It provides a progress bar to show the download
 * progress.
 *
 * @author xhea1
 */
@NullMarked
public class ProgressBarListener implements DownloadListener, AutoCloseable {

    private final ProgressBar progressBar;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * @param downloadAmount the total amount of files to be downloaded
     */
    public ProgressBarListener(int downloadAmount) {
        progressBar = ProgressBar.builder().setTaskName("Downloading").setInitialMax(downloadAmount).setStyle(
                ProgressBarStyle.ASCII).build();
    }

    /**
     * Called when a download fails.
     *
     * @param url        the URL of the file being downloaded
     * @param outputPath the path where the file will be saved
     */
    @Override
    public void onFailure(String url, Path outputPath) {
        progressBar.step();
        failureCount.incrementAndGet();
    }

    /**
     * @return the number of successful downloads
     */
    public int getSuccessCount() {
        return successCount.get();
    }

    /**
     * @return the number of failed downloads
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Called when a download succeeds.
     *
     * @param url        the URL of the file being downloaded
     * @param outputPath the path where the file will be saved
     */
    @Override
    public void onSuccess(String url, Path outputPath) {
        progressBar.step();
        successCount.incrementAndGet();
    }


    @Override
    public void close() throws Exception {
        progressBar.close();
    }
}
