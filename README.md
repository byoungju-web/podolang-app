# 포도랑 앱 (kr.podolang.app)

podolang.kr 을 앱으로 띄우는 TWA(Trusted Web Activity) 앱입니다. 크레딧 충전은 구글 플레이 결제로 합니다.

- 화면·기능은 전부 podolang.kr (저장소 byoungju-web/podolang) 에 있습니다. 화면을 고쳐도 이 앱은 다시 만들 필요가 없습니다.
- 이 저장소에 올리면 GitHub Actions 가 `podolang.aab`(Play Console 용)와 `podolang.apk`(시험용)를 만들어 Releases 에 올립니다.
- 필요한 GitHub Secret: `PL_KEYSTORE_B64`, `PL_KS_PASS`
- 충전 상품 ID (Play Console 인앱 상품): `c100` · `c300` · `c1000`
