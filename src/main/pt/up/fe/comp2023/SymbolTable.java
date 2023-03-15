package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {

    List<String> imports;
    String className;
    String superString;
    List<Symbol> fields;
    List<String> methods;
    Type returnType;
    List<Symbol> parameters;
    List<Symbol> localVariables;

    public SymbolTable(JmmParserResult jmmParserResult){
        Map<String, Object> infoMap = new HashMap<>();

        infoMap.put("imports", imports);
        infoMap.put("className", className);
        infoMap.put("super", superString);
        infoMap.put("fields", fields);
        infoMap.put("methods", methods);
        infoMap.put("returnType", returnType);
        infoMap.put("parameters", parameters);
        infoMap.put("localVariables", localVariables);

        AstVisitor visitor = new AstVisitor();

        visitor.visit(jmmParserResult.getRootNode(), infoMap);


    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superString;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return this.methods;
    }

    @Override
    public Type getReturnType(String s) {
        return this.returnType;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return this.parameters;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.localVariables;
    }
}
