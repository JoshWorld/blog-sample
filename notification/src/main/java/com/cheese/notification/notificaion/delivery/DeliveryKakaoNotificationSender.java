package com.cheese.notification.notificaion.delivery;

import com.cheese.notification.kakao.KakaoNotificaionSender;
import com.cheese.notification.kakao.KakaoNotificationDto;
import com.cheese.notification.kakao.KakaoNotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class DeliveryKakaoNotificationSender extends KakaoNotificaionSender implements DeliveryNotificationSender {

    public DeliveryKakaoNotificationSender(KakaoNotificationRepository kakaoNotificationRepository) {
        super(kakaoNotificationRepository);
    }

    @Override
    public void send(DeliveryMessageDto.Message dto) {
        create(buildKaKaoNotificationDto(dto));
    }


    private KakaoNotificationDto.Creation buildKaKaoNotificationDto(DeliveryMessageDto.Message dto) {
        return KakaoNotificationDto.Creation.builder()
                .subject(writeSubject(dto))
                .content(writeContent(dto))
                .mobile(dto.getReceiver().getMobile())
                .templateCode("COD001")
                .build();
    }

    private String writeSubject(DeliveryMessageDto.Message dto) {
        return dto.getDelivery().getItemName() + "물품이  도착했습니다.";
    }

    private String writeContent(DeliveryMessageDto.Message dto) {
        return dto.getSender().getName() + " 님이 보내주신 물품이 도착완료 했습니다.";
    }
}
