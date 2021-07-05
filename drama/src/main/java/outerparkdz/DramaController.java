package outerparkdz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class DramaController {

        @Autowired
        DramaRepository dramaRepository;

        @RequestMapping(value = "/chkAndModifySeat",
                method = RequestMethod.GET,
                produces = "application/json;charset=UTF-8")
        public Boolean modifySeat(HttpServletRequest request, HttpServletResponse response) throws Exception {

                System.out.println("##### /drama/modifySeat  called #####");

                boolean status = false;
                Long dramaId = Long.valueOf(request.getParameter("dramaId"));
                int seats = Integer.parseInt(request.getParameter("seats"));

                System.out.println("### dramaId : " + dramaId.toString());
                System.out.println("### seats : " + Integer.toString(seats));

                Drama drama = dramaRepository.findByDramaId(dramaId);

                System.out.println("### drama : " + Integer.toString(drama.getReservableSeat()));

                if(drama.getReservableSeat() >= seats) {

                        System.out.println("##### Process Reserve.. #####");
                        status = true;
                        drama.setReservableSeat(drama.getReservableSeat() - seats);
                        dramaRepository.save(drama);

                }

                System.out.println("##### Status #####" + Boolean.toString(status));

                return status;
        }
 }
