package com.anoyi.douyin.service;

import com.alibaba.fastjson.JSON;
import com.anoyi.douyin.bean.DyUserVO;
import com.anoyi.douyin.entity.DyAweme;
import com.anoyi.douyin.rpc.RpcNodeDyService;
import com.anoyi.douyin.util.DyNumberConvertor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
@Slf4j
public class DouyinService {

    private final static String UserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1";

    private final static String XMLHttpRequest = "XMLHttpRequest";

    private final static String VIDEO_LIST_API = "https://www.iesdouyin.com/aweme/v1/aweme/post/?user_id=%s&count=21&max_cursor=%s&aid=1128&_signature=%s&dytk=%s";

    private final static String USER_SHARE_API = "https://www.iesdouyin.com/share/user/%s?share_type=link";

    private final static String RECOMMEND_VIDEO_API = "https://aweme-eagle-hl.snssdk.com/aweme/v1/feed/?version_code=7.7.0&pass-region=1&pass-route=1&js_sdk_version=1.17.4.3&app_name=aweme&vid=C266ADED-A5C8-463E-9C5A-DA376B0FA802&app_version=7.7.0&device_id=67068710449&channel=App%20Store&mcc_mnc=46011&aid=1128&screen_width=828&openudid=c8412ad758b51f17b5ab859866ee24809789ab24&os_api=18&ac=WIFI&os_version=12.4&device_platform=iphone&build_number=77019&device_type=iPhone11,8&iid=83511626666&idfa=6FDD82E0-8687-4C6F-BB99-A7748637C048&volume=-0.06&count=6&longitude=121.482183606674&feed_style=0&filter_warn=0&cached_item_num=1&address_book_access=0&last_ad_show_interval=18&user_id=95044648655&type=0&gps_access=3&latitude=31.24161681429322&pull_type=2&max_cursor=0";

    private final static String TT_TOKEN = "000691de6bdedb03f4630c6e9752ae210339f3075692e4eabe75ae25b5107a4fa133395e42466336f61166a6d0b57e642515";

    private final static String GORGON = "8300e71d000094d0396dcadee0624bc8d6d517436119a24aa886";

    private final RpcNodeDyService rpcNodeDyService;

    /**
     * 系统推荐列表
     */
    public DyAweme videoList() {
        try {
            Document document = Jsoup.connect(RECOMMEND_VIDEO_API)
                    .header("X-Khronos", String.valueOf(new Date().getTime() / 1000))
                    .header("x-Tt-Token", TT_TOKEN)
                    .header("X-Gorgon", GORGON)
                    .ignoreContentType(true)
                    .get();
            log.info(document.title());
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 用户视频列表
     */
    public DyAweme videoList(String dyId, String dytk, String cursor) {
        return getVideoList(dyId, dytk, cursor);
    }

    /**
     * 获取抖音用户视频列表
     */
    private DyAweme getVideoList(String dyId, String dytk, String cursor){
        String script = null;
        try {
            Document document = Jsoup.connect("https://www.iesdouyin.com/share/user/" + dyId).get();
            script = document.select("script").get(1).html();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String signature = rpcNodeDyService.generateSignature(dyId, script);
        String api = String.format(VIDEO_LIST_API, dyId, cursor, signature, dytk);
        try {
            Document document = httpGet(api);
            return JSON.parseObject(document.text(), DyAweme.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("HTTP request error: " + api);
    }

    /**
     * 获取抖音用户信息
     */
    public DyUserVO getDyUser(String dyId) {
        String api = String.format(USER_SHARE_API, dyId);
        try {
            DyUserVO dyUser = new DyUserVO();
            dyUser.setId(dyId);
            Document document = httpGet(api);
            parseIconFonts(document);
            String nickname = document.select("p.nickname").text();
            dyUser.setNickname(nickname);
            String avatar = document.select("img.avatar").attr("src");
            dyUser.setAvatar(avatar);
            String tk = match(document.html(), "dytk: '(.*?)'");
            dyUser.setTk(tk);
            String shortId = document.select("p.shortid").text();
            dyUser.setShortId(shortId);
            String verifyInfo = document.select("div.verify-info").text();
            dyUser.setVerifyInfo(verifyInfo);
            String signature = document.select("p.signature").text();
            dyUser.setSignature(signature);
            String location = document.select("span.location").text();
            dyUser.getExtraInfo().put("location", location);
            String constellation = document.select("span.constellation").text();
            dyUser.getExtraInfo().put("constellation", constellation);
            String focus = document.select("span.focus.block span.num").text();
            dyUser.getFollowInfo().put("focus", focus);
            String follower = document.select("span.follower.block span.num").text();
            dyUser.getFollowInfo().put("follower", follower);
            String likeNum = document.select("span.liked-num.block span.num").text();
            dyUser.getFollowInfo().put("likeNum", likeNum);
            DyAweme videos = videoList(dyId, tk, "0");
            dyUser.setVideos(videos);
            return dyUser;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("HTTP request error: " + api);
    }

    /**
     * HTTP 请求
     */
    private Document httpGet(String url) throws IOException {
        Connection.Response response = Jsoup.connect(url)
                .header("user-agent", UserAgent)
                .header("x-requested-with", XMLHttpRequest)
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .ignoreContentType(true).execute();
        String html = response.body().replace("&#xe", "");
        return Jsoup.parse(html);
    }

    /**
     * 正则匹配
     */
    private String match(String content, String regx){
        Matcher matcher = Pattern.compile(regx).matcher(content);
        if (matcher.find()){
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 全局 icon 数字解析
     */
    private void parseIconFonts(Document document){
        Elements elements = document.select("i.icon.iconfont");
        elements.forEach(element -> {
            String text = element.text();
            String number = DyNumberConvertor.getNumber(text);
            element.text(number);
        });
    }

}
