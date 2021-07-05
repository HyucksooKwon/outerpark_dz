package outerparkdz;

import java.util.Optional;

import outerparkdz.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ReservationRepository reservationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_UpdateStatus(@Payload Shipped shipped){

        if(!shipped.validate()) return;
        System.out.println("\n\n##### listener UpdateStatus : " + shipped.toJson() + "\n\n");

        Optional<Reservation> optionalReservation = reservationRepository.findById(shipped.getReservationId());
        Reservation reservation = optionalReservation.get();
        reservation.setStatus(shipped.getStatus());
        reservationRepository.save(reservation);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
