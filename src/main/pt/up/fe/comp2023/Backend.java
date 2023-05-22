package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class Backend implements JasminBackend {
    StringBuilder code;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        this.code = new StringBuilder();
        ClassUnit ollir = ollirResult.getOllirClass();

        System.out.println(ollirResult.getOllirClass());
        System.out.println(ollir.getClassName());
        System.out.println(ollirResult.getOllirClass().getSuperClass());
        System.out.println(ollirResult.getOllirClass().getField(0).getFieldName());
        System.out.println(ollirResult.getOllirClass().getClassAccessModifier());
        System.out.println(ollirResult.getOllirClass().getImportedClasseNames());
        System.out.println(ollirResult.getOllirClass().getMethod(1).getInstr(0).getInstType());
        System.out.println(ollirResult.getOllirClass().getMethod(0).isConstructMethod());
        System.out.println(ollirResult.getOllirClass().getPackage());

        return new JasminResult(ollirResult, this.code.toString(), new ArrayList<>());
    }
}
