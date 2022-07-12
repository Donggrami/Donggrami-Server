package donggrami.earth1round.src.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import donggrami.earth1round.src.auth.model.PostUserRes;
import com.google.gson.JsonElement;
import donggrami.earth1round.config.BaseResponse;
import donggrami.earth1round.config.secret.Secret;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;



@RestController
@RequiredArgsConstructor
//@RequestMapping(value = "/login/getKakaoAuthUrl")
public class Controller {
    private final String KAKAO_CLIENT_ID = Secret.KAKAO_CLIENT_ID;
    private final String KAKAO_REDIRECT_URI = "http://localhost:8080/login/kakao";

    @Autowired
    private final Service service;

//    public @ResponseBody String getKakaoAuthUrl(HttpServletRequest request) throws Exception {
//        String reqUrl =
//                "https://kauth.kakao.com/oauth/authorize"
//                        + "?client_id="
//                        + KAKAO_CLIENT_ID
//                        + "&redirect_uri="
//                        + KAKAO_REDIRECT_URI
//                        + "&response_type=code";
//
//        return reqUrl;
//    }

    /**
     * 회원가입 API
     * [POST] /login/kakao?code=
     * @return BaseResponse<PostUserRes> - user_id 반환
     */
    // 카카오 연동정보 조회
    @RequestMapping(value = "/login/kakao")
    public BaseResponse<PostUserRes> kakaoLogin(@RequestParam(value = "code", required = false) String code) throws Exception {
//        System.out.println("code : " + code);
        String access_Token = getKakaoAccessToken(code);
//        System.out.println("accessToken : " + access_Token);

        HashMap<String, Object> userInfo = getKakaoUserInfo(access_Token);
//        System.out.println("email : " + userInfo.get("email"));
//        System.out.println("nickname : " + userInfo.get("nickname"));

        Long longPostUserRes = service.createUser(userInfo);
        PostUserRes postUserRes = new PostUserRes(new Long(longPostUserRes));
//        System.out.println(postUserRes);

        return new BaseResponse<>(postUserRes);
    }

    //토큰발급
    public String getKakaoAccessToken(String authorize_code) {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";

        try {
            URL url = new URL(reqURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // POST 요청
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            //	POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=" + KAKAO_CLIENT_ID);  // 발급받은 key
            sb.append("&redirect_uri=" + KAKAO_REDIRECT_URI); // 설정해 놓은 경로
            sb.append("&code=" + authorize_code);
            bw.write(sb.toString());
            bw.flush();

            //    결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            //    요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            //  JSON파싱 객체 생성
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            System.out.println("access_token : " + access_Token);
            System.out.println("refresh_token : " + refresh_Token);

            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return access_Token;
    }

    //사용자 정보 조회
    public HashMap<String, Object> getKakaoUserInfo(String access_Token) {

        HashMap<String, Object> userInfo = new HashMap<String, Object>();
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 요청에 필요한 Header에 포함될 내용
            conn.setRequestProperty("Authorization", "Bearer " + access_Token);

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            JsonObject properties = element.getAsJsonObject().get("properties").getAsJsonObject();
            JsonObject kakao_account = element.getAsJsonObject().get("kakao_account").getAsJsonObject();

            String nickname = properties.getAsJsonObject().get("nickname").getAsString();
            String email = kakao_account.getAsJsonObject().get("email").getAsString();

            userInfo.put("accessToken", access_Token);
            userInfo.put("nickname", nickname);
            userInfo.put("email", email);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return userInfo;
    }
}

