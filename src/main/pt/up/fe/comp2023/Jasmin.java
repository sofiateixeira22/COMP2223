package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

import static java.lang.Math.max;

public class Jasmin implements JasminBackend {
    StringBuilder code;
    String className;
    Method method;
    private int n_locals = 0;
    private int stackMax;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        this.code = new StringBuilder();
        ClassUnit ollir = ollirResult.getOllirClass();

        classVisit(ollir);

//        System.out.println(ollirResult.getOllirClass().getSuperClass());
//        System.out.println(ollirResult.getOllirClass().getField(0).getFieldName());
//        System.out.println(ollirResult.getOllirClass().getClassAccessModifier());
//        System.out.println(ollirResult.getOllirClass().getImportedClasseNames());
//        System.out.println(ollirResult.getOllirClass().getMethod(1).getInstr(0).getInstType());
//        System.out.println(ollirResult.getOllirClass().getMethod(0).isConstructMethod());
//        System.out.println(ollirResult.getOllirClass().getPackage());
//        System.out.println("hello");

        System.out.println(this.code.toString());

        return new JasminResult(ollirResult, this.code.toString(), new ArrayList<>());
    }

    public void classVisit(ClassUnit ollir) {
        this.className = ollir.getClassName();
        this.code.append(".class public ").append(ollir.getClassName());

        this.code.append("\n.super ");
        if(ollir.getSuperClass() != null) {
            this.code.append(ollir.getSuperClass());
        } else this.code.append("java/lang/Object");
        this.code.append("\n");

        fieldVisit(ollir);

        methodVisit(ollir);
    }

    public void fieldVisit(ClassUnit ollir) {
        for(var field : ollir.getFields()) {
            this.code.append("\n.field ");
            if(field.isFinalField()) this.code.append("final ");
            this.code.append(field.getFieldName()).append(" ");

            if(field.getFieldType().toString().equals("INT32")) this.code.append("I");
            else if(field.getFieldType().toString().equals("BOOLEAN")) this.code.append("Z");
        }
        this.code.append("\n");
    }

    public String getAccessModifiers(AccessModifiers accessModifier, boolean isConstructMethod) {
        switch (accessModifier) {
            case PUBLIC:
                return "public";
            case PRIVATE:
                return "private";
            case DEFAULT:
                return isConstructMethod ? "public" : "private";
            case PROTECTED:
                return "protected";
            default:
                return "";
        }
    }

    public void methodVisit(ClassUnit ollir) {
        for(var method : ollir.getMethods()) {
            this.method = method;

            this.code.append("\n.method ");
            this.code.append(getAccessModifiers(method.getMethodAccessModifier(), method.isConstructMethod()));

            if(method.isConstructMethod()) this.code.append(" <init>");
            else {
                if(method.isStaticMethod()) this.code.append(" static");
                if(method.isFinalMethod()) this.code.append(" final");

                this.code.append(" ").append(method.getMethodName());
            }

            this.code.append("(");
            paramVisit(method);
            this.code.append(")");

            this.code.append(getType(method.getReturnType().getTypeOfElement()));

            StringBuilder tmp = new StringBuilder();
            String currentLabel = "";
            for(var inst : method.getInstructions()) {
                if(!method.getLabels(inst).isEmpty())
                    if(!currentLabel.equals(method.getLabels(inst).get(0))) {
                        currentLabel = method.getLabels(inst).get(0);
                        for(String label : method.getLabels(inst))
                            tmp.append("\n\t").append(label).append(":");
                    }

                tmp.append(instructionVisit(inst));
            }

            if(!method.isConstructMethod()) {
                this.code.append("\n\t.limit locals ").append(n_locals);
                this.code.append("\n\t.limit stack ").append(max(stackMax, 5)).append("\n");
            }

            this.code.append("\n.end method\n");
        }
    }

    public void paramVisit(Method method) {
        if(method.getMethodName().equals("main")) {
            this.code.append("[Ljava/lang/String;");
        } else {
            for(var param : method.getParams()) {
                if(param.isLiteral()) this.code.append("L");

                switch (param.getType().getTypeOfElement()) {
                    case INT32:
                        this.code.append("I");
                        break;
                    case BOOLEAN:
                        this.code.append("Z");
                        break;
                    case ARRAYREF:
                        this.code.append("[I");
                        break;
                    case OBJECTREF:
                        this.code.append("OBJECTREF");
                        break;
                    case STRING:
                        this.code.append("java/lang/String");
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public String instructionVisit(Instruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        switch (inst.getInstType()) {
            case PUTFIELD:
                jasminCode.append(PutFieldVisit(inst));
                break;
            case BRANCH:
                jasminCode.append(BranchVisit(inst));
                break;
            case GOTO:
                jasminCode.append(GotoVisit((GotoInstruction) inst));
                break;
            case CALL:
                jasminCode.append(CallVisit(inst, false));
                break;
            case RETURN:
                jasminCode.append(ReturnVisit((ReturnInstruction) inst));
                break;
            case ASSIGN:
                jasminCode.append(AssignVisit(inst));
                break;
            default:
                break;
        }

        return  jasminCode.toString();
    }

    public String PutFieldVisit(Instruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        System.out.println(inst);

        return jasminCode.toString();
    }

    public String BranchVisit(Instruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        System.out.println(inst);

        return jasminCode.toString();
    }

    public String GotoVisit(GotoInstruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append("\n\tgoto ").append(inst.getLabel()).append("\n");

        return jasminCode.toString();
    }

    public String CallVisit(Instruction inst, Boolean assign) {
        StringBuilder jasminCode = new StringBuilder();

        System.out.println(inst);

        return jasminCode.toString();
    }

    public String ReturnVisit(ReturnInstruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        Element e1 = inst.getOperand();
        if(e1 != null) {
            if(!e1.isLiteral()) {

            }
        }

        return jasminCode.toString();
    }

    public String AssignVisit(Instruction inst) {
        StringBuilder jasminCode = new StringBuilder();

        System.out.println(inst);

        return jasminCode.toString();
    }

    public String getType(ElementType type) {
        switch (type) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case ARRAYREF:
                return "[I";
            case OBJECTREF:
                return "L" + this.className + ";";
            case VOID:
                return "V";
            default:
                return "";
        }
    }
}
