package outerparkdz;

public class Reserved extends AbstractEvent {

    private Long id;
    private String dramaId;
    private Long price;
    private Long seats;
    private String status;

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
    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
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