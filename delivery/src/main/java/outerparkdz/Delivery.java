package outerparkdz;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long reservationId;
    private String status;

    @PostPersist
    public void onPostPersist(){
        System.out.println("####################################################");
        System.out.println("##### Delevery - PostPersist - this.getStatus() : [" + this.getStatus() + "] #####");
        System.out.println("####################################################");

        if(this.getStatus().equals("ShipPrepared")) {
            Shipped shipped = new Shipped();
            BeanUtils.copyProperties(this, shipped);
            shipped.setStatus("DeliveryShipped");
            shipped.publishAfterCommit();
        }

        if(this.getStatus().equals("SeatCanceled")) {
            DeliveryCanceled deliveryCanceled = new DeliveryCanceled();
            BeanUtils.copyProperties(this, deliveryCanceled);
            deliveryCanceled.setStatus("DeliveryCanceled");
            deliveryCanceled.publishAfterCommit();
        }
    }

    @PostUpdate
    public void onPostUpdate(){
        System.out.println("####################################################");
        System.out.println("##### Delivery - PostUpdate - this.getStatus() : [" + this.getStatus() + "] #####");
        System.out.println("####################################################");

        if(this.getStatus().equals("DeliveryShipped")) {
            Shipped shipped = new Shipped();
            BeanUtils.copyProperties(this, shipped);
            shipped.publishAfterCommit();
        }

        if(this.getStatus().equals("DeliveryCanceled")) {
            DeliveryCanceled deliveryCanceled = new DeliveryCanceled();
            BeanUtils.copyProperties(this, deliveryCanceled);
            deliveryCanceled.publishAfterCommit();
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
