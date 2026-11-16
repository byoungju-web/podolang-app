package kr.podolang.app;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;

/**
 * 포도랑 화면(podolang.kr)이 앱에게 구글 플레이 결제를 맡길 때 받는 곳.
 * 화면의 window.getDigitalGoodsService('https://play.google.com/billing') 가 이 서비스를 거쳐 Play Billing 을 부른다.
 * (구글 공식 Bubblewrap 의 PlayBillingFeature 와 같은 내용)
 */
public class DelegationService extends com.google.androidbrowserhelper.trusted.DelegationService {
    @Override
    public void onCreate() {
        super.onCreate();
        registerExtraCommandHandler(new DigitalGoodsRequestHandler(getApplicationContext()));
    }
}
