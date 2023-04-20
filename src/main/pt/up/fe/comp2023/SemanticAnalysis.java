package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
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

    public Pair<Boolean, Type> checkVariableExists(String varName){

        if (this.currentMethodVariables == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Variable with the name: " + varName + " has no previous assignment."));

            return new Pair<>(false, null);
        }

        for (Symbol variable : this.currentMethodVariables){
            if (variable.getName().equals(varName)){
                return new Pair<>(true, variable.getType());
            }
        }

        for (Symbol variable : this.table.getFields()){
            if (variable.getName().equals(varName)){
                return new Pair<>(true, variable.getType());
            }
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                "Variable with the name: " + varName + " has no previous assignment."));

        return new Pair<>(false, null);
    }

    public Pair<Boolean, Type> checkMethodExists(String methodName){
        for (String method : this.table.getMethods()) {
            if (method.equals(methodName)){
                return new Pair<>(true, this.table.getReturnType(methodName));
            }
        }
        return new Pair<>(false, null);
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

   public Pair<Boolean, Type> checkClassNew(JmmNode jmmNode){

        return new Pair<>(false, null);
   }

    public Pair<Boolean, Type> checkArrayNew(JmmNode jmmNode){

        return new Pair<>(false, null);
    }

    public void checkAssignment(JmmNode jmmNode){

        JmmNode assignment1 = jmmNode.getChildren().get(0);
        JmmNode assignment2 = jmmNode.getChildren().get(1);

        Pair<Boolean, Type> checkedVar = checkVariableExists(assignment1.get("value"));
        Pair<Boolean, Type> checkedVar2;

        boolean validAssignment = false;

        checkedVar2 = traverseTree(assignment2);

        if (checkedVar2.a && checkedVar.a) {

            if (checkedVar2.b == checkedVar.b) {
                validAssignment = true;
            }
            if (isInImports(checkedVar2.b.getName()) && isInImports(checkedVar.b.getName())){
                validAssignment = true;
            }
            if (checkedVar2.b.getName().equals(this.table.getSuper()) || checkedVar.b.getName().equals(this.table.getSuper())) {

                validAssignment = true;
            }
            if (!validAssignment){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                        "Cannot assign variable of type " + checkedVar2.b.getName() + " to variable of type: " + checkedVar.b.getName()));

            }
        }
    }

    public void checkArrayAccess(JmmNode jmmNode){
        String arrayName = jmmNode.getJmmChild(0).get("value");

        Pair<Boolean, Type> checkedVar = checkVariableExists(arrayName);
        Pair<Boolean, Type> checkedVar2;

        boolean validAccess = false;

        checkedVar2 = traverseTree(jmmNode.getJmmChild(1));

        if (checkedVar2.a && checkedVar.a && checkedVar.b.isArray()){

            if (checkedVar2.b == checkedVar.b){
                validAccess = true;
            }
            if (isInImports(checkedVar2.b.getName()) || isInImports(checkedVar.b.getName())) {
                validAccess = true;
            }
            if (!validAccess){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                        "Cannot assign variable of type " + checkedVar2.b.getName() + " to variable of type: " + checkedVar.b.getName()));

            }
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                "Cannot assign variable of type " + checkedVar2.b.getName() + " to variable of type: " + checkedVar.b.getName()));

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

    public Pair<Boolean, Type> traverseTree(JmmNode jmmNode){

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
        if (jmmNode.toString().contains("ArrayNew")){
            return checkArrayNew(jmmNode);
        }
        if (jmmNode.toString().contains("ClassNew")){
            return checkClassNew(jmmNode);
        }
        if (jmmNode.toString().contains("IdentifierExpr")){
            return checkVariableExists(jmmNode.get("value"));
        }
        if (jmmNode.toString().contains("BinaryOp")){
            checkArrayAccess(jmmNode);
        }
        if (jmmNode.toString().contains("MethodCall")){
            checkMethodCall(jmmNode);
        }
        if (jmmNode.toString().contains("Condition")){
            checkCondition(jmmNode);
        }
        if (jmmNode.toString().contains("Integer")){
            return new Pair<>(true, new Type("int", false));
        }
        if (jmmNode.toString().contains("ReturnStatement")){
            checkReturn(jmmNode);
        }
        else if (jmmNode.getChildren() != null){
            for (JmmNode child : jmmNode.getChildren()){
                traverseTree(child);
            }
        }

        return new Pair<>(false, null);
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
