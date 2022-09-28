package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    IHotelService iHotelService;

    @PostMapping("/list")
    public PageResult list(@RequestBody RequestParams params){
        return iHotelService.search(params);
    }

    @PostMapping("/filters")
    public Map<String, List<String>> filter(@RequestBody RequestParams params){
        return iHotelService.getFilters(params);

    }

    @GetMapping("/suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {
        System.out.println(prefix);
        return iHotelService.getSuggestions(prefix);
    }

}
