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
    @Autowired DramaRepository dramaRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_IncreaseSeat(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener IncreaseSeat : " + canceled.toJson() + "\n\n");

        Drama drama = dramaRepository.findByDramaId(Long.valueOf(canceled.getDramaId()));
        drama.setReservableSeat(drama.getReservableSeat() + canceled.getSeats().intValue());
        dramaRepository.save(drama);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
