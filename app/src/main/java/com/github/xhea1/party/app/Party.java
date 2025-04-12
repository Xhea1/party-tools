package com.github.xhea1.party.app;

import com.github.xhea1.party.app.listener.ProgressBarListener;
import com.github.xhea1.party.app.util.TableFormatter;
import com.github.xhea1.partytools.model.CreatorRecord;
import com.github.xhea1.partytools.model.FileRecord;
import com.github.xhea1.partytools.model.PostRecord;
import com.github.xhea1.partytools.service.PartyHTTPService;
import com.google.common.base.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Party commandline app.
 */
@NullMarked
@CommandLine.Command(name = "party", description = "Tool for interacting with party services.",
                     mixinStandardHelpOptions = true, version = "0.1", subcommands = {Party.PartyDownload.class, Party.PartySearch.class})
class Party {

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Party());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @SuppressWarnings("unused")
    enum Site {
        COOMER("https://coomer.su/"), KEMONO("https://kemono.su/");

        private final String baseUrl;

        Site(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * Download subcommand
     */
    @CommandLine.Command(name = "download")
    static class PartyDownload implements Callable<Integer> {

        private static final Logger LOGGER = LogManager.getLogger();

        @CommandLine.Option(names = "-creator", required = true, description = "ID of the creator to download.")
        String creator;

        @CommandLine.Option(names = "-site", required = true,
                            description = "Site to download from. Available choices: ${COMPLETION-CANDIDATES}")
        Site site;

        @CommandLine.Option(names = "-service", required = true,
                            description = "Service to download from, e.g. fansly, onlyfans, patreon, discord, etc.")
        String service;

        @CommandLine.Option(names = "-output",
                            description = "Output directory for the downloaded files. Defaults to the current directory.")
        Path outputDir = Path.of(".");

        @CommandLine.Option(names = {"-c", "-concurrent"}, description = "Maximum number of concurrent downloads. Defaults to ${DEFAULT-VALUE}.")
        int maxConcurrentDownloads = 5;

        @Override
        public Integer call() throws Exception {
            PartyHTTPService partyHTTPService = new PartyHTTPService(site.baseUrl);
            LOGGER.info("Downloading posts from user {} for service {}...", creator, site.name());
            try {
                List<PostRecord> postsForUser = partyHTTPService.getPostsForUser(service, creator);
                Set<FileRecord> filesToDownload = HashSet.newHashSet(postsForUser.size());
                postsForUser.forEach(post -> {
                    post.file()
                            .ifPresent(filesToDownload::add);
                    filesToDownload.addAll(post.attachments());
                });
                int size = filesToDownload.size();
                LOGGER.info("Found {} posts with {} files.", postsForUser.size(), size);
                int successfulDownloads = 0;
                int failedDownloads = 0;
                try(ProgressBarListener listener = new ProgressBarListener(size)) {
                    partyHTTPService.downloadFiles(filesToDownload, outputDir, maxConcurrentDownloads, listener);
                    successfulDownloads = listener.getSuccessCount();
                    failedDownloads = listener.getFailureCount();
                }
                LOGGER.info("All files downloaded.");
                LOGGER.info("Successful downloads: {}", successfulDownloads);
                LOGGER.info("Failed downloads: {}", failedDownloads);
                return 0;
            } catch (Exception e) {
                LOGGER.error("Error during download: ", e);
                return 1;
            }
        }
    }

    @CommandLine.Command(name = "search", description = "Search for a creator.")
    static class PartySearch implements Callable<Integer> {

        private static final Logger LOGGER = LogManager.getLogger();

        @CommandLine.Option(names = "-creator", required = true, description = "Name of the creator to search for.")
        String creator;

        @CommandLine.Option(names = "-site", required = true,
                            description = "Site to download from. Available choices: ${COMPLETION-CANDIDATES}")
        Site site;

        @CommandLine.Option(names = "-service",
                            description = "Optionally filter creators by service, e.g. fansly, onlyfans, patreon, discord, etc.")
        String service;

        @Override
        public Integer call() throws Exception {
            PartyHTTPService partyHTTPService = new PartyHTTPService(site.baseUrl);
            Set<CreatorRecord> creators = partyHTTPService.getCreators();
            if (!Strings.isNullOrEmpty(service)) {
                creators = creators.stream()
                        .filter(c -> c.service().equalsIgnoreCase(service))
                        .collect(Collectors.toSet());
            }
            creators = creators.stream()
                    .filter(c -> c.name().equalsIgnoreCase(creator))
                    .collect(Collectors.toSet());
            LOGGER.info("Found {} creators: ", creators.size());
            if(!creators.isEmpty()) {
                LOGGER.info("\n" + TableFormatter.formatCreators(creators));
            }
            return 0;
        }
    }
}
