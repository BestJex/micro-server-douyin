package com.anoyi.douyin.controller;

import com.anoyi.douyin.bean.DySignatureRequestBean;
import com.anoyi.douyin.bean.DyUserVO;
import com.anoyi.douyin.entity.DyAweme;
import com.anoyi.douyin.service.DouyinService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/douyin")
@AllArgsConstructor
@CrossOrigin("*")
public class DouyinController {

    private final DouyinService douyinService;

    @PostMapping("/user/{id}")
    public DyUserVO user(@RequestBody String html, @PathVariable("id") String id){
        Document document = Jsoup.parse(html);
        return douyinService.getDyUserByHtml(id, document);
    }

    @GetMapping("/user/{id}")
    public DyUserVO user(@PathVariable("id") String id) {
        return douyinService.getDyUser(id);
    }

    @GetMapping("/post/{id}/{tk}")
    public DyAweme posts(@PathVariable("id") String id,
                          @PathVariable("tk") String tk,
                          @RequestParam(value = "cursor", defaultValue = "0") String cursor,
                          @RequestParam("s") String sign) {
        return douyinService.postVideos(id, tk, cursor, sign);
    }

    @GetMapping("/like/{id}/{tk}")
    public DyAweme likes(@PathVariable("id") String id,
                          @PathVariable("tk") String tk,
                          @RequestParam(value = "cursor", defaultValue = "0") String cursor,
                          @RequestParam("s") String sign) {
        return douyinService.likeVideos(id, tk, cursor, sign);
    }

    @GetMapping("/video")
    public DyAweme video() {
        return douyinService.videoList();
    }

    @GetMapping("/media/signature")
    public String mediaSignature() {
        return douyinService.mediaSign();
    }

    @PostMapping("/enterprise/signature")
    public String enterpriseSignature(@RequestBody DySignatureRequestBean signatureRequestBean) {
        return douyinService.enterpriseSign(signatureRequestBean.getCookie(), signatureRequestBean.getUrl());
    }

}