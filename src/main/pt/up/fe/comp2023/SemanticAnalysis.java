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
    List<Symbol> currentMethodParameters = new ArrayList<>();

    List<Report> reports = new ArrayList<>();

    SymbolTable table;

    int counter = 0;

    public boolean isInImports(String className){
        if (table.getImports().contains(className)){
            return true;
        }
        return false;
    }

    public Pair<Boolean, Type> checkVariableExists(String varName){

        if (this.currentMethodVariables != null){

            for (Symbol variable : this.currentMethodVariables){
                if (variable.getName().equals(varName)){
                    return new Pair<>(true, variable.getType());
                }
            }
        }

        if (this.currentMethodParameters != null) {

            for (Symbol variable : this.currentMethodParameters) {
                if (variable.getName().equals(varName)) {
                    return new Pair<>(true, variable.getType());
                }
            }
        }

        if (this.table.getFields() != null) {
            for (Symbol variable : this.table.getFields()) {
                if (variable.getName().equals(varName)) {
                    return new Pair<>(true, variable.getType());
                }
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

    public Pair<Boolean, Type>  checkOperation(JmmNode jmmNode, String opType){

        JmmNode child1 = jmmNode.getChildren().get(0);
        JmmNode child2 = jmmNode.getChildren().get(1);

        String expectedType = "";

        boolean validOperation = false;

        if (opType.equals("logical")){
            expectedType = "boolean";
        }
        if (opType.equals("additive") || opType.equals("multiplicative")) {
            expectedType = "int";
        }

        String type1 = "";
        String type2 = "";

        Pair<Boolean, Type> var1Check = traverseTree(child1);
        Pair<Boolean, Type> var2Check = traverseTree(child2);

        if (var1Check.a && var2Check.a){
            if (var1Check.b.getName().equals(expectedType) && var2Check.b.getName().equals(expectedType)){
                validOperation = true;
            }
        }

        if (!validOperation){
            System.out.println("VAR 2CHECK : " + var2Check);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot execute operation between variable types"));
            this.counter+=1;
            return new Pair<>(false, null);
        }

        return new Pair<>(true, new Type(expectedType, false));
    }

   public Pair<Boolean, Type> checkClassNew(JmmNode jmmNode){

        return new Pair<>(false, null);
   }

    public Pair<Boolean, Type> checkArrayNew(JmmNode jmmNode){

        String arrayType = jmmNode.getJmmChild(0).get("t");
        Pair<Boolean, Type> checkArrayIndex = traverseTree(jmmNode.getJmmChild(1));

        if (!checkArrayIndex.b.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot initialize array with non-int length."));
            this.counter+=1;

            return new Pair<>(false, null);
        }

        return new Pair<>(true, new Type(arrayType, true));
    }

    public Pair<Boolean, Type> checkAssignment(JmmNode jmmNode){

        JmmNode assignment1 = jmmNode.getChildren().get(0);
        JmmNode assignment2 = jmmNode.getChildren().get(1);

        Pair<Boolean, Type> checkedVar = traverseTree(assignment1);
        Pair<Boolean, Type> checkedVar2;

        boolean validAssignment = false;

        if (!checkedVar.a){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Variable does not exist."));
            return new Pair<>(false, null);
        }

        checkedVar2 = traverseTree(assignment2);

        if (checkedVar2.a && checkedVar.a) {

            if (checkedVar2.b.equals(checkedVar.b)) {
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
                        "Cannot assign variable of incompatible types."));
                return new Pair<>(false, null);

            }
        }

        return new Pair<>(true, checkedVar2.b);
    }

    public Pair<Boolean, Type> checkArrayAccess(JmmNode jmmNode){

        System.out.println("JMM NODE IS: " + jmmNode.toString());
        Pair<Boolean, Type> checkedVar = traverseTree(jmmNode.getJmmChild(0));
        Pair<Boolean, Type> checkedVar2;

        boolean validAccess = false;

        if (!checkedVar.b.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot access index of non array object."));
            return new Pair<>(false, null);
        }

        checkedVar2 = traverseTree(jmmNode.getJmmChild(1));

        System.out.println("CHECKED VAR IS: " + checkedVar2);

        if (checkedVar2.a && checkedVar.a){

            if (Objects.equals(checkedVar2.b.getName(), "int")){
                validAccess = true;
            }
            if (!validAccess){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                        "Cannot assign variable of incompatible types."));
                return new Pair<>(false, null);
            }
        }

        return new Pair<>(true, checkedVar2.b);

    }

    public Pair<Boolean, Type> checkMethodCall(JmmNode jmmNode){

        Pair<Boolean, Type> checkMethodCaller = traverseTree(jmmNode.getJmmChild(0));
        String methodCalled = jmmNode.getJmmChild(1).get("value");

        Pair<Boolean, Type> checkMethodCall = checkMethodExists(methodCalled);

        if (!checkMethodCaller.a){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Method Caller does not exist."));
            return new Pair<>(false, null);
        }

        if (isInImports(checkMethodCaller.b.getName()) || isInImports(this.table.getSuper())){
            return new Pair<>(true, null);
        }

        if (!checkMethodCall.a){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Method called does not exist."));
            return new Pair<>(false, null);
        }
        if (!isInImports(checkMethodCaller.b.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Class not imported."));
            return new Pair<>(false, null);
        }

        boolean validMethodCall = true;


        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getIndexOfSelf() != 0 && child.toString().contains("IdentifierExpr")) {
                Pair<Boolean, Type> checkParam = checkVariableExists(child.get("value"));
                if (this.table.getParameters(methodCalled).get(child.getIndexOfSelf()).getName().equals(checkParam.b.getName())) {
                    continue;
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                            "Invalid parameter in function call."));
                    validMethodCall = false;
                }
            }
            if (child.getIndexOfSelf() == jmmNode.getNumChildren()-1){
                Pair<Boolean, Type> checkReturn = traverseTree(child);

                if (!checkReturn.b.getName().equals(checkMethodCall.b.getName())){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                            "Return type is not compatible with function declaration."));
                }
            }
        }


        if (!validMethodCall){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Cannot assign variable of type " + " to variable of type: "));
            return new Pair<>(false, null);
        }

        return new Pair<>(false, null);
    }

    public void checkCondition(JmmNode jmmNode){
        String childType = jmmNode.getChildren().toString();

        if (!childType.contains("Boolean") && !childType.contains("LogicalOp")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, this.counter,
                    "Invalid Condition"));
            this.counter += 1;
        }
    }

    public Pair<Boolean, Type> traverseTree(JmmNode jmmNode){

        if (jmmNode.toString().equals("MethodDeclaration")){
            this.currentMethodVariables = this.table.getLocalVariables(jmmNode.getChildren().get(1).get("value"));
            this.currentMethodParameters = this.table.getParameters(jmmNode.getChildren().get(1).get("value"));
        }
        if (jmmNode.toString().contains("MultiplicativeOp")){
            return checkOperation(jmmNode, "multiplicative");
        }
        if (jmmNode.toString().contains("AdditiveOp")){
            return checkOperation(jmmNode, "additive");
        }
        if (jmmNode.toString().contains("LogicalOp")){
            return checkOperation(jmmNode, "logical");
        }
        if (jmmNode.toString().equals("AssignmentOp")){
            return checkAssignment(jmmNode);
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
        if (jmmNode.toString().equals("BinaryOp")){
            return checkArrayAccess(jmmNode);
        }
        if (jmmNode.toString().equals("MethodCall")){
            return checkMethodCall(jmmNode);
        }
        if (jmmNode.toString().equals("Condition")){
            checkCondition(jmmNode);
        }
        if (jmmNode.toString().contains("Integer")){
            return new Pair<>(true, new Type("int", false));
        }
        if (jmmNode.toString().contains("Boolean")){
            return new Pair<>(true, new Type("boolean", false));
        }
        if (jmmNode.toString().contains("ThisExpr")){
            return new Pair<>(true, new Type(this.table.getClassName(), false));
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

        //System.out.println(jmmParserResult.getRootNode().toTree());

        traverseTree(jmmParserResult.getRootNode());

        return new JmmSemanticsResult(jmmParserResult, table, this.reports);
    }
}
