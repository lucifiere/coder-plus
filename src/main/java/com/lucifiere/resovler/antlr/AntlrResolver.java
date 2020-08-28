package com.lucifiere.resovler.antlr;

import com.lucifiere.antlr.MySqlLexer;
import com.lucifiere.antlr.MySqlParser;
import com.lucifiere.antlr.MySqlParserBaseListener;
import com.lucifiere.common.FiledType;
import com.lucifiere.extract.Model;
import com.lucifiere.extract.table.TableField;
import com.lucifiere.extract.table.TableModel;
import com.lucifiere.resovler.ResolverReq;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Optional;

/**
 * 基于ANTLR的属性值解析工具
 *
 * @author XD.Wang
 * Date 2020-7-25.
 */
public class AntlrResolver extends MySqlParserBaseListener {

    private TableModel tableModel;

    private TableField cursor;

    private TableField getCursor() {
        cursor = Optional.ofNullable(cursor).orElse(new TableField());
        return cursor;
    }

    private void resetCursor() {
        cursor = null;
    }

    public void setTableModel(TableModel tableModel) {
        this.tableModel = tableModel;
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    @Override
    public void enterTableName(MySqlParser.TableNameContext ctx) {
        tableModel.setName(extractContent(ctx));
    }

    @Override
    public void enterColumnDeclaration(MySqlParser.ColumnDeclarationContext ctx) {
        var currentFiled = getCursor();
        var nameNode = ctx.getChild(MySqlParser.UidContext.class, 0);
        currentFiled.setName(extractContent(nameNode));
    }

    @Override
    public void exitColumnDeclaration(MySqlParser.ColumnDeclarationContext ctx) {
        tableModel.addTableFiled(getCursor());
        resetCursor();
    }

    @Override
    public void enterColumnDefinition(MySqlParser.ColumnDefinitionContext ctx) {
        var currentFiled = getCursor();
        var dataTypeNode = ctx.getChild(MySqlParser.DataTypeContext.class, 0);
        var dataTypeNameNode = dataTypeNode.getChild(0);
        Optional.ofNullable(dataTypeNameNode).ifPresent(node -> currentFiled.setType(FiledType.getBySqlType(extractContent(node))));
        var commentNode = ctx.getChild(MySqlParser.CommentColumnConstraintContext.class, 0);
        Optional.ofNullable(commentNode).ifPresent(node -> currentFiled.setComment(extractContent(node.getChild(1))));
    }

    @Override
    public void exitColumnDefinition(MySqlParser.ColumnDefinitionContext ctx) {
    }

    private String extractContent(ParseTree treeNode) {
        return treeNode == null ? null : treeNode.getText().toLowerCase();
    }

    @Override
    public Model resolve(ResolverReq resolverReq) {
        if (resolverReq instanceof AntlrResolverReq req) {
            var input = CharStreams.fromString(req.sourceCode().toUpperCase());
            // 词法解析
            var lexer = new MySqlLexer(input);
            var tokens = new CommonTokenStream(lexer);
            // 语法解析
            var parser = new MySqlParser(tokens);
            // 指定根语法节点
            MySqlParser.CreateTableContext ctDdlTree = parser.createTable();
            // 创建一个树遍历器
            var walker = new ParseTreeWalker();
            var model = new TableModel();
            // 注册回调，开始遍历树
            walker.walk(this, ctDdlTree);
            return model;
        }
        throw new UnsupportedOperationException("类型匹配有误！");
    }

}
