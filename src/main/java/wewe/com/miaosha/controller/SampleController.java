package wewe.com.miaosha.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import wewe.com.miaosha.domain.User;
import wewe.com.miaosha.redis.RedisService;
import wewe.com.miaosha.redis.UserKey;
import wewe.com.miaosha.result.Result;
import wewe.com.miaosha.service.UserService;

@Controller
@RequestMapping("/demo")
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model) {
        model.addAttribute("name", "wewe");
        return "hello";
    }

    @RequestMapping("/db/tx")
    @ResponseBody
    public Result<Boolean> dbTx() {
        boolean tx = userService.tx();
        return Result.success(true);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<User> redisSet() {
        User user = new User();
        user.setId(3);
        user.setName("wewe");

        Boolean v2 = redisService.set(UserKey.getById,""+user.getId(), user);
        User v1 = redisService.get(UserKey.getById,""+user.getId(), User.class);
        return Result.success(v1);
   }
}

