# 秒杀项目

http://112.74.172.117/resources/login.html
服务器配置：2 vCPU 8GB,nginx反向代理网络25Mbps，其余1Mbps

## 结构示意图

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020052816101061.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200528161047148.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

## 项目搭建步骤
### 1. 搭建SpringBoot框架
### 2. 集成mybatis
1. pom中配置mybatis-generator
2. 编写mybatis-generator.xml
### 3. SpringMVC开发用户信息
1. 三层Model
    1. 数据层DataObject 与数据库一一映射
    2. 业务层Domain Model 数据，get，set,整个领域模型
    3. 接入层ViewObject 前端展示模型
2. CommonReturnType   Status + Data
    给前端返回一个有意义的信息
    程序报错时有具体的错误编码
3. 定义错误信息
4. 定义ExceptionHandler 解决未被controller层吸收的exception

### 4. 校验规则工具类
1. 引入hibernare-validator
2. 在相关属性上添加注释,@NotBlank(message=""),@NotNull(message=""),@Min(),@Max()等

### 5. 用户模型
1. 注册
    1. opt验证码获取
    2. 前后端分离，注册界面
    3. 用户注册接口
        1. 验证手机号和对应的otpcode相符合
        2. UserModel userModel = new UserModel();
    4. 跨域介绍
        出于安全原因，浏览器限制从脚本内发起的跨源HTTP请求。 
        例如，XMLHttpRequest和Fetch API遵循同源策略。 
        这意味着使用这些API的Web应用程序只能从加载应用程序的同一个域请求HTTP资源，除非使用CORS头文件。
        跨域的体现，在于它的域名不同或者端口不同，但要注意以下的形式为非跨域模式
    5. 跨域请求@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
       允许跨域传输所有的header参数
    6. 将telephone设置为唯一索引,防止相同手机号重复注入。
    
2. 登录
    1. 入参校验,输入的手机号密码不能为空
    2. 通过手机号获取用户信息
    3. 比对用户信息内加密的密码是否和传输进来的密码相匹配
    4. 生成登录凭证token，UUID
    5. 若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中
        
### 6. 商品模块
1. 商品的创建
    1. 库存表与商品表分开设计,提高性能
    2.  1. 校验入参
        2. 转化itemmodel->dataobject
        3. 写入数据库
        4. 返回创建完成的对象
2. 商品详情页浏览
    1. 先取本地缓存
    2. 根据商品的id到redis内获取
    3. 若redis内不存在对应的itemModel,则访问下游service
    4. 填充本地缓存
3. 商品列表浏览
    
### 7. 交易模块
1. 创建订单
    1. 校验下单状态:商品是否存在,用户是否合法,购买数量是否正确
    2. 落单减库存:订单创建后锁定库存
    3. 订单入库
        1. 判断商品是否是活动商品
        2. 生成16位订单号,前8位为时间信息,年月日,中间6位为自增序列,最后两位为分库分表位
            1. 自增序列通过一个单独的sequence表来完成
            2. sequence需要设置最大值,初始值,进行循环使用
            3. 事务等级设置@Transactional(propagation = Propagation.REQUIRES_NEW),sequence应该具有全局唯一性,事务回滚后,也会将当前事务的sequence释放
    4. 商品销量增加
    5. 返回前端

2. OrderController
    1. 获取用户登录信息
    2. 

### 8. 秒杀活动模块
1. 活动模型的创建
2. 活动模型与商品模型结合
3. 秒杀service提供的服务:
    1. 根据itemid获取即将进行的或正在进行的秒杀活动
    2. 活动发布
    3. 生成秒杀用的令牌
  
## 云端部署项目步骤
1. 本地在项目根目录下使用mvn clean package打包生成miaosha.jar文件
2. 将jar包服务上传到服务端上并编写额外的application.properties配置文件
3. 编写deploy.sh文件启动对应的项目
    ```
   nohup java -Xms400m -Xmx400m -XX:NewSize=200m -XX:MaxNewSize=200m -jar miaosha.jar --spring.config.addition-location=/miaosha/application.properties
   nohup:以非停止方式运行程序，这样即便控制台退出了程序也不会停止
   -Xms 堆的初始值    -Xmx 堆的最大值    
   -XX:NewSize=200m      新生代预估上限的默认值
   -XX:MaxNewSize=200m   新生代占整个堆内存的最大值 
    ```
4. 使用./deploy.sh &启动应用程序
5. 打开阿里云的网络安全组配置，将80端口开放给外网可访问

## 优化步骤
### 一、对访问商品页的优化
#### 1. 内嵌Tomcat配置
1. 修改application.properties
    ```
    server.tomcat.accept-count=1000         等待队列长度
    server.tomcat.max-threads=100           最大工作线程数目
    server.tomcat.min-spare-threads=50      最小工作线程数目
    ```
