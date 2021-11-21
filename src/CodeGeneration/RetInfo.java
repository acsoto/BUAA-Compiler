package CodeGeneration;

import java.util.HashMap;

public class RetInfo {
    private Integer eip;
    private HashMap<String, Var> varTable;
    private Integer stackPtr;
    private Integer paraNum;
    private Integer callArgsNum;
    private Integer nowArgsNum;

    public RetInfo(Integer eip, HashMap<String, Var> varTable, Integer stackPtr, Integer paraNum, Integer callArgsNum, Integer nowArgsNum) {
        this.eip = eip;
        this.varTable = varTable;
        this.stackPtr = stackPtr;
        this.paraNum = paraNum;
        this.callArgsNum = callArgsNum;
        this.nowArgsNum = nowArgsNum;
    }

    public Integer getEip() {
        return eip;
    }

    public HashMap<String, Var> getVarTable() {
        return varTable;
    }

    public Integer getStackPtr() {
        return stackPtr;
    }

    public Integer getParaNum() {
        return paraNum;
    }

    public Integer getCallArgsNum() {
        return callArgsNum;
    }

    public Integer getNowArgsNum() {
        return nowArgsNum;
    }
}
