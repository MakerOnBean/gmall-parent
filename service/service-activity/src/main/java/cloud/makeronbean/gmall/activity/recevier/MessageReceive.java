package cloud.makeronbean.gmall.activity.recevier;

import cloud.makeronbean.gmall.activity.utils.CacheHelper;
import org.springframework.stereotype.Component;

/**
 * @author makeronbean
 */
@Component
public class MessageReceive {

    public void receiveMessage(String message) {
        System.out.println(message);

        message = message.replaceAll("\"","");
        String[] split = message.split(":");
        if (split.length == 2) {
            CacheHelper.put(split[0],split[1]);
        }


    }
}
