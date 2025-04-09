package com.github.xhea1.partytools.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xhea1.partytools.model.FileRecord;
import com.github.xhea1.partytools.model.PostRecord;
import com.github.xhea1.partytools.service.listener.DownloadListener;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service for making HTTP requests and dynamically parsing JSON responses. This service is specifically designed to
 * query the `/search_hash/{file_hash}` endpoint and extract the `posts` data, excluding nested fields like `file` and
 * `attachments`.
 */
@NullMarked
public class PartyHTTPService {
    private static final String DOWNLOAD_SUBPATH = "/data/";
    private static final String API_SUBPATH = "api/v1";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;


    /**
     * Constructs an HTTPService with the specified base URL.
     *
     * @param baseUrl The base URL for the API endpoint. Usually either {@code https://coomer.su/} or
     *                {@code https://kemono.su/}
     */
    public PartyHTTPService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @param post JSON Node for the post
     * @return {@link PostRecord}
     */
    private PostRecord getPostRecord(JsonNode post) {
        Optional<FileRecord> file = Optional.empty();
        var fileNode = post.get("file");
        if (!fileNode.isEmpty()) {
            file = Optional.of(getFileRecord(fileNode));
        }
        List<FileRecord> attachments = new ArrayList<>();
        for (JsonNode attachment : post.get("attachments")) {
            attachments.add(getFileRecord(attachment));
        }
        return new PostRecord(post.get("file_id")
                                      .asInt(), post.get("id")
                                      .asText(), post.get("user")
                                      .asText(), post.get("service")
                                      .asText(), post.get("title")
                                      .asText(), post.get("published")
                                      .asText(), post.get("substring")
                                      .asText(), file, attachments);
    }

    /**
     * @param fileNode JSON node
     * @return {@link FileRecord}
     */
    private FileRecord getFileRecord(JsonNode fileNode) {
        return new FileRecord(fileNode.get("name")
                                      .asText(), fileNode.get("path")
                                      .asText());
    }

    /**
     * Get all posts for an SHA-2/SHA-256 hash.
     *
     * @param fileHash The hash used to query the endpoint.
     * @return A list of {@link PostRecord} containing all found posts.
     * @throws IOException If the request fails or the response is invalid.
     * @implNote Retrieves the `posts` data from the `/search_hash/{file_hash}` endpoint.
     */
    @NotNull
    public List<PostRecord> getPostsByHash(String fileHash) throws IOException {
        if (Strings.isNullOrEmpty(fileHash)) {
            throw new IllegalArgumentException("fileHash must not be null");
        }
        String url = baseUrl + API_SUBPATH + "/search_hash/" + fileHash;

        return executeQueryForPosts(url);
    }

    /**
     * Execute a query which returns post data.
     *
     * @param url url to query
     * @return A list of {@link PostRecord} containing the `posts` data.
     * @throws IOException If the request fails or the response is invalid.
     */
    private List<PostRecord> executeQueryForPosts(String url) throws IOException {
        // Create HTTP GET request
        Request request = new Request.Builder().url(url)
                .build();
        List<PostRecord> postRecords = new ArrayList<>();
        try (Response response = client.newCall(request)
                .execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body()
                        .string();
                JsonNode rootNode = objectMapper.readTree(jsonResponse);

                // Extract `posts` array
                JsonNode posts = rootNode.get("posts");
                if (posts != null && posts.isArray()) {
                    for (JsonNode post : posts) {
                        PostRecord postRecord = getPostRecord(post);
                        postRecords.add(postRecord);
                    }
                }
            }
        }
        return postRecords;
    }

    /**
     * Get all posts of the given user.
     *
     * @param service   service to fetch data from. Values include {@code fansly}, {@code onlyfans}, {@code patreon} and
     *                  others.
     * @param creatorId the ID of the creator
     * @return @return A list of {@link PostRecord} containing all found posts.
     * @throws IOException If the request fails or the response is invalid.
     */
    public List<PostRecord> getPostsForUser(String service, String creatorId) throws IOException {
        if (Strings.isNullOrEmpty(service)) {
            throw new IllegalArgumentException("service must not be null");
        }
        if (Strings.isNullOrEmpty(creatorId)) {
            throw new IllegalArgumentException("creatorId must not be null");
        }
        String url = baseUrl + API_SUBPATH + service + "/user/" + creatorId;
        return executeQueryForPosts(url);
    }

    /**
     * Download the given files.
     *
     * @param records                files to download
     * @param downloadDir            directory to download to
     * @param maxConcurrentDownloads maximum amount of concurrent downloads
     */
    public void downloadFiles(Collection<FileRecord> records, Path downloadDir, int maxConcurrentDownloads, @Nullable DownloadListener listener) {
        try (var service = new FileDownloadService(maxConcurrentDownloads, listener)) {
            var download = service.downloadFiles(records.stream()
                                                         .collect(Collectors.toMap(
                                                                 fileRecord -> createDownloadUrl(fileRecord.path()),
                                                                 FileRecord::name)), downloadDir);
            download.join();
        }
    }

    /**
     * Create the URL for a download
     */
    private String createDownloadUrl(String path) {
        return baseUrl + DOWNLOAD_SUBPATH + path;
    }
}
