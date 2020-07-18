# 秒杀项目

http://112.74.172.117/resources/login.html

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
### 1. 内嵌Tomcat配置
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
### 2. 分布式扩展
1. 连接到mysql服务器上修改系统相关的配置，将对应的用户授予远程连接及后续的所有权限
2. nginx

    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200529131351261.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
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
        2. maven导入springboot对redis和session操作的jar包
        3. 编写RedisConfig.java,对redis进行具体设置
    2. 基于token传输类似sessionid:java代码session实现迁移到redis
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
### 3. 缓存优化
1. reids缓存
    1. 单机模式
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601222613781.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
    2. 哨兵模式
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601223403863.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
    
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200601223342633.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
    3. 集群cluster模式
    
        读写分离
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200718170629533.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)
2. 热点内存本地缓存
3. nginx proxy cache缓存
4. nginx lua缓存