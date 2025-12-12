package com.hospital.req.sms;

import com.example.EncryptUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Component
public class SmsClient {

  private final SmsProperties props;
  private final RestTemplate restTemplate = new RestTemplate();
  private final SecureRandom rnd = new SecureRandom();

  public SmsClient(SmsProperties props) {
    this.props = props;
  }

  public String send(String phoneCsv, String templateId, String messageContent) {
    String type = "1";
    String requestId = System.currentTimeMillis() + String.format("%06d", rnd.nextInt(1_000_000));
    String serialNumber = build20DigitSerial();

    // 1) 签名用 map（必须包含公共字段 + 短信字段）
    Map<String, Object> map4Sign = new HashMap<>();
    map4Sign.put("spCode", props.getSpCode());
    map4Sign.put("loginName", props.getLoginName());
    map4Sign.put("apiKey", props.getApiKey());
    map4Sign.put("requestId", requestId);

    map4Sign.put("userNumber", phoneCsv);
    map4Sign.put("templateId", templateId);
    map4Sign.put("messageContent", messageContent);
    map4Sign.put("serialNumber", serialNumber);
    map4Sign.put("type", type);

    String sign = EncryptUtil.getSign(map4Sign);

    // 2) body：只放短信字段
    Map<String, Object> body = new HashMap<>();
    body.put("templateId", templateId);
    body.put("userNumber", phoneCsv);
    body.put("messageContent", messageContent);
    body.put("serialNumber", serialNumber);
    body.put("type", type);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
    headers.set("x-sp-code", props.getSpCode());
    headers.set("x-app-key", props.getLoginName());
    headers.set("x-request-id", requestId);
    headers.set("x-sign", sign);

    ResponseEntity<String> resp = restTemplate.exchange(
        props.getUrl(), HttpMethod.POST, new HttpEntity<>(body, headers), String.class
    );

    return resp.getBody();
  }

  public String sendWithDefaultTemplate(String phoneCsv, String messageContent) {
    return send(phoneCsv, props.getTemplateId(), messageContent);
  }

  private String build20DigitSerial() {
    long ms = System.currentTimeMillis(); // 13位
    int r = rnd.nextInt(10_000_000);      // 7位
    return ms + String.format("%07d", r);
  }
}

