package com.anoyi.douyin.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.anoyi.douyin.bean.DyUserVO;
import com.anoyi.douyin.entity.DyAweme;
import com.anoyi.douyin.rpc.RpcNodeDyService;
import com.anoyi.douyin.util.DyNumberConverter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

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

    private final static String POST_VIDEOS_API = "https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id=%s&count=21&max_cursor=%s&aid=1128&_signature=%s&dytk=%s";

    private final static String LIKE_VIDEOS_API = "https://www.iesdouyin.com/web/api/v2/aweme/like/?user_id=%s&count=21&max_cursor=%s&aid=1128&_signature=%s&dytk=%s";

    private final static String USER_SHARE_API = "https://www.iesdouyin.com/share/user/%s?share_type=link";

    private final static String RECOMMEND_VIDEO_API = "https://aweme-eagle-hl.snssdk.com/aweme/v1/feed/?version_code=7.7.0&pass-region=1&pass-route=1&js_sdk_version=1.17.4.3&app_name=aweme&vid=C266ADED-A5C8-463E-9C5A-DA376B0FA802&app_version=7.7.0&device_id=67068710449&channel=App%20Store&mcc_mnc=46011&aid=1128&screen_width=828&openudid=c8412ad758b51f17b5ab859866ee24809789ab24&os_api=18&ac=WIFI&os_version=12.4&device_platform=iphone&build_number=77019&device_type=iPhone11,8&iid=83511626666&idfa=6FDD82E0-8687-4C6F-BB99-A7748637C048&volume=-0.06&count=6&longitude=121.482183606674&feed_style=0&filter_warn=0&cached_item_num=1&address_book_access=0&last_ad_show_interval=18&user_id=95044648655&type=0&gps_access=3&latitude=31.24161681429322&pull_type=2&max_cursor=0";

    private final static String TT_TOKEN = "000691de6bdedb03f4630c6e9752ae210339f3075692e4eabe75ae25b5107a4fa133395e42466336f61166a6d0b57e642515";

    private final static String GORGON = "8300e71d000094d0396dcadee0624bc8d6d517436119a24aa886";

    private final RpcNodeDyService rpcNodeDyService;

    /**
     * Media 签名算法
     */
    public String mediaSign() {
        return rpcNodeDyService.mediaSignature();
    }

    /**
     * Media 签名算法
     */
    public String enterpriseSign(String cookie, String url) {
        try {
            Connection.Response response = Jsoup.connect("https://e.douyin.com/aweme/v1/bluev/user/info")
                    .header("Cookie", cookie)
                    .header("User-Agent", UserAgent)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET).execute();
            JSONObject json = JSON.parseObject(response.body());
            return rpcNodeDyService.enterpriseSignature(json.getString("sec_token"), url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 用户发布的视频列表
     */
    public DyAweme postVideos(String dyId, String dytk, String cursor, String signature) {
        String api = String.format(POST_VIDEOS_API, dyId, cursor, signature, dytk);
        return getVideoList(api);
    }

    /**
     * 用户喜欢的视频列表
     */
    public DyAweme likeVideos(String dyId, String dytk, String cursor, String signature) {
        String api = String.format(LIKE_VIDEOS_API, dyId, cursor, signature, dytk);
        return getVideoList(api);
    }

    /**
     * 获取抖音用户视频列表
     */
    private DyAweme getVideoList(String api) {
        try {
            Document document = httpGet(api);
            DyAweme aweme = JSON.parseObject(document.text(), DyAweme.class);
            aweme.getAweme_list().forEach(item -> {
                String[] urlList = item.getVideo().getPlay_addr().getUrl_list();
                for (int i = 0; i < urlList.length; i++) {
                    urlList[i] = urlList[i].replace("https://aweme.snssdk.com/aweme/v1/play/", "https://aweme.snssdk.com/aweme/v1/playwm/");
                }
                item.getVideo().getPlay_addr().setUrl_list(urlList);
            });
            return aweme;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("HTTP request error: " + api);
    }

    public DyUserVO getDyUserByHtml(String id, Document document) {
        DyUserVO dyUser = new DyUserVO();
        dyUser.setId(id);
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
        String focus = document.select("span.focus.block span.num").text();
        dyUser.getFollowInfo().put("focus", focus);
        String follower = document.select("span.follower.block span.num").text();
        dyUser.getFollowInfo().put("follower", follower);
        String likeNum = document.select("span.liked-num.block span.num").text();
        dyUser.getFollowInfo().put("likeNum", likeNum);
        String posts = document.select("div[data-type=post] span.num").text();
        dyUser.setPosts(posts);
        String likes = document.select("div[data-type=like] span.num").text();
        dyUser.setLikes(likes);
        Matcher matcher = Pattern.compile("tac='.*?'").matcher(document.select("script").html());
        if (matcher.find()){
            String tacScript = matcher.group(0);
            String sign = rpcNodeDyService.iesSignature(id, tacScript);
            dyUser.setSign(sign);
            return dyUser;
        }
        throw new RuntimeException("Unknow Exception");
    }

    /**
     * 获取抖音用户信息
     */
    public DyUserVO getDyUser(String id) {
        try {
            String api = String.format(USER_SHARE_API, id);
            Document document = httpGet(api);
            return getDyUserByHtml(id, document);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("HTTP request error: " + id);
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
    private String match(String content, String regx) {
        Matcher matcher = Pattern.compile(regx).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 全局 icon 数字解析
     */
    private void parseIconFonts(Document document) {
        Elements elements = document.select("i.icon.iconfont");
        elements.forEach(element -> {
            String text = element.text();
            String number = DyNumberConverter.getNumber(text);
            element.text(number);
        });
    }

}
