package com.hw10.app;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  private String serverUrl = "<serverUrl>";
  private String apiKey = "<apiKey>";

  @Bean
  public ElasticsearchClient elasticsearchClient() {
    RestClient restClient = RestClient
        .builder(HttpHost.create(serverUrl))
        .setDefaultHeaders(new Header[]{
            new BasicHeader("Authorization", "ApiKey " + apiKey)
        })
        .build();

    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }
}