package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SemanticAnalysis implements JmmAnalysis {

    List<Symbol> currentMethodVariables = new ArrayList<>();

    List<Report> reports = new ArrayList<>();

    SymbolTable table;

    int counter = 0;

    public boolean isInImports(String className){
        System.out.println(table.getImports());
        if (table.getImports().contains(className)){
            return true;
        }
        return false;
    }

    public boolean checkSymbolExists(List<Symbol> symbolList, String symbolName){
        if (symbolList == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Variable " + symbolName + " has no previous declaration"));
            this.counter+=1;
            return false;
        }
        for (Symbol symbol : symbolList){
            if (symbol.getName().equals(symbolName)){
                return true;
            }
        }
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                "Variable " + symbolName + " has no previous declaration"));
        this.counter+=1;
        return false;
    }

    public Type getSymbolType(String symbolName){
        if (this.currentMethodVariables == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Variable " + symbolName + " has no previous declaration"));
            this.counter+=1;
            return new Type(null, false);
        }
        for (Symbol symbol : this.currentMethodVariables){
            if (symbol.getName().equals(symbolName)){
                return symbol.getType();
            }
        }

        return new Type(null, false);
    }

    public void  checkMathOperation(JmmNode jmmNode, String opType){

        JmmNode child1 = jmmNode.getChildren().get(0);
        JmmNode child2 = jmmNode.getChildren().get(1);

        String type1 = "int";
        String type2 = "int";


        if (child1.toString().contains("IdentifierExpr")){
            type1 = getSymbolType(child1.get("value")).getName();
        }
        else if (child1.toString().contains("ArrayNew")){
            type1 = "array";
            return;
        }

        if (child2.toString().contains("IdentifierExpr")){
            type2 = getSymbolType(child2.get("value")).getName();
        }
        else if (child2.toString().contains("ArrayNew")){
            type2 = "array";
        }

        if ((type1 != "int" && type1 != "float") || (type2 != "int" && type2 != "float")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot carry out " + opType + " operation between type: "  + type1 + " and type: " + type2));
            this.counter+=1;
        }

    }

    public void checkLogicalOperation(JmmNode jmmNode){
        JmmNode child1 = jmmNode.getChildren().get(0);
        JmmNode child2 = jmmNode.getChildren().get(1);

        String type1 = "boolean";
        String type2 = "boolean";

        type2 = getSymbolType(child2.get("value")).getName();
        type1 = getSymbolType(child1.get("value")).getName();

        if (!Objects.equals(type1, "boolean") || !Objects.equals(type2, "boolean")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot carry out logical operation between type: "  + type1 + " and type: " + type2));
            this.counter+=1;
        }
    }
    public void checkNewArray(JmmNode jmmNode){

    }

    public String checkNewClass(String varType, JmmNode jmmNode){
        String classType = jmmNode.get("value");

        System.out.println("CLASS TYPE: " + classType);

        if (!isInImports(classType)) {

            if (!Objects.equals(varType, classType)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                        "Cannot assign variable of type " + classType + " to variable of type: " + varType));
                this.counter += 1;
            }
        }

        return classType;
    }

    public void checkAssignment(JmmNode jmmNode){

        String var1 = jmmNode.getChildren().get(0).get("value");

        System.out.println("VALUE 1: " + var1);

        if (!isInImports(getSymbolType(var1).getName())) {

            if (checkSymbolExists(currentMethodVariables, var1)) {
                JmmNode child2 = jmmNode.getChildren().get(1);
                Type type1 = getSymbolType(var1);
                String type1Name = type1.getName();
                String child2Type = "";
                if (child2.toString() != "ArrayNew") {
                    checkNewArray(child2);
                }
                if (child2.toString().contains("IdentifierExpr")) {
                    checkSymbolExists(currentMethodVariables, child2.get("value"));
                }
                if (child2.toString().contains("Integer")) {
                    child2Type = "int";
                }
                if (child2.toString().contains("Boolean")) {
                    child2Type = "boolean";
                }
                if (child2.toString().contains("ClassNew")) {
                    child2Type = checkNewClass(type1Name, child2);
                }

                if (!isInImports(child2Type)){

                    System.out.println("Child 2 TYPE: " + child2Type);
                    if (!Objects.equals(child2Type, type1Name)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                                "Cannot assign variable of type " + child2Type + " to variable of type: " + type1Name));
                    }
                }
            }
        }

    }

    public void checkArrayAccess(JmmNode jmmNode){
        String arrayID = jmmNode.getChildren().get(0).get("value");
        Type arrayType = getSymbolType(arrayID);

        if (!arrayType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot access index of a non-array"));
            this.counter+=1;
        }
    }

    public void checkMethodCall(JmmNode jmmNode){
        String funcCaller = jmmNode.getChildren().get(0).get("value");
        String funcName = jmmNode.getChildren().get(1).get("value");
        System.out.println("SUPER: " + this.table.getSuper());
        if ((this.table.getSuper() != null && (!isInImports(getSymbolType(funcCaller).getName()) && !isInImports(this.table.getSuper()))) || !isInImports(getSymbolType(funcCaller).getName())){

            if (!isInImports(getSymbolType(funcCaller).getName()) && !isInImports(this.table.getSuper())) {

                if (!this.table.getMethods().contains(funcName)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                            "Call to method: " + funcName + " cannot be completed because it does not exist."));
                    this.counter += 1;
                }
            }
        }
    }

    public void checkCondition(JmmNode jmmNode){
        String childType = jmmNode.getChildren().toString();
        if (!childType.contains("Boolean") && !childType.contains("LogicalOp")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Invalid Condition"));
            this.counter += 1;
        }
    }

    public void checkReturn(JmmNode jmmNode){
        for (JmmNode child : jmmNode.getChildren()){
            traverseTree(child);
        }
    }

    public void traverseTree(JmmNode jmmNode){

        if (jmmNode.toString().contains("MethodDeclaration")){
            this.currentMethodVariables = this.table.getLocalVariables(jmmNode.getChildren().get(1).get("value"));
        }
        if (jmmNode.toString().contains("MultiplicativeOp")){
            checkMathOperation(jmmNode, "multiplicative");
        }
        if (jmmNode.toString().contains("AdditiveOp")){
            checkMathOperation(jmmNode, "additive");
        }
        if (jmmNode.toString().contains("AssignmentOp")){
            checkAssignment(jmmNode);
        }
        if (jmmNode.toString().contains("IdentifierExpr")){
            checkSymbolExists(this.currentMethodVariables, jmmNode.get("value"));
        }
        if (jmmNode.toString().contains("BinaryOp")){
            checkArrayAccess(jmmNode);
        }
        if (jmmNode.toString().contains("MethodCall")){
            checkMethodCall(jmmNode);
        }
        if (jmmNode.toString().contains("LogicalOp")){
            checkLogicalOperation(jmmNode);
        }
        if (jmmNode.toString().contains("Condition")){
            checkCondition(jmmNode);
        }
        if (jmmNode.toString().contains("ReturnStatement")){
            checkReturn(jmmNode);
        }
        else if (jmmNode.getChildren() != null){
            for (JmmNode child : jmmNode.getChildren()){
                traverseTree(child);
            }
        }

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        var table = new SymbolTable(jmmParserResult);
        this.table = table;

        System.out.println(jmmParserResult.getRootNode().toTree());

        //SemanticVisitor semanticVisitor = new SemanticVisitor();

        //semanticVisitor.visit(jmmParserResult.getRootNode());

        //System.out.println(table.print());

        traverseTree(jmmParserResult.getRootNode());

        return new JmmSemanticsResult(jmmParserResult, table, this.reports);
    }
}
