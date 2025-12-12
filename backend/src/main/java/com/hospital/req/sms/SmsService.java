package com.hospital.req.sms;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
  private final SmsClient client;

  public SmsService(SmsClient client) {
    this.client = client;
  }

  public String notifyPhones(String phoneCsv, String content) {
    return client.sendWithDefaultTemplate(phoneCsv, content);
  }
}