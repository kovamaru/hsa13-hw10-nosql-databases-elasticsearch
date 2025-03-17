package com.hw10.app;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final ElasticsearchClient client;

    private static final String INDEX_NAME = "words_autocomplete";

    @Value("classpath:words.txt")
    private Resource wordsFile;

    @Override
    public void run(String... args) throws Exception {
        if (!indexExists()) {
            deleteIndex();
            createIndex();
            bulkIndexWords(loadWordsFromFile());
        }
    }

    private boolean indexExists() throws Exception {
        BooleanResponse exists = client.indices().exists(e -> e.index(INDEX_NAME));
        return exists.value();
    }

    private void deleteIndex() throws Exception {
        client.indices().delete(d -> d.index(INDEX_NAME));
        log.info("Deleted existing index: {}", INDEX_NAME);
    }

    private void createIndex() throws Exception {
        client.indices().create(c -> c
            .index(INDEX_NAME)
            .settings(s -> s
                .analysis(a -> a
                    .filter("autocomplete_filter", filterBuilder ->
                        filterBuilder.definition(tokenFilterDef ->
                            tokenFilterDef.edgeNgram(edge -> edge
                                .minGram(1)
                                .maxGram(20)
                            )
                        )
                    )
                    .analyzer("autocomplete_analyzer", an -> an
                        .custom(ca -> ca
                            .tokenizer("standard")
                            .filter("lowercase", "autocomplete_filter")
                        )
                    )
                )
            )
            .mappings(m -> m
                .properties("word", prop -> prop
                    .text(t -> t.analyzer("autocomplete_analyzer"))
                )
            )
        );
        log.info("Created index: {}", INDEX_NAME);
    }

    private List<String> loadWordsFromFile() throws Exception {
        List<String> result = new ArrayList<>();
        if (!wordsFile.exists()) {
            log.warn("Resource not found: {}", wordsFile);
            return result;
        }
        try (var is = wordsFile.getInputStream();
             var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isBlank()) {
                    result.add(trimmed);
                }
            }
        }
        log.info("Loaded {} words from file.", result.size());
        return result;
    }

    private void bulkIndexWords(List<String> words) throws Exception {
        if (words.isEmpty()) {
            log.info("No words found to index!");
            return;
        }
        List<BulkOperation> ops = new ArrayList<>();
        for (String w : words) {
            ops.add(BulkOperation.of(b -> b
                .index(idx -> idx
                    .index(INDEX_NAME)
                    .document(new WordDoc(w))
                )
            ));
        }
        BulkResponse bulkResp = client.bulk(BulkRequest.of(b -> b.operations(ops)));
        if (bulkResp.errors()) {
            log.error("Bulk indexing had errors...");
        } else {
            log.info("Indexed {} words into {}", ops.size(), INDEX_NAME);
        }
    }

  public record WordDoc(String word) {}
}
