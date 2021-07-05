
package outerparkdz.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="drama", url="http://localhost:8081")
//@FeignClient(name="drama", url="${api.url.drama}")
public interface DramaService {

    @RequestMapping(method= RequestMethod.GET, path="/chkAndModifySeat")
    public boolean modifySeat(@RequestParam("dramaId") String dramaId,
                              @RequestParam("seats") int seatCount);

}