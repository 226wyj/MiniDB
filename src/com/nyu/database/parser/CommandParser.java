package com.nyu.database.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandParser {
    //----------------
    // Attributes
    //----------------

    private static final String[] ALL_COMMANDS = {
            "inputfromfile", "outputtofile", "select", "project",
            "sum", "avg", "sumgroup", "avggroup", "join", "sort",
            "movavg", "movsum", "btree", "hash", "concat", "showDB"
    };

    private static final String[] ALL_OPERATORS = {">", "<", "=", "!=", ">=", "<="};


    private String tableName;
    private String commandName;
    private List<String> arguments;
    private OperationExpression operationExpression;


    //----------------
    // Constructor(s)
    //----------------

    public CommandParser() {
        tableName = null;
        commandName = null;
        arguments = new ArrayList<>();
        operationExpression = null;
    }


    //----------------
    // Accessors
    //----------------

    public static String[] getAllCommands() {
        return ALL_COMMANDS;
    }

    public static String[] getAllOperators() {
        return ALL_OPERATORS;
    }

    public void setTableName(String tb) {
        this.tableName = tb;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setCommandName(String cn) {
        this.commandName = cn;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public OperationExpression getOperationExpression() {
        return operationExpression;
    }

    public void setOperationExpression(OperationExpression operationExpression) {
        this.operationExpression = operationExpression;
    }

    //----------------
    // Other Methods.
    //----------------

    public void resetArguments() {
        this.arguments = new ArrayList<>();
    }

    public boolean isValidCommand(String command) {
        return Arrays.asList(getAllCommands()).contains(command);
    }

    public boolean isManageCommand(String command) {
        if (!command.contains(":=") && !command.contains("(") && !command.contains(")")) {
            return isValidCommand(command);
        }
        return false;
    }

    private void parseJoinCommand(String strArguments) {
        String[] argumentsSplit = strArguments.split(",");
        String targetTable1 = argumentsSplit[0];
        String targetTable2 = argumentsSplit[1];
        String condition = argumentsSplit[2 ];
        getArguments().add(targetTable1);
        getArguments().add(targetTable2);

        parseOperator(condition);
        parseOperands(condition, getOperationExpression());
    }

    /**
     * Parse "select" command separately.
     * @param strArguments a string that contains all the arguments and the CONDITION.
     */
    private void parseSelectCommand(String strArguments) {
        String[] argumentsSplit = strArguments.split(",");
        String targetTable = argumentsSplit[0];
        String condition = argumentsSplit[1];
        getArguments().add(targetTable);

        parseOperator(condition);
        parseOperands(condition, getOperationExpression());
    }

    private void parseOperator(String condition) {
        if (condition.contains(">")) {
            if (condition.contains(">=")) {
                getOperationExpression().setOperator(">=");
            } else {
                getOperationExpression().setOperator(">");
            }
        } else if (condition.contains("<")) {
            if (condition.contains("<=")) {
                getOperationExpression().setOperator("<=");
            } else {
                getOperationExpression().setOperator("<");
            }
        } else if (condition.contains("!=")) {
            getOperationExpression().setOperator("!=");
        } else if (condition.contains("=")) {
            getOperationExpression().setOperator("=");
        } else {
            System.out.println("Error! Something is wrong with the condition, "
                    + "please recheck carefully.");
        }
    }

    private void parseOperands(String condition, OperationExpression oe) {
        if (oe.isEqual()) {
            oe.setOperand1(condition.split("=")[0]);
            oe.setOperand2(condition.split("=")[1]);
        } else if (oe.isGreater()) {
            oe.setOperand1(condition.split(">")[0]);
            oe.setOperand2(condition.split(">")[1]);
        } else if (oe.isLess()) {
            oe.setOperand1(condition.split("<")[0]);
            oe.setOperand2(condition.split("<")[1]);
        } else if (oe.isGreaterEqual()) {
            oe.setOperand1(condition.split(">=")[0]);
            oe.setOperand2(condition.split(">=")[1]);
        } else if (oe.isLessEqual()) {
            oe.setOperand1(condition.split("<=")[0]);
            oe.setOperand2(condition.split("<=")[1]);
        } else if (oe.isNotEqual()) {
            oe.setOperand1(condition.split("!=")[0]);
            oe.setOperand2(condition.split("!=")[1]);
        } else {
            System.out.println("Error! Something is wrong with the condition, "
                    + "please recheck carefully.");
        }
    }


    /**
     * Parse the input string into different kinds of commands.
     * @param str the target string needs to be parsed
     */
    public void parseCommand(String str) {

        // Delete all the spaces, and the commands entered are case agnostic.
        str = str.replaceAll("\\s", "").toLowerCase();
        String[] strSplit = str.split(":=");
        String strParenthesis = null;

        // Normal commands with certain table names.
        if (strSplit.length > 1) {
            setTableName(strSplit[0]);
            strParenthesis = strSplit[1];
        } else if (strSplit.length == 1) {
            // Add index and output file commands, these commands don't have table names.
            setTableName("");
            strParenthesis = strSplit[0];
        }

        assert strParenthesis != null;
        setCommandName(strParenthesis.split("\\(")[0]);

        // To judge whether the input command is valid.
        if (!isValidCommand(getCommandName())) {
            System.out.println("Error! Not a valid command, please recheck.");
            return;
        }

        String strArguments = strParenthesis.substring(
                getCommandName().length() + 1, strParenthesis.length() - 1);

        // The format of the "select" and "join" commands is special,
        // we need to deal with them separately.
        if (isSelect()) {
            setOperationExpression(new OperationExpression());
            parseSelectCommand(strArguments);
            getOperationExpression().formalType();
            return;
        }

        if (isJoin()) {
            setOperationExpression(new OperationExpression());
            parseJoinCommand(strArguments);
            return;
        }

        String[] argumentsSplit = strArguments.split(",");

        resetArguments();
        for (String s : argumentsSplit) {
            getArguments().add(s);
        }
    }


    public boolean isInputFromFile() {
        return getCommandName().equals("inputfromfile");
    }

    public boolean isSelect() {
        return getCommandName().equals("select");
    }

    public boolean isProject() {
        return getCommandName().equals("project");
    }

    public boolean isSum() {
        return getCommandName().equals("sum");
    }

    public boolean isAvg() {
        return getCommandName().equals("avg");
    }

    public boolean isSumGroup() {
        return getCommandName().equals("sumgroup");
    }

    public boolean isAvgGroup() {
        return getCommandName().equals("avggroup");
    }

    public boolean isJoin() {
        return getCommandName().equals("join");
    }

    public boolean isSort() {
        return getCommandName().equals("sort");
    }

    public boolean isMovAvg() {
        return getCommandName().equals("movavg");
    }

    public boolean isMovSum() {
        return getCommandName().equals("movsum");
    }

    public boolean isBtree() {
        return this.getCommandName().equals("btree");
    }

    public boolean isHash() {
        return this.getCommandName().equals("hash");
    }

    public boolean isConcat() {
        return getCommandName().equals("concat");
    }

    public boolean isOutputToFile() {
        return getCommandName().equals("outputtofile");
    }

}
