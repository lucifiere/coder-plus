package com.lucifiere.render.freemarker;

import cn.hutool.log.StaticLog;
import com.google.common.collect.Maps;
import com.lucifiere.model.Model;
import com.lucifiere.render.AbstractRender;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public abstract class FreemarkerRender extends AbstractRender {

    public FreemarkerRender(String templateId) {
        super(templateId);
    }

    @Override
    protected String doRender(Model model) {
        Writer out = new StringWriter();
        try {
            String templateId = templateInstant.getTemplateSpec().getId();
            FreemarkerTemplateManager manager = FreemarkerTemplateManager.getManager();
            Template template = manager.getTemplate(templateId);
            Map<String, Object> dataModel = Maps.newHashMap();
            dataModel.putAll(model.extractAttrs());
            dataModel.putAll(templateInstant.getTemplateSpec().getAttrs());
            template.process(dataModel, out);
            return out.toString();
        } catch (TemplateException e) {
            StaticLog.error("模板操作执行失败！" + e.getMessage(), e);
            return null;
        } catch (IOException e) {
            StaticLog.error("IO操作失败！" + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            StaticLog.error("执行失败！" + e.getMessage(), e);
            return null;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                StaticLog.error("IO关闭失败！" + e.getMessage(), e);
            }
        }
    }

}
