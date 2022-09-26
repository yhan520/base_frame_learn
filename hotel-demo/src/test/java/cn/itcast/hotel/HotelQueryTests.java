package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

public class HotelQueryTests {
    RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://152.136.197.17:9200")));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void testQuery() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        //request.source().query(QueryBuilders.matchAllQuery());
        //request.source().query(QueryBuilders.matchQuery("all", "如家"));
        //request.source().query(QueryBuilders.multiMatchQuery("如家", "name", "brand"));
        //request.source().query(QueryBuilders.termQuery("city", "北京"));
        request.source().query(QueryBuilders.rangeQuery("price").gte(100).lte(500));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termQuery("city", "深圳"));
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte("100").lte(500));
        request.source().query(boolQueryBuilder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testPageAndSort() throws IOException {
        int page = 1, size = 5;
        SearchRequest request = new SearchRequest("hotel");

        request.source().query(QueryBuilders.matchAllQuery());
        request.source().sort("price", SortOrder.DESC);
        request.source().from((page - 1) * size).size(size);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testHighlight() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(Boolean.FALSE));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    private static void handleResponse(SearchResponse response) {
        //得到查询到的所有数据
        SearchHits hits = response.getHits();
        //获取数据的条数
        long num = hits.getTotalHits().value;
        System.out.println("数据总条数：" + num);

        //获取真实存储的内容，存放在数组中
        SearchHit[] hitsHits = hits.getHits();
        for (SearchHit hit : hitsHits) {
            //将这些内容转换为Json字符串
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            //获取高亮集合
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightName= highlightFields.get("name");
                if(highlightName != null){
                    String s = highlightName.getFragments()[0].string();
                    hotelDoc.setName(s);
                }
            }
            System.out.println(hotelDoc);
        }
    }
}
