package outerparkdz;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String dramaId;
    private Long seats;
    private String status;

    @PostPersist
    public void onPostPersist() throws Exception {
        
        System.out.println("##### Reservation - call drama service by configMap #####");
        boolean rslt = ReservationApplication.applicationContext.getBean(outerparkdz.external.DramaService.class)
            .modifySeat(this.getDramaId(), this.getSeats().intValue());

        if(rslt) {
            System.out.println("##### Reservation - Result : true #####");
            this.setStatus("SeatsReserved");
            Reserved reserved = new Reserved();
            reserved.setId(this.getId());
            BeanUtils.copyProperties(this, reserved);

            reserved.publishAfterCommit();
        }
        else
        {
            System.out.println("##### Reservation - Result : false - too big seats count #####");
            throw new Exception("Too big seats Count");
        }
    }

    @PreRemove
    public void onPreRemove(){
        Canceled canceled = new Canceled();
        canceled.setId(this.getId());
        canceled.setStatus("SeatCanceled");
        BeanUtils.copyProperties(this, canceled);
        canceled.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getDramaId() {
        return dramaId;
    }

    public void setDramaId(String dramaId) {
        this.dramaId = dramaId;
    }
    public Long getSeats() {
        return seats;
    }

    public void setSeats(Long seats) {
        this.seats = seats;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
