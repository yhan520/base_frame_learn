package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.api.R;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class HotelDocumentTests {
    RestHighLevelClient client;

    @Autowired
    IHotelService iHotelService;

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://152.136.197.17:9200")));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void testAddDocument() throws IOException {
        Hotel hotel = iHotelService.getById(60214L);

        HotelDoc hotelDoc = new HotelDoc(hotel);
        String jsonString = JSON.toJSONString(hotelDoc);

        IndexRequest request = new IndexRequest("hotel");
        request.id(hotelDoc.getId() + "");
        request.source(jsonString, XContentType.JSON);

        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocumentById() throws IOException {
        GetRequest request = new GetRequest("hotel");
        request.id("60214");

        GetResponse response = client.get(request, RequestOptions.DEFAULT);

        String json = response.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocument() throws IOException {
        UpdateRequest request = new UpdateRequest("hotel", "60214");
        request.doc(
                "price", 599,
                "starName", "four"
        );
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        DeleteRequest request = new DeleteRequest("hotel", "60214");
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
        List<Hotel> hotels = iHotelService.list();
        BulkRequest request = new BulkRequest();
        HotelDoc hotelDoc;
        for(Hotel hotel : hotels) {
            hotelDoc = new HotelDoc(hotel);
            String jsonString = JSON.toJSONString(hotelDoc);
            request.add(new IndexRequest("hotel").id(hotelDoc.getId().toString()).source(jsonString, XContentType.JSON));

        }
        //批量处理BulkRequest，其本质就是将多个普通的CRUD请求组合在一起发送
        client.bulk(request, RequestOptions.DEFAULT);
    }
}