2. 修改WebServerConfiguration.java
    ```
    //定制化keepalivetimeout,设置30秒内没有请求则服务端自动断开keepalive链接
    protocol.setKeepAliveTimeout(30000);
    //当客户端发送超过10000个请求则自动断开keepalive链接,防止被大量请求攻击
    protocol.setMaxKeepAliveRequests(10000);
    ```
#### 2. 分布式扩展
1. 连接到mysql服务器上修改系统相关的配置，将对应的用户授予远程连接及后续的所有权限
2. nginx

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200719144028954.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
    1. nginx静态资源部署
       进入nginx根目录下的html下，然后新建resources目录用于存放前端静态资源
    2. nginx动态请求反向代理
       配置一个backend server，可以用于指向后端不同的server集群
   ```
       upstream backend_server{
           server 47.107.149.48 weight=1;
           server 120.25.202.247 weight=1;         weight 表示权重
           keepalive 30;                           长连接时间
   }
   ```
3. 分布式会话,一般使用token,因为移动端有时会不支持cookie
    1. 基于cookie传输sessionid:java tomcat容器session实现迁移到redis
        1. 配置redis和jedis
            ```
               #redis
               spring.redis.host=47.106.250.216
               spring.redis.port=6379
               spring.redis.database=10
               #设置jedis连接池
               spring.redis.jedis.pool.max-active=50
               spring.redis.jedis.pool.min-idle=20
            ```
        2. maven导入springboot对redis和session操作的jar包,会将session存入到redis中
        3. 编写RedisConfig.java,对redis进行具体设置
    2. 基于token传输类似sessionid:java代码session实现迁移到redis
        1. 将登录token返回客户端并存入redis
            ```
           //若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中
           //生成登录凭证token，UUID
           String uuidToken = UUID.randomUUID().toString();
           uuidToken = uuidToken.replace("-", "");
           //建立token和用户登陆态之间的联系
           //通过RedisTemplate类操作SpringBoot内嵌的redis
           redisTemplate.opsForValue().set(uuidToken, userModel);
           redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);//存活时间 
           //将token返回到客户端
           return CommonReturnType.create(uuidToken);
           ```
        2. 前端将获取到的token存储到localStorage
#### 3. 缓存优化
##### 1. reids缓存
1. 单机模式

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601222613781.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
2. 哨兵模式

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601223403863.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601223342633.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
    主从同步,读写分离
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200718171322444.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

3. 集群cluster模式
    有多个redis服务器，雪花状集群，n读n写，自动竞选出对应master和slave，
    每台redis服务器和别的服务器都有联系，清楚的知道他们的主从关系，自己所处的地位。
    只需连接其中任意一台服务器就能只能整个集群的关系，并存储到内存中。get，set操作发送目标也就清楚了。
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200719140004592.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

    当某个服务器丢失，集群会自动调整。会向客户端发送reask请求，调整内存中存储的集群关系
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200719140348737.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
4. 具体实现
    1. 根据商品的id到redis内获取
    2. 若redis内不存在对应的itemModel,则访问下游service,并将其存入redis中

5. 缺陷
    redis是集中式管理缓存,需要使用网络io,对性能有影响
##### 2. 本地热点缓存
1. 使用Guaua cache做为本地缓存
    1. 提供一个并发的hashmap
    2. 可控制大小和超时时间
    3. 可配置lru策略
    4. 线程安全
2. 特点
    1. 存放热点数据,大量访问,很少有改动
    2. 脏读不敏感:每个分布式服务器上都保存着热点数据,本地热点缓存很少有方法改变数据
    3. 内存可控
3. 实现步骤
    1. 根据商品的id到本地缓存内获取
    2. 若不存在则到redis,下游service中寻找
    3. 将商品放入缓存
##### 3. nginx proxy cache缓存
依靠文件系统存储索引级的文件作为缓存,缓存的文件地址存储在内存中
经测试性能会下降,所以不采用
##### 4. nginx lua缓存
shared dic：共享内存字典，所有worker进程可见，lru淘汰
1. 在Nginx.conf中配置
    ```
    lua_shared_dict my_cache 128m; #name：my_cache，内存大小：128m
    location /luaitem/get{
           default_type "application/json";
           content_by_lua_file ../lua/itemredis.lua;
    }
    ```
2. 到lua目录下创建lua脚本
    ```
    # get方法
    function get_from_cache(key)
            local cache_ngx = ngx.shared.my_cache # 获取Nginx缓存
            local value = cache_ngx:get(key)
            return value
    end
    # set方法
    function set_to_cache(key,value,exptime)
            if not exptime then
                    exptime = 0
            end
            local cache_ngx = ngx.shared.my_cache
            local succ,err,forcible = cache_ngx:set(key,value,exptime)
            return succ
    end
    
    local args = ngx.req.get_uri_args()
    local id = args["id"]
    local item_model = get_from_cache("item_"..id)
    if item_model == nil then
            local resp = ngx.location.capture("/item/get?id="..id)
            item_model = resp.body
            set_to_cache("item_"..id,item_model,1*60)
    end
    ngx.say(item_model)
    ```
