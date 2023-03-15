package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.function.BiFunction;

public class AstVisitor extends AJmmVisitor {

    @Override
    protected void buildVisitor() {

    }

    @Override
    public Object visit(JmmNode jmmNode) {
        return super.visit(jmmNode);
    }

    @Override
    public void addVisit(Object kind, BiFunction method) {
        super.addVisit(kind, method);
    }
}
