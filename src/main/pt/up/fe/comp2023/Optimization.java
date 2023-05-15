package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class Optimization implements JmmOptimization {
    private StringBuilder code;
    private SymbolTable symbolTable;
    private int indexFirstLevel = 0;
    private int indexSecondLevel = 0;
    private int indexTemp = 0;
    private int indexIf = 0;
    private int indexWhile = 0;
    private JmmSemanticsResult jmmSemanticsResult;
    private StringBuilder operationBuilder = new StringBuilder();
    private List<String> imports = new ArrayList<>();
    private List<Symbol> localVariables = new ArrayList<>();
    private List<Symbol> methodParam = new ArrayList<>();
    private List<Symbol> fields = new ArrayList<>();

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        this.code = new StringBuilder();
        this.jmmSemanticsResult = jmmSemanticsResult;
        this.symbolTable = jmmSemanticsResult.getSymbolTable();

        importVisit();
        classDeclarationVisit();

        System.out.println("\n** Code **\n\n" + this.code);

        return new OllirResult(jmmSemanticsResult, code.toString(), new ArrayList<>());
    }

    public void importVisit() {
        var root = jmmSemanticsResult.getRootNode();

        int numImports = 0;
        for(var child : root.getChildren())
            if(child.getKind().equals("ImportDeclaration")) numImports+=1;

        for(var i = 0; i < numImports; i++) {
            indexFirstLevel+=1;
            this.code.append("import ");
            if(root.getJmmChild(i).getNumChildren() > 1) {
                var size = root.getJmmChild(i).getNumChildren();
                for (int j = 0; j < size-1; j++) {
                    this.code.append(root.getJmmChild(i).getJmmChild(j).get("value")).append(".");
                }
                this.code.append(root.getJmmChild(i).getJmmChild(size-1).get("value")).append(";\n");
                imports.add(root.getJmmChild(i).getJmmChild(size-1).get("value"));
            } else {
                this.code.append(root.getJmmChild(i).getJmmChild(0).get("value")).append(";\n");
                imports.add(root.getJmmChild(i).getJmmChild(0).get("value"));
            }
        }

        this.code.append("\n");
    }

    public void classDeclarationVisit() {
        indexFirstLevel +=1;

        this.code.append(this.symbolTable.getClassName()).append(" ");

        indexSecondLevel+=1;

        var superClass = this.symbolTable.getSuper();
        if(superClass != null) {
            indexSecondLevel+=1;
            this.code.append("extends ").append(superClass).append(" ");
        }
        this.code.append("{\n");

        varDeclarationVisit(); //global variables

        this.code.append("\n\t.construct ").append(this.symbolTable.getClassName()).append("().V {\n");
        this.code.append("\t\tinvokespecial(this, \"<init>\").V;\n");
        this.code.append("\t}\n\n");

        methodVisit();

        this.code.append("}");
    }

    public void varDeclarationVisit() {
        this.code.append("\n");

        this.fields = this.symbolTable.getFields();

        for(var fieldString: this.symbolTable.getFields()) {
            indexSecondLevel+=1;
            this.code.append("\t.field public ").append(fieldString.getName());

            var type = fieldString.getType();
            if(type.isArray()) this.code.append(".array");
            if(type.getName().equals("int[]") || type.getName().equals("int")) this.code.append(".i32");
            if(type.getName().equals("boolean[]") || type.getName().equals("boolean")) this.code.append(".bool");
            if(type.getName().equals(("String[]")) || type.getName().equals("String")) this.code.append(".String");

            this.code.append(";\n");
        }
    }

    public void methodVisit() {
        for(var methodString: this.symbolTable.getMethods()) {
            indexSecondLevel+=1;

            if(methodString.equals("main")) this.code.append("\t.method public static ").append(methodString).append("(");
            else this.code.append("\t.method public ").append(methodString).append("(");

            methodParametersVisit(methodString);

            this.code.append(")");

            var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);

            var returnType = method.getJmmChild(0).get("t");

            if(returnType.equals("int[]") || returnType.equals("boolean[]")) this.code.append(".array");

            if(returnType.equals("int") || returnType.equals("int[]")) this.code.append(".i32 ");
            else if (returnType.equals("boolean") || returnType.equals("boolean[]")) this.code.append(".bool ");
            else if(returnType.equals("void")) this.code.append(".V ");
            else this.code.append(".").append(returnType).append(" ");

            this.code.append("{\n");

            this.localVariables = getLocalVariables(method);

            this.indexTemp = 0;
            this.indexIf = 0;
            this.indexWhile = 0;

            for(JmmNode jmmNode : method.getChildren()) {
                if(jmmNode.getKind().equals("Statement"))
                    statementVisit(jmmNode);
            }

            if(!returnType.equals("void") && method.getJmmChild(method.getNumChildren()-1).getJmmChild(0).getJmmChild(0).getKind().equals("AdditiveOp")) {
                this.code.append(getReturnVar(returnType));
            } else {
                this.code.append("\n\t\tret");
                if(!returnType.equals("void")) {
                    var returnValue = getReturnVar(returnType);
                    if(returnType.equals("int[]") || returnType.equals("boolean[]")) this.code.append(".array");
                    if(returnType.equals("int") || returnType.equals("int[]")) this.code.append(".i32 ").append(returnValue).append(";\n");
                    if(returnType.equals("boolean") || returnType.equals("boolean[]")) this.code.append(".bool ").append(returnValue).append(";\n");
                    else this.code.append(".").append(returnType).append(" ").append(returnValue).append(";\n");;
                } else
                    this.code.append(".V;\n");
            }

            this.code.append("\t}\n\n");
        }
    }

    public void methodParametersVisit(String methodString) {
        this.methodParam = this.symbolTable.getParameters(methodString);

        for(var parameterString: this.symbolTable.getParameters(methodString)) {
            this.code.append(parameterString.getName());

            var type = parameterString.getType();
            if(type.isArray()) this.code.append(".array");
            if(type.getName().equals("int[]") || type.getName().equals("int")) this.code.append(".i32");
            else if(type.getName().equals("boolean[]") || type.getName().equals("boolean")) this.code.append(".bool");
            else if(type.getName().equals(("String[]")) || type.getName().equals("String")) this.code.append(".String");
            else this.code.append(".").append(type.getName());

            var size = this.symbolTable.getParameters(methodString).size();
            if(size > 1 && parameterString != this.symbolTable.getParameters(methodString).get(size-1))
                this.code.append(", ");
        }
    }

    public void statementVisit(JmmNode jmmNode) {
        if(jmmNode.getJmmChild(0).getKind().equals("AssignmentOp"))
            assignmentVisit(jmmNode.getJmmChild(0));

        if(jmmNode.getJmmChild(0).getKind().equals("MethodCall"))
            methodCallVisit(jmmNode.getJmmChild(0), null);

        if(jmmNode.hasAttribute("stmt")) {
            if(jmmNode.get("stmt").equals("if")) ifVisit(jmmNode);
            if(jmmNode.get("stmt").equals("while")) whileVisit(jmmNode);
        }
    }

    public void assignmentVisit(JmmNode jmmNode) {
        var dest_node = jmmNode.getJmmChild(0);
        String dest = "";
        String array_dest = "";
        String array_og = "";
        String operation = "";
        String tmp = "";
        boolean length = false;

        if(dest_node.hasAttribute("value")) dest = dest_node.get("value");

        if(dest_node.getKind().equals("ArrayAccess")) {
            this.indexTemp += 1;
            dest = dest_node.getJmmChild(0).get("value");
            array_dest = arrayVisit(dest_node, null);
        }

        var type = getType(dest);

        if(jmmNode.getJmmChild(1).getKind().equals("ArrayAccess")) {
            this.indexTemp += 1;
            array_og = arrayVisit(jmmNode.getJmmChild(1), null);
        }

        if(jmmNode.getJmmChild(1).getKind().equals("ArrayNew")) {
            this.indexTemp+=1;
            arrayVisit(jmmNode.getJmmChild(1), dest);
        }

        if(jmmNode.getJmmChild(1).getKind().equals("AdditiveOp") || jmmNode.getJmmChild(1).getKind().equals("MultiplicativeOp")) {
            StringBuilder returnType = new StringBuilder();
            if(type.equals("int")) returnType.append("int");
            operation = operationVisit(jmmNode.getJmmChild(1), returnType.toString(), new StringBuilder());
        }

        if(jmmNode.getJmmChild(1).getKind().equals("Length")) {
            this.indexTemp += 1;
            tmp = arrayVisit(jmmNode.getJmmChild(1), null);
            length = true;
        }

        if(!jmmNode.getJmmChild(1).getKind().equals("ArrayNew")) {
            this.code.append("\t\t").append(dest);

            if (!array_dest.equals("")) {
                this.code.append("[").append(array_dest).append(".i32]");
            }

            if (type.equals("int") || type.equals("int[]") || length) this.code.append(".i32 :=.i32 ");
            else if (type.equals("boolean") || type.equals("boolean[]")) this.code.append(".bool :=.bool ");
            else this.code.append(".").append(type).append(" :=.").append(type).append(" ");
        }

        if(jmmNode.getJmmChild(1).hasAttribute("value")) {
            var value = jmmNode.getJmmChild(1).get("value");

            if(jmmNode.getJmmChild(1).getKind().equals("ClassNew"))
                this.code.append("new(").append(value).append(")");
            else this.code.append(value);

            if(type.equals("int") || type.equals("int[]")) this.code.append(".i32;\n");
            else if(type.equals("boolean") || type.equals("boolean[]")) this.code.append(".bool;\n");
            else this.code.append(".").append(type).append(";\n");

            if(jmmNode.getJmmChild(1).getKind().equals("ClassNew"))
                this.code.append("\t\tinvokespecial(").append(dest).append(".").append(type).append(", \"<init>\").V;\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("AdditiveOp") || jmmNode.getJmmChild(1).getKind().equals("MultiplicativeOp")) {
            this.code.append(operation).append(".i32;\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("LogicalOp")) {

            var node = jmmNode.getJmmChild(1);
            var op = node.get("op");

            if(node.getJmmChild(0).getKind().equals("Boolean"))
                this.code.append(node.getJmmChild(0).get("value")).append(".bool ");
            this.code.append(op).append(".bool ");
            if(node.getJmmChild(1).getKind().equals("Boolean"))
                this.code.append(node.getJmmChild(1).get("value")).append(".bool");
            this.code.append(";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("UnaryPreOp")) {
            var node = jmmNode.getJmmChild(1);
            var op = node.get("op");

            this.code.append(op).append(".bool ");
            this.code.append(node.getJmmChild(0).get("value"));
            if(node.getJmmChild(0).getKind().equals("Boolean"))
                this.code.append(".bool");
            var var_type = getType(node.getJmmChild(0).get("value"));
            if(type.equals("boolean")) this.code.append(".bool");
            this.code.append(";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("RelationalOp")) {
            var node = jmmNode.getJmmChild(1);
            var op = node.get("op");

            if(node.getJmmChild(0).getKind().equals("Integer"))
                this.code.append(node.getJmmChild(0).get("value")).append(".i32 ");
            this.code.append(op).append(".bool ");
            if(node.getJmmChild(1).getKind().equals("Integer"))
                this.code.append(node.getJmmChild(1).get("value")).append(".i32");
            this.code.append(";\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("ArrayAccess")) {
            this.code.append(jmmNode.getJmmChild(1).getJmmChild(0).get("value")).append("[");
            this.code.append(array_og).append(".i32]");

            var array_type = getType(jmmNode.getJmmChild(1).getJmmChild(0).get("value"));
            if(array_type.equals("int[]")) this.code.append(".i32;\n");
            else if(array_type.equals("boolean[]")) this.code.append(".bool;\n");
        }

        if(jmmNode.getJmmChild(1).getKind().equals("MethodCall")) {
            var code_length = this.code.length();
            methodCallVisit(jmmNode.getJmmChild(1), type);
            this.code.delete(code_length, code_length+2);
        }

        if(jmmNode.getJmmChild(1).getKind().equals("Length")) {
            this.code.append(tmp).append(".i32;\n");
        }
    }

    public String getType(String var) {
        String type = "";
        if(this.localVariables != null)
            for(var localVar : this.localVariables)
                if(localVar.getName().equals(var)) type = localVar.getType().getName();

        if(this.methodParam != null)
            for(var param : this.methodParam)
                if(param.getName().equals(var)) type = param.getType().getName();

        if(this.fields != null)
            for(var field : this.fields)
                if(field.getName().equals(var)) type = field.getType().getName();

        return type;
    }

    public List<Symbol> getLocalVariables(JmmNode jmmNode) {
        List<Symbol> localVariables = new ArrayList<>();
        var size = jmmNode.getNumChildren();

        for(int i = 0; i < size; i++) {
            if(jmmNode.getJmmChild(i).getKind().equals("VarDeclaration")) {
                boolean isArray = false;

                if(jmmNode.getJmmChild(i).getJmmChild(0).get("t").equals("int[]")
                || jmmNode.getJmmChild(i).getJmmChild(0).get("t").equals("boolean[]")) isArray = true;

                Type type = new Type(jmmNode.getJmmChild(i).getJmmChild(0).get("t"), isArray);
                Symbol localVar = new Symbol(type, jmmNode.getJmmChild(i).getJmmChild(1).get("value"));

                localVariables.add(localVar);
            }
        }

        return localVariables;
    }

    public void methodCallVisit(JmmNode jmmNode, String dest_type) {
        var method = jmmNode.getJmmChild(1).get("value");
        var dest = jmmNode.getJmmChild(0).get("value");

        StringBuilder methodcall = new StringBuilder();

        var isVar = getType(dest);
        if(isVar.equals("") && this.imports.contains(dest)) {
            methodcall.append("\t\tinvokestatic(").append(dest);
        } else {
            methodcall.append("\t\tinvokevirtual(").append(dest);

            if(jmmNode.getJmmChild(0).getKind().equals("Integer"))
                methodcall.append(".i32");
            if(jmmNode.getJmmChild(0).getKind().equals("Boolean"))
                methodcall.append(".bool");
            else {
                var type = getType(dest);
                if(type.equals("int")) methodcall.append(".i32");
                else if(type.equals("boolean")) methodcall.append(".bool");
                else methodcall.append(".").append(type);
            }
        }
        if(jmmNode.getNumChildren() > 2) {
            methodcall.append(", \"").append(method).append("\", ");
            if(jmmNode.getNumChildren() > 3) {
                for(int i = 2; i < jmmNode.getNumChildren()-1; i++) {
                    methodcall.append(jmmNode.getJmmChild(i).get("value")).append(".");
                    if(jmmNode.getJmmChild(i).getKind().equals("Integer"))
                        methodcall.append("i32");
                    else if(jmmNode.getJmmChild(i).getKind().equals("Boolean"))
                        methodcall.append("bool");
                    else {
                        var type = getType(jmmNode.getJmmChild(i).get("value"));
                        if(type.equals("int")) methodcall.append("i32");
                        else if(type.equals("boolean")) methodcall.append("bool");
                        else methodcall.append(type);
                    }
                    methodcall.append(", ");
                }
            }
            var node = jmmNode.getJmmChild(jmmNode.getNumChildren()-1);
            if(node.hasAttribute("value")) {
                methodcall.append(node.get("value"));

                if(node.getKind().equals("Integer")) methodcall.append(".i32");
                else if(node.getKind().equals("Boolean")) methodcall.append(".bool");
                else {
                    var type = getType(node.get("value"));
                    if(type.equals("int[]") || type.equals("boolean[]")) methodcall.append(".array");
                    if(type.equals("int") || type.equals("int[]")) methodcall.append(".i32");
                    else if(type.equals("boolean") || type.equals("boolean[]")) methodcall.append(".bool");
                    else methodcall.append(type);
                }
            } else if(node.getKind().equals("Length")) {
                this.indexTemp += 1;
                methodcall.append(arrayVisit(node, node.getJmmChild(0).get("value"))).append(".i32");
            } else if(node.getKind().equals("ArrayAccess")) {
                this.indexTemp += 1;
                var type = getType(node.getJmmChild(0).get("value"));

                var tmp = arrayVisit(node, null);

                this.indexTemp += 1;

                this.code.append("\t\ttemp").append(this.indexTemp);

                if(type.equals("int[]")) this.code.append(".i32 :=.i32 ");
                else if(type.equals("boolean[]")) this.code.append(".bool :=.bool ");

                this.code.append(node.getJmmChild(0).get("value")).append("[temp").append(this.indexTemp-1).append(".i32]");

                if(type.equals("int[]")) this.code.append(".i32;\n");
                else if(type.equals("boolean[]")) this.code.append(".bool;\n");

                methodcall.append("temp").append(this.indexTemp);

                if(type.equals("int[]")) methodcall.append(".i32");
                else if(type.equals("boolean[]")) methodcall.append(".bool");
            }

            if(dest_type != null && (dest_type.equals("int") || dest_type.equals("int[]"))) methodcall.append(").i32;\n");
            else if(dest_type != null && (dest_type.equals("boolean") || dest_type.equals("boolean[]"))) methodcall.append(").bool;\n");
            else if(dest_type != null) methodcall.append(").").append(dest_type).append(";\n");
            else if(dest_type == null) methodcall.append(").V;\n");
        } else {
            methodcall.append(", \"").append(method).append("\").V;\n");
        }
        this.code.append(methodcall);
    }

    public String operationVisit(JmmNode jmmNode, String type, StringBuilder tmp) {
        var op = jmmNode.get("op");
        this.indexTemp += 1;

        var left = jmmNode.getJmmChild(0);
        if(left.hasAttribute("value")) {
            for(var localVar : this.localVariables) {
                if(localVar.getName().equals(left.get("value")))
                    if(localVar.getType().getName().equals("int")) this.operationBuilder.append(left.get("value")).append(".i32");
            }
            if(left.getKind().equals("Integer")) this.operationBuilder.append(left.get("value")).append(".i32");
        } else if(left.getKind().equals("ArrayAccess")) {
            var array_type = getType(left.getJmmChild(0).get("value"));
            this.indexTemp += 1;
            var array = arrayVisit(left, null);
            this.operationBuilder.append(left.getJmmChild(0).get("value")).append("[").append(array).append(".i32]");
            if (array_type.equals("int[]")) this.operationBuilder.append(".i32");
            else if (array_type.equals("boolean[]")) this.operationBuilder.append(".bool");
        } else if(left.getKind().equals("Length")) {
            arrayVisit(left, null);
            this.operationBuilder.append("temp").append(this.indexTemp).append(".i32");
            this.indexTemp += 1;
        } else {
            operationVisit(left, type, tmp);
            this.operationBuilder.append(tmp).append(".i32");
        }

        this.operationBuilder.append(" ").append(op).append(".i32 ");

        var right = jmmNode.getJmmChild(1);
        if(right.hasAttribute("value")) {
            for(var localVar : this.localVariables) {
                if(localVar.getName().equals(right.get("value")))
                    if (localVar.getType().getName().equals("int")) this.operationBuilder.append(right.get("value")).append(".i32");
            }
            if(right.getKind().equals("Integer")) this.operationBuilder.append(right.get("value")).append(".i32");
        } else if(right.getKind().equals("ArrayAccess")) {
            var array_type = getType(right.getJmmChild(0).get("value"));
            this.indexTemp += 1;
            var array = arrayVisit(right, null);
            this.operationBuilder.append(right.getJmmChild(0).get("value")).append("[").append(array).append(".i32]");
            if (array_type.equals("int[]")) this.operationBuilder.append(".i32");
            else if (array_type.equals("boolean[]")) this.operationBuilder.append(".bool");
        } else if(right.getKind().equals("Length")) {
            arrayVisit(right, null);
            this.operationBuilder.append("temp").append(this.indexTemp).append(".i32");
            this.indexTemp += 1;
        } else {
            operationVisit(right, type, tmp);
            this.operationBuilder.append(tmp).append(".i32");
        }

        this.code.append("\t\ttemp").append(this.indexTemp).append(".i32 :=.i32 ").append(this.operationBuilder.toString()).append(";\n");

        if(!tmp.isEmpty()) tmp.delete(0, tmp.length());

        tmp.append("temp").append(this.indexTemp);

        this.operationBuilder.delete(0, this.operationBuilder.length());

        return tmp.toString();
    }

    public void ifVisit(JmmNode jmmNode) {
        this.indexIf += 1;

        this.code.append("\t\tif (");

        var condition = jmmNode.getJmmChild(0);
        if(condition.getJmmChild(0).getKind().equals("RelationalOp")) {
            var op = condition.getJmmChild(0).get("op");
            var var1 = condition.getJmmChild(0).getJmmChild(0).get("value");
            var var2 = condition.getJmmChild(0).getJmmChild(1).get("value");

            this.code.append(var1).append(".");

            if(condition.getJmmChild(0).getJmmChild(0).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var1_type = getType(var1);
                if(var1_type.equals("int")) this.code.append("i32 ");
                else if(var1_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var1_type);
            }

            this.code.append(op).append(".bool ");

            this.code.append(var2).append(".");

            if(condition.getJmmChild(0).getJmmChild(1).getKind().equals("Integer"))
                this.code.append("i32");
            else {
                var var2_type = getType(var2);
                if(var2_type.equals("int")) this.code.append("i32 ");
                else if(var2_type.equals("boolean")) this.code.append("bool ");
                else this.code.append(var2_type);
            }
        } else if(condition.getJmmChild(0).getKind().equals("IdentifierExpr")) {
            var value = condition.getJmmChild(0).get("value");
            var type = getType(value);
            this.code.append(value).append(".");
            if(type.equals("int")) this.code.append("i32");
            else if(type.equals("boolean")) this.code.append("bool");
            else this.code.append(type);
        } else if (condition.getJmmChild(0).getKind().equals("Boolean")) {
            var value = condition.getJmmChild(0).get("value");
            this.code.append(value).append(".bool");
        }

        this.code.append(") goto ifbody_").append(this.indexIf).append(";\n");

        var else_stmt = jmmNode.getJmmChild(2);
        if(else_stmt.getJmmChild(0).getKind().equals("Statement")) {
            if(else_stmt.getJmmChild(0).hasAttribute("stmt") && else_stmt.getJmmChild(0).get("stmt").equals("if")) {
                ifVisit(else_stmt.getJmmChild(0));
                this.indexIf -=1;
            }
            else statementVisit(else_stmt.getJmmChild(0));
        } else if(else_stmt.getJmmChild(0).getKind().equals("MethodCall"))
            methodCallVisit(else_stmt.getJmmChild(0), null);

        this.code.append("\t\tgoto endif_").append(this.indexIf).append(";\n");
        this.code.append("\t\tifbody_").append(this.indexIf).append(":\n");

        var if_stmt = jmmNode.getJmmChild(1);
        if(if_stmt.getJmmChild(0).getKind().equals("Statement")) {
            if(if_stmt.getJmmChild(0).hasAttribute("stmt") && if_stmt.getJmmChild(0).get("stmt").equals("if")) {
                ifVisit(if_stmt.getJmmChild(0));
                this.indexIf -=1;
            }
            else statementVisit(if_stmt.getJmmChild(0));
        } else if(if_stmt.getJmmChild(0).getKind().equals("MethodCall"))
            methodCallVisit(if_stmt.getJmmChild(0), null);

        this.code.append("\t\tendif_").append(this.indexIf).append(":\n");
    }

    public void whileVisit(JmmNode jmmNode) {
        this.indexWhile += 1;
        var index = this.indexWhile;

        StringBuilder if_statement = new StringBuilder();

        if_statement.append("\t\tif (");

        var condition = jmmNode.getJmmChild(0);

        if(condition.getJmmChild(0).getKind().equals("RelationalOp")) {
            var op = condition.getJmmChild(0).get("op");
            var var1 = condition.getJmmChild(0).getJmmChild(0).get("value");
            var var2 = condition.getJmmChild(0).getJmmChild(1).get("value");

            if_statement.append(var1).append(".");

            if(condition.getJmmChild(0).getJmmChild(0).getKind().equals("Integer"))
                if_statement.append("i32");
            else {
                var var1_type = getType(var1);
                if(var1_type.equals("int")) if_statement.append("i32 ");
                else if(var1_type.equals("boolean")) if_statement.append("bool ");
                else if_statement.append(var1_type);
            }

            if_statement.append(op).append(".bool ");

            if_statement.append(var2).append(".");

            if(condition.getJmmChild(0).getJmmChild(1).getKind().equals("Integer"))
               if_statement.append("i32");
            else {
                var var2_type = getType(var2);
                if(var2_type.equals("int")) if_statement.append("i32 ");
                else if(var2_type.equals("boolean")) if_statement.append("bool ");
                else if_statement.append(var2_type);
            }
        } else if(condition.getJmmChild(0).getKind().equals("IdentifierExpr")) {
            var value = condition.getJmmChild(0).get("value");
            var type = getType(value);
            this.code.append(value).append(".");
            if(type.equals("int")) this.code.append("i32");
            else if(type.equals("boolean")) this.code.append("bool");
            else this.code.append(type);
        } else if (condition.getJmmChild(0).getKind().equals("Boolean")) {
            var value = condition.getJmmChild(0).get("value");
            this.code.append(value).append(".bool");
        }

        if_statement.append(") goto whilebody_").append(index).append(";\n");
        this.code.append(if_statement);
        this.code.append("\t\tgoto endwhile_").append(index).append(";\n");

        this.code.append("\t\twhilebody_").append(index).append(":\n");

        var stmts_size = jmmNode.getJmmChild(1).getNumChildren();
        for(int i = 0; i < stmts_size; i++) {
            if(jmmNode.getJmmChild(1).getJmmChild(i).getKind().equals("Statement"))
                statementVisit(jmmNode.getJmmChild(1).getJmmChild(i));
        }

        this.code.append(if_statement);
        this.code.append("\t\tendwhile_").append(index).append(":");

    }

    public String arrayVisit(JmmNode jmmNode, String dest) {
        if(jmmNode.getKind().equals("ArrayNew")) {

            var temp = "temp" + this.indexTemp;
            String temp_type;

            if(jmmNode.getJmmChild(1).getKind().equals("Integer")) {
                this.code.append("\t\ttemp").append(this.indexTemp).append(".i32 :=.i32 ").append(jmmNode.getJmmChild(1).get("value")).append(".i32;\n");
                temp_type = "int";
            } else {
                var type = getType(jmmNode.getJmmChild(1).get("value"));
                temp_type = type;
                if(type.equals("int"))
                    this.code.append("\t\ttemp").append(this.indexTemp).append(".i32 :=.i32 ").append(jmmNode.getJmmChild(1).get("value")).append(".i32;\n");
                else if(type.equals("boolean"))
                    this.code.append("\t\ttemp").append(this.indexTemp).append(".bool :=.bool ").append(jmmNode.getJmmChild(1).get("value")).append(".bool;\n");
                else
                    this.code.append("\t\ttemp").append(this.indexTemp).append(".").append(type).append(" :=.").append(type).append(" ").append(jmmNode.getJmmChild(1).get("value")).append(".").append(type).append(";\n");
            }

            var type = getType(dest);
            if(type.equals("int[]") || type.equals("int")) this.code.append("\t\t").append(dest).append(".array.i32 :=.array.i32 ");
            else if(type.equals("boolean[]") || type.equals("boolean")) this.code.append("\t\t").append(dest).append(".array.bool :=.array.bool ");
            else this.code.append("\t\t").append(dest).append(".array.").append(type).append(" :=.array.").append(type).append(" ");

            this.code.append("new(array, ").append(temp).append(".");

            if(temp_type.equals("int")) this.code.append("i32");
            else if(temp_type.equals("boolean")) this.code.append("bool");
            else this.code.append(temp_type);

            this.code.append(").array");

            if(jmmNode.getJmmChild(0).get("t").equals("int"))
                this.code.append(".i32;\n");
            else if(jmmNode.getJmmChild(0).get("t").equals("boolean"))
                this.code.append(".bool;\n");
            else this.code.append(".").append(jmmNode.getJmmChild(0).get("t")).append(";\n");

        } else if(jmmNode.getKind().equals("Length")) {
            var temp = "temp" + this.indexTemp;

            this.code.append("\t\ttemp").append(this.indexTemp).append(".i32 :=.i32 ");
            this.code.append("arraylength(").append(jmmNode.getJmmChild(0).get("value")).append(".array");

            var type = getType(jmmNode.getJmmChild(0).get("value"));
            if(type.equals("int") || type.equals("int[]")) this.code.append(".i32");
            else if(type.equals("boolean") || type.equals("boolean[]")) this.code.append(".bool");
            else this.code.append(".").append(type);

            this.code.append(").i32.i32;\n");

            return temp;
        }  if(jmmNode.getKind().equals("ArrayAccess")) {
            var temp = "temp" + this.indexTemp;

            if(jmmNode.getJmmChild(1).hasAttribute("value")) {
                this.code.append("\t\ttemp").append(this.indexTemp);
                var temp_type = getType(jmmNode.getJmmChild(1).get("value"));
                if(jmmNode.getJmmChild(1).getKind().equals("Integer") || temp_type.equals("int"))
                    this.code.append(".i32 :=.i32 ").append(jmmNode.getJmmChild(1).get("value")).append(".i32;\n");
                else if(jmmNode.getJmmChild(1).getKind().equals("Boolean") || temp_type.equals("boolean"))
                    this.code.append(".bool :=.bool ").append(jmmNode.getJmmChild(1).get("value")).append(".bool;\n");
                else this.code.append(".").append(temp_type).append(" :=.").append(temp_type).append(" ").append(jmmNode.getJmmChild(1).get("value")).append(".").append(temp_type).append(";\n");
            } else if(jmmNode.getJmmChild(1).getKind().equals("MethodCall")) {
                this.indexTemp += 1;
                this.code.append("\t\ttemp").append(this.indexTemp);
                this.code.append(".i32 :=.i32 ");
                var code_length = this.code.length();
                methodCallVisit(jmmNode.getJmmChild(1), null);
                this.code.delete(code_length, code_length+2);
                this.code.delete(this.code.length()-4, this.code.length());
                this.code.append(".i32.i32;\n");
            } else if(jmmNode.getJmmChild(1).getKind().equals("AdditiveOp") || jmmNode.getJmmChild(1).getKind().equals("MultiplicativeOp")) {
                temp = operationVisit(jmmNode.getJmmChild(1), "int", new StringBuilder());
            } else if(jmmNode.getJmmChild(1).getKind().equals("ArrayAccess")) {
                var tmp = arrayVisit(jmmNode.getJmmChild(1), null);
                this.indexTemp += 1;
                this.code.append("\t\ttemp").append(this.indexTemp);

                var type = getType(jmmNode.getJmmChild(1).getJmmChild(0).get("value"));
                if(type.equals("int[]")) this.code.append(".i32 :=.i32 ");
                else if(type.equals("boolean[]")) this.code.append(".bool :=.bool ");

                this.code.append(jmmNode.getJmmChild(1).getJmmChild(0).get("value")).append("[").append(tmp).append(".i32]");
                if(type.equals("int[]")) this.code.append(".i32;\n");
                else if(type.equals("boolean[]")) this.code.append(".bool;\n");

                temp = "temp" + this.indexTemp;
            }


            return temp;

        }

        return "";
    }

    public String getReturnVar(String returnType) {
        var method = this.jmmSemanticsResult.getRootNode().getJmmChild(this.indexFirstLevel-1).getJmmChild(this.indexSecondLevel-1);
        StringBuilder returnVar = new StringBuilder();
        var returnValue = method.getJmmChild(method.getNumChildren()-1).getJmmChild(0).getJmmChild(0);

        if(returnValue.hasAttribute("value")) {
            returnVar.append(returnValue.get("value"));
            var type = getType(returnValue.get("value"));

            if(type.equals("int[]") || type.equals("boolean[]")) returnVar.append(".array");
            if(returnType.equals("int") || returnType.equals("int[]")) {
                returnVar.append(".i32");
            } else if(returnType.equals("boolean") || returnType.equals("boolean[]")) {
                returnVar.append(".bool");
            } else returnVar.append(".").append(returnType);
        }
        if(returnValue.getKind().equals("AdditiveOp")) {
            var operation = operationVisit(returnValue, returnType, new StringBuilder());
            returnVar.append("\t\tret.i32 ").append(operation).append(".i32;\n");
        }
        return returnVar.toString();
    }
}