3. http://112.74.172.117/luaitem/get?id=6

### 二、对交易性能的优化
#### 1. 性能瓶颈
1. 对交易信息的校验过程会对mysql进行多次访问
2. 落单减库存的sql操作会加上一个行锁
3. 生产订单的过程也会对mysql多次访问
#### 2. 高效交易验证方式
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200602125232587.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
##### 1. 用户风控策略优化：
将商品，用户信息放入redis缓存中并设置有效时间，验证时从缓存中获取
##### 2. 活动校验策略优化：引入活动发布流程，模型缓存化，紧急下线能力
1. 扣减库存缓存化,不采用
    1. 活动发布同步库存进缓存
    2. 下单交易减缓存库存
    3. 缓存与数据库不一致,不是好方法,若缓存出现问题,无法回调
2. 异步同步数据库
    1. 活动发布同步库存进缓存
    2. 下单交易减缓存库存
    3. 异步消息扣减数据库内库存
        1. 引入rocketmq
        2. application.properties配置rocketmq的nameserver地址和topic
        3. MqProducer.java 发出减库存的消息
           MqConsumer.java 接受消息,到数据库里减库存
        4. 问题:
            1. 异步消息发送失败
            2. 扣减操作执行失败
            3. 下单失败无法正确回补库存
3. 库存数据库最终一致性保证:解决少卖问题
    1. spring提供的一个方法:在事务提交后再执行某个操作
        ```
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                //异步更新库存
                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
            }
        });
        ```
    2. 在整个订单事务提交完毕后再进行异步更新库存操作
    3. 使用TransactionMQProducer,transactionMQProducer.sendMessageInTransaction(message, argsMap);提交事务型信息
    4. sendMessageInTransaction(message, argsMap);
       内部会执行executeLocalTransaction(Message msg, Object arg)//真正要做的事  创建订单
    5. 当返回状态为unknown不知道是否成功还是失败时
       执行checkLocalTransaction(MessageExt msg)//根据是否扣减库存成功，来判断要返回COMMIT,ROLLBACK还是继续UNKNOWN
    6. checkLocalTransaction(MessageExt msg)通过获取库存流水stockLogDO来判断订单状态
    7. 库存售罄处理
        1. 落单减库存时若库存为0,则打上库存售罄标识,加入redis缓存
        2. 操作后续流程前先在redis中判断是否有售罄标识

### 三、流量削峰
#### 1. 秒杀令牌
1. 问题
    1. 秒杀接口泄露,会被脚本不停刷
    2. 秒杀验证逻辑和秒杀下单接口强关联
    3. 秒杀验证逻辑复杂,对交易系统产生无关联负载
2. 方案
    1. 秒杀接口需要令牌才能进入
    2. 秒杀令牌由秒杀活动模块产生
    3. 下单前需要获得秒杀令牌
3. 秒杀令牌具体实现:生成令牌的方法PromoServiceImpl.generateSecondKillToken(Integer promoId, Integer itemId, Integer userId)
#### 2. 秒杀大闸
1. 问题:秒杀令牌在秒杀活动开始后可以无限制生成,影响系统性能
2. 原理:
    1. 利用秒杀令牌的授权原理定制化发牌逻辑,做到大闸功能
    2. 根据秒杀商品初始库存办法相应数量的令牌
3. 实现:
    1. 发布活动时将秒杀大闸限制数字设到redis内
    2. 在生成令牌时,获取大闸数量,并-1;若-1后仍>=0,则生成令牌
#### 3. 队列泄洪
1. 问题:
    1. 浪涌流量涌入后系统无法应付
    2. 多库存,多商品等令牌限制能力弱
2. 原理:
    1. 排队有时候比并发更高效
    2. 依靠排队和下游拥塞窗口程度调整队列释放流量大小
3. 实现: 线程池
### 四、防刷限流
#### 1. 验证码生成
1. CodeUtil生成验证码
2. 将图片写入HttpServletResponse中显示在前端
3. 将生成的验证码写入redis中,与用户id绑定
2. 生成令牌前必须验证码验证
#### 2. 限流
1. 令牌桶
限制速度最大值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200603210155875.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
2. 漏桶
限制速度,平滑流量
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200603210237535.png)  
3. 实现

    使用google的guava的RateLimter
    ```
    private RateLimiter orderCreateRateLimiter = RateLimiter.create(300);
    ```
    在下单前用tryAcquire()获取令牌
    ```
    if (!orderCreateRateLimiter.tryAcquire()) {
        throw new BusinessException(EmBusinessError.RATELIMIT);
    }
    ```
    当令牌不足,线程会陷入睡眠一段时间,提前获取下一秒的令牌,然后才可以继续操作.