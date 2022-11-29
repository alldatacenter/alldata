package cn.datax.service.codegen.engine;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.engine.AbstractTemplateEngine;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Properties;

public class VelocityTemplateEngine extends AbstractTemplateEngine {
    private static final String DOT_VM = ".vm";
    private VelocityEngine velocityEngine;

    public VelocityTemplateEngine() {
    }

    @Override
    public VelocityTemplateEngine init(ConfigBuilder configBuilder) {
        super.init(configBuilder);
        if (null == this.velocityEngine) {
            Properties p = new Properties();
            p.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            p.setProperty("file.resource.loader.path", "");
            p.setProperty("UTF-8", ConstVal.UTF8);
            p.setProperty("input.encoding", ConstVal.UTF8);
            p.setProperty("file.resource.loader.unicode", "true");
            this.velocityEngine = new VelocityEngine(p);
        }

        return this;
    }

    @Override
    public void writer(Map<String, Object> objectMap, String templatePath, String outputFile) throws Exception {
        if (!StringUtils.isBlank(templatePath)) {
            Template template = this.velocityEngine.getTemplate(templatePath, ConstVal.UTF8);
            FileOutputStream fos = new FileOutputStream(outputFile);
            Throwable var6 = null;

            String entity = objectMap.get("entity").toString();
            objectMap.put("className", entity.replace("Entity", ""));
            objectMap.put("classNameLower", StrUtil.lowerFirst(entity.replace("Entity", "")));
            try {
                OutputStreamWriter ow = new OutputStreamWriter(fos, ConstVal.UTF8);
                Throwable var8 = null;

                try {
                    BufferedWriter writer = new BufferedWriter(ow);
                    Throwable var10 = null;

                    try {
                        template.merge(new VelocityContext(objectMap), writer);
                    } catch (Throwable var54) {
                        var10 = var54;
                        throw var54;
                    } finally {
                        if (writer != null) {
                            if (var10 != null) {
                                try {
                                    writer.close();
                                } catch (Throwable var53) {
                                    var10.addSuppressed(var53);
                                }
                            } else {
                                writer.close();
                            }
                        }

                    }
                } catch (Throwable var56) {
                    var8 = var56;
                    throw var56;
                } finally {
                    if (ow != null) {
                        if (var8 != null) {
                            try {
                                ow.close();
                            } catch (Throwable var52) {
                                var8.addSuppressed(var52);
                            }
                        } else {
                            ow.close();
                        }
                    }

                }
            } catch (Throwable var58) {
                var6 = var58;
                throw var58;
            } finally {
                if (fos != null) {
                    if (var6 != null) {
                        try {
                            fos.close();
                        } catch (Throwable var51) {
                            var6.addSuppressed(var51);
                        }
                    } else {
                        fos.close();
                    }
                }

            }

            logger.debug("模板:" + templatePath + ";  文件:" + outputFile);
        }
    }

    @Override
    public String templateFilePath(String filePath) {
        return null != filePath && !filePath.contains(".vm") ? filePath + ".vm" : filePath;
    }
}
