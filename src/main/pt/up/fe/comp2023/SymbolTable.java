package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {

    List<String> imports;
    String className;
    String superString;
    List<Symbol> fields;
    List<String> methods;
    Map<String, Type> methodTypes;
    Map<String, List<Symbol>> methodParameters;
    Map<String, List<Symbol>> localVariables;

    public SymbolTable(JmmParserResult jmmParserResult){

        AstVisitor visitor = new AstVisitor();
        visitor.visit(jmmParserResult.getRootNode(), "");

        this.imports = visitor.getImports();
        this.className = visitor.getClassName();
        this.superString = visitor.getSuperString();
        this.fields = visitor.getFields();
        this.methods = visitor.getMethods();
        this.methodTypes = visitor.getMethodTypes();
        this.methodParameters = visitor.getMethodParameters();
        this.localVariables = visitor.getLocalVariables();

        System.out.println(this.localVariables);

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
        return methodTypes.get(s);
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return this.methodParameters.get(s);
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.localVariables.get(s);
    }
}
