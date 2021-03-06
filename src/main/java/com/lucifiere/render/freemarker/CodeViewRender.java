package com.lucifiere.render.freemarker;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
import com.lucifiere.model.Model;
import com.lucifiere.model.TableModel;
import com.lucifiere.render.View;
import com.lucifiere.render.views.CodeView;
import com.lucifiere.utils.CodeStyle;

public class CodeViewRender extends FreemarkerRender {

    public CodeViewRender(String templateId) {
        super(templateId);
    }

    @Override
    protected void processModelBeforeRender(Model model) {
        super.processModelBeforeRender(model);
    }

    @Override
    protected View createView(String content, Model model) {
        Preconditions.checkArgument(StrUtil.isNotBlank(content));
        Preconditions.checkArgument(model instanceof TableModel);
        var codeView = new CodeView();
        codeView.setContent(content);
        codeView.setFileSetting(template.getTemplateSpec().getFileSetting());
        var tableModel = (TableModel) model;
        codeView.setName(tableModel.getName());
        return codeView;
    }

}
