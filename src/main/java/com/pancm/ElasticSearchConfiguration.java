package com.pancm;


import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
/***
 * *   ____  ___________  ___________           ________   ____  __.  _____ _____.___.
 * *   \   \/  /\______ \ \_   _____/           \_____  \ |    |/ _| /  _  \\__  |   |
 * *    \     /  |    |  \ |    __)     ______   /   |   \|      <  /  /_\  \/   |   |
 * *    /     \  |    `   \|     \     /_____/  /    |    \    |  \/    |    \____   |
 * *   /___/\  \/_______  /\___  /              \_______  /____|__ \____|__  / ______|
 * *    	 \_/        \/     \/                       \/        \/       \/\/
 * */
public class ElasticSearchConfiguration {
    private String esAddress;

    private String userEsAddress;

    private String supervisoryControlEsAddress;

    private static final int CONNECT_TIME_OUT = 30000;
    private static final int SOCKET_TIME_OUT = 30000;
    private static final int CONNECTION_REQUEST_TIME_OUT = 20000;
    private static final int MAX_CONNECT_NUM = 500;
    private static final int MAX_CONNECT_PER_ROUTE = 500;

    /**
     * 资源、商品相关
     */
    public RestHighLevelClient highLevelClient() {
        return buildHighLevelClient(esAddress);
    }

    /**
     * 用户独享
     */
    public RestHighLevelClient userHighLevelClient() {
        return buildHighLevelClient(userEsAddress);
    }

    /**
     * 监控独享
     */
    public RestHighLevelClient supervisoryControlHighLevelClient() {
        return buildHighLevelClient(supervisoryControlEsAddress);
    }

    public RestHighLevelClient buildHighLevelClient(String esHosts) {
        String[] address = esHosts.split(",");
        int length = address.length;

        HttpHost[] httpHosts = new HttpHost[length];
        for (int i = 0; i < length; i++) {
            String[] split = address[i].split(":");
            httpHosts[i] = new HttpHost(split[0], Integer.parseInt(split[1]), "http");
        }
        RestClientBuilder builder = RestClient.builder(httpHosts);
        // 异步httpclient连接延时配置
        builder.setRequestConfigCallback(new RequestConfigCallback() {
            @Override
            public Builder customizeRequestConfig(Builder requestConfigBuilder) {
                requestConfigBuilder.setConnectTimeout(CONNECT_TIME_OUT);
                requestConfigBuilder.setSocketTimeout(SOCKET_TIME_OUT);
                requestConfigBuilder.setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT);
                return requestConfigBuilder;
            }
        });
        // 异步httpclient连接数配置
        builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder.setMaxConnTotal(MAX_CONNECT_NUM);
                httpClientBuilder.setMaxConnPerRoute(MAX_CONNECT_PER_ROUTE);
                return httpClientBuilder;
            }
        });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
