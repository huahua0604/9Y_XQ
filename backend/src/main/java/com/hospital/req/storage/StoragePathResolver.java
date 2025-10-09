package com.hospital.req.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 把 upload-dir(相对路径) 解析为“相对 JAR 目录”的绝对路径 */
@Component
public class StoragePathResolver {
    private final Path root;

    public StoragePathResolver(@Value("${app.upload-dir:${upload-dir:./data/uploads}}") String configured) {
        File jarDir = new ApplicationHome(StoragePathResolver.class).getDir();
        Path base = (jarDir != null) ? jarDir.toPath() : Paths.get(".").toAbsolutePath();
        Path cfg = Paths.get(configured == null ? "./data/uploads" : configured);
        this.root = cfg.isAbsolute() ? cfg.normalize() : base.resolve(cfg).normalize();
    }

    public Path root() { return root; }
}
