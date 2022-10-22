package cloud.makeronbean.gmall.all.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author makeronbean
 */
@Controller
public class UserController {



    /**
     * 跳转到登陆页面
     */
    @GetMapping("/login.html")
    public String login(String originUrl, Model model){
        model.addAttribute("originUrl",originUrl);
        return "login";
    }

}
