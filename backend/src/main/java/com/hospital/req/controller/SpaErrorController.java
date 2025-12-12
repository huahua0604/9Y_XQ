package com.hospital.req.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int status = statusObj != null ? Integer.parseInt(statusObj.toString()) : 500;
        String uri = uriObj != null ? uriObj.toString() : "";

        // 只对 404 进行 SPA 回退
        if (status == HttpStatus.NOT_FOUND.value()) {
            boolean isApi = uri.startsWith("/api/")
                    || uri.startsWith("/files/")
                    || uri.startsWith("/actuator/")
                    || uri.startsWith("/v3/")
                    || uri.startsWith("/swagger-ui/");
            boolean isStatic = uri.matches(".*\\.(js|css|map|png|jpg|jpeg|gif|svg|ico|woff2?)$");

            if (!isApi && !isStatic) {
                // 前端路由交给 Vue
                return "forward:/index.html";
            }
        }
        // 其他情况交给默认错误处理
        return null;
    }
}
