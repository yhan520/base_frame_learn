package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            SearchRequest request = new SearchRequest("hotel");
            //构造查询条件
            buildBasicQuery(request,params);

            //构造分页
            request.source().from((params.getPage() - 1) * params.getSize()).size(params.getSize());

            //构造排序
            buildSort(params, request);

            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return handleResponse(response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {

        try {
            //返回的结果
            Map<String, List<String>> result = new HashMap<>();

            SearchRequest request = new SearchRequest("hotel");
            //构造基础查询条件
            buildBasicQuery(request, params);
            //设置结果的长度
            request.source().size(0);
            //构造聚合条件
            buildAggregation(request);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            Aggregations aggregations = response.getAggregations();
            List<String> cityAges = getAggByName(aggregations, "cityAges");
            result.put("city", cityAges);

            List<String> brandAges = getAggByName(aggregations, "brandAges");
            result.put("brand", brandAges);

            List<String> starNameAges = getAggByName(aggregations, "starNameAges");
            result.put("starName", starNameAges);

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            SearchRequest request = new SearchRequest("hotel");

            request.source().query(QueryBuilders.matchAllQuery()).size(0);

            //请求参数
            /*request.source().suggest(new SuggestBuilder().addSuggestion("suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)));*/
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));

            //发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            //解析结果
            Suggest suggest = response.getSuggest();
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            //遍历options，获取text，转换成字符串，保存到集合中
            List<String> list = new ArrayList<>();
            for (CompletionSuggestion.Entry.Option option : options) {
                String s = option.getText().toString();
                list.add(s);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getAggByName(Aggregations aggregations, String aggName) {
        //根据聚合名称，获取聚合结果
        Terms cityAggs = aggregations.get(aggName);
        //获取聚合结果中的buckets
        List<? extends Terms.Bucket> buckets = cityAggs.getBuckets();
        List<String> list = new ArrayList<>();
        //遍历buckets，并将其中的key保存到list集合中
        for (Terms.Bucket bucket : buckets) {
            String key = bucket.getKeyAsString();
            list.add(key);
        }
        return list;
    }

    private static void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders.terms("cityAges").field("city").size(10));
        request.source().aggregation(AggregationBuilders.terms("starNameAges").field("starName").size(10));
        request.source().aggregation(AggregationBuilders.terms("brandAges").field("brand").size(10));
    }

    private static void buildSort(RequestParams params, SearchRequest request) {
        if(params.getSortBy() != null) {
            if("score".equals(params.getSortBy())) {
                request.source().sort(SortBuilders.fieldSort("score").order(SortOrder.DESC));
            }else if("price".equals(params.getSortBy())) {
                request.source().sort(SortBuilders.fieldSort("price").order(SortOrder.ASC));
            }
        }
        //位置排序
        //String location = params.getLocation();
        String location = "30.922659, 121.574572";
        if(location != null && !"".equals(location)){
            request.source().sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.KILOMETERS));
        }
    }

    public void buildBasicQuery(SearchRequest request, RequestParams params) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        String key = params.getKey();
        if(key == null || "".equals(key)) {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }else {
            boolQueryBuilder.must(QueryBuilders.matchQuery("all", key));
        }

        if(params.getCity() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("city", params.getCity()));
        }

        if(params.getBrand() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }

        if(params.getStarName() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }

        if(params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price")
                    .gte(params.getMinPrice())
                    .lte(params.getMaxPrice()));
        }

        request.source().query(boolQueryBuilder);

    }

    public PageResult handleResponse(SearchResponse response) {
        PageResult pageResult = new PageResult();
        //获取结果
        SearchHits hits = response.getHits();

        long num = hits.getTotalHits().value;
        pageResult.setTotal(num);

        HotelDoc hotelDoc;
        List<HotelDoc> hotelDocList = new ArrayList<>();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            //获取数据
            String json = hit.getSourceAsString();
            //将数据反序列化
            hotelDoc = JSON.parseObject(json, HotelDoc.class);

            //获取排序值
            Object[] sortValues = hit.getSortValues();
            if(sortValues != null) {
                if(sortValues.length == 1) {
                    Object distance = sortValues[0];
                    hotelDoc.setDistance(distance);
                }else if(sortValues.length > 1){
                    Object distance = sortValues[1];
                    hotelDoc.setDistance(distance);
                }
            }
            hotelDocList.add(hotelDoc);
        }

        pageResult.setHotels(hotelDocList);
        return pageResult;
    }
}
