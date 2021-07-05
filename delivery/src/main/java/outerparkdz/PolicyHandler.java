package outerparkdz;

import outerparkdz.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReserved_PrepareShip(@Payload Reserved reserved){

        if(!reserved.validate()) return;

        System.out.println("\n\n##### listener PrepareShip : " + reserved.toJson() + "\n\n");

        // Sample Logic //
        Delivery delivery = new Delivery();
        delivery.setReservationId(reserved.getId());
        delivery.setStatus("ShipPrepared");
        deliveryRepository.save(delivery);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_CanceledDelivery(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener CanceledDelivery : " + canceled.toJson() + "\n\n");

        // Cancel shipment
        Delivery delivery = deliveryRepository.findByReservationId(canceled.getId());
        delivery.setStatus("DeliveryCanceled");
        deliveryRepository.save(delivery);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
