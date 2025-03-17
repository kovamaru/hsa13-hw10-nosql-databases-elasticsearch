package com.hw10.app;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.hw10.app.StartupRunner.WordDoc;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

  private final ElasticsearchClient client;
  private static final String INDEX_NAME = "words_autocomplete";

  @GetMapping
  public List<String> search(
      @RequestParam("query") String query,
      @RequestParam(name = "size", defaultValue = "10") int size
  ) throws Exception {
    if (query == null || query.isEmpty()) {
      return List.of();
    }

    SearchRequest sr = SearchRequest.of(s -> s
        .index(INDEX_NAME)
        .size(size)
        .query(q -> q.match(m -> {
          m.field("word");

          if (query.length() <= 7) {
            m.query(FieldValue.of(query));
            m.fuzziness("AUTO");
          } else {
            m.query(FieldValue.of(query));
            m.minimumShouldMatch("75%");
          }

          return m;
        }))
    );

    SearchResponse<WordDoc> response = client.search(sr, WordDoc.class);

    List<String> results = new ArrayList<>();
    for (Hit<WordDoc> hit : response.hits().hits()) {
      results.add(hit.source().word());
    }
    return results;
  }
}
