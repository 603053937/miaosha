# 秒杀项目

## 结构示意图

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020052816101061.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200528161047148.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dld2Vib3k=,size_16,color_FFFFFF,t_70)

## 项目搭建步骤
1. 搭建SpringBoot框架
2. 集成mybatis
    1. pom中配置mybatis-generator
    2. 编写mybatis-generator.xml
3. SpringMVC开发用户信息
    1. 三层Model
        1. DataObject 与数据库一一映射
        2. Model 数据，get，set,整个领域模型
        3. ViewObject 前端展示模型
    2. CommonReturnType   Status + Data
        给前端返回一个有意义的信息
        程序报错时有具体的错误编码
    3. 定义错误信息
    4. 定义ExceptionHandler 解决未被controller层吸收的exception

4. 校验规则工具类
    1. 引入hibernare-validator
    2. 在相关属性上添加注释,@NotBlank(message=""),@NotNull(message=""),@Min(),@Max()等

5. 用户模型
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
        
6. 商品模块
    1. 商品的创建
    