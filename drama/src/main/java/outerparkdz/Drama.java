package outerparkdz;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Drama_table")
public class Drama {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private Integer reservableSeat;

    @PostPersist
    public void onPostPersist(){
        DramaRegistered dramaRegistered = new DramaRegistered();
        BeanUtils.copyProperties(this, dramaRegistered);
        dramaRegistered.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        SeatModified seatModified = new SeatModified();
        BeanUtils.copyProperties(this, seatModified);
        seatModified.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getDramaId() {
        return dramaId;
    }

    public void setDramaId(Long dramaId) {
        this.dramaId = dramaId;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Integer getReservableSeat() {
        return reservableSeat;
    }

    public void setReservableSeat(Integer reservableSeat) {
        this.reservableSeat = reservableSeat;
    }




}
