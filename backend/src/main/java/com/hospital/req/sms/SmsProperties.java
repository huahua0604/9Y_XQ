package com.hospital.req.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
  private String url;
  private String spCode;
  private String loginName;
  private String apiKey;
  private String templateId;

}

