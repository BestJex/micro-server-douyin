### 签名生成

**media.douyin.com**

- API: /douyin//media/signature
- 请求方式：GET

**e.douyin.com**

- API: /douyin/enterprise/signature
- 请求方式：POST
- Header ①： Content-Type: application/json
- 请求体：
```$xslt
{
	"cookie": "抖音企业账号 Cookie",
	"url": "抖音企业 API" 
}
```

> url 示例：/aweme/v1/bluev/add/quick/response
