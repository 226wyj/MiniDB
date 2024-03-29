package com.nyu.database.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nyu.database.dao.DataReader;
//import com.nyu.database.index.hash.HashTable;
import com.nyu.database.parser.CommandParser;
import com.nyu.database.parser.OperationExpression;

public class DataBase {
    //----------------
    // Attributes
    //----------------
    private Map<String, Table> tables;


    //----------------
    // Constructor(s)
    //----------------
    public DataBase() {
        tables = new LinkedHashMap<>();
    }


    //----------------
    // Accessors
    //----------------
    public void setTables(LinkedHashMap<String, Table> tables) {
        this.tables = tables;
    }

    public Map<String, Table> getTables() {
        return this.tables;
    }


    //----------------
    // Other Methods
    //----------------
    public int getTableNum() {
        return getTables().size();
    }

    public void getDBMessage() {
        if (getTableNum() == 0) {
            System.out.println("There is no table in the current DB!");
            return;
        }
        System.out.println("Total number of tables:" + getTableNum());
        for (Map.Entry<String, Table> entry : getTables().entrySet()) {
            System.out.print(entry.getKey() + "\t");
        }
        System.out.println();
    }

    /**
     * To get a table from DB by the table's name.
     *
     * @param name the table's name that we want to get.
     * @return target table if the DB contains the table, otherwise null.
     */
    private Table getTableByName(String name) {
        for (String tableName : getTables().keySet()) {
            if (tableName.equals(name)) {
                return getTables().get(name);
            }
        }
        return null;
    }

    /**
     * Read data from a target file and create a corresponding table if
     * there is no table with the same name. Otherwise, the old table with
     * the same name will be overlapped.
     *
     * @param parser used for parsing command.
     * @throws IOException if anything goes wrong when reading files.
     */
    public void inputFromFile(CommandParser parser) throws IOException {
        String tableName = parser.getTableName();
        Table table = new Table(tableName);
        if (getTables().containsKey(tableName)) {
            System.out.println("Warning! There has already existed "
                    + "a table with the same name, the old one will"
                    + "be overlapped.");
            getTables().remove(tableName);
        }

        // Read file.
        String fileName = parser.getArguments().get(0);
        DataReader dataReader = new DataReader();
        dataReader.readFile(fileName);
        table.importFile(dataReader);

        // Add table to the DB.
        getTables().put(tableName, table);
    }

    public void outputToFile(String tableName, String fileName) throws IOException {
        Table table = getTableByName(tableName);
        if (table == null) {
            System.out.println("Error! The target table doesn't exist, please "
                    + "recheck carefully!");
            return;
        }
        table.outputFile(fileName);
    }

    public void outputToFile(String tableName,
                             String fileName, String delimiter) throws IOException {
        Table table = getTableByName(tableName);
        if (table == null) {
            System.out.println("Error! The target table doesn't exist, please "
                    + "recheck carefully!");
            return;
        }
        table.outputFile(fileName, delimiter);
    }

    public void outputToFile(CommandParser parser) {
        String tableName = parser.getArguments().get(0);
        Table table = getTableByName(tableName);
        if (table == null) {
            System.out.println("Error! The target table doesn't exist, please "
                    + "recheck carefully!");
            return;
        } else {
            table.showTable();
        }
    }

    /**
     * Get certain column from the target table by column's name, and update
     * the corresponding message in the new table.
     *
     * @param targetTable the target table
     * @param newTable the new table
     * @param columnName the column's name
     */
    private void updateColumns(Table targetTable, Table newTable, String columnName) {
        if (!targetTable.getColumnNames().contains(columnName)) {
            System.out.println("Error! There is something wrong with the column "
                    + "name, please recheck carefully!");
            return;
        }
        // Update column names.
        newTable.getColumnNames().add(columnName);
        // Update corresponding column data.
        List<Integer> columnData = targetTable.getColumnData().get(columnName);
        newTable.getColumnData().put(columnName, columnData);
    }

    /**
     * Do projection operation on certain columns of the target table.
     *
     * @param parser used for parsing the command
     */
    public void project(CommandParser parser) {
        Table targetTable = getTableByName(parser.getArguments().get(0));

        // Check if the target exists.
        if (targetTable == null) {
            System.out.println("Error! The target table doesn't exist, please "
                    + "recheck carefully!");
            return;
        }

        String newName = parser.getTableName();
        Table newTable = new Table(newName);

        for (int i = 1; i < parser.getArguments().size(); i++) {
            String columnName = parser.getArguments().get(i);
            updateColumns(targetTable, newTable, columnName);
        }
        // Update row data.
        newTable.updateRowData(null);

        // Add the new table to current DB.
        getTables().put(newName, newTable);
    }

    /**
     * Calculate the summation or average value of certain column.
     * @param parser used for parsing the command
     * @param mode either "avg" or "sum", "avg" means get the average value of the
     *             target column, "sum" means get the summation value of the target
     *             column.
     */
    public void avgOrSum(CommandParser parser, String mode) {
        try {
            // Create a new table.
            String newName = parser.getTableName();
            Table newTable = new Table(newName);

            // Get the target column name and data.
            String columnName = parser.getArguments().get(1);
            List<Integer> targetColumn = getTargetColumnData(parser);


            // Calculate the average/sum value of the column.
            int sum = 0;
            for (int columnValue : targetColumn) {
                sum += columnValue;
            }
            int avgValue = sum / targetColumn.size();

            // Set column name and the corresponding data.
            // Update the row data as well.
            String newColumnName = mode + "(" + columnName + ")";
            newTable.getColumnNames().add(newColumnName);
            ArrayList<Integer> newColumnData = new ArrayList<>();

            if (mode.equals("avg")) {
                newColumnData.add(avgValue);
            } else if (mode.equals("sum")) {
                newColumnData.add(sum);
            } else {
                System.out.println("Error! Mode can only be avg or sum!");
                return;
            }

            newTable.getColumnData().put(newColumnName, newColumnData);
            newTable.updateRowData(null);
            getTables().put(newName, newTable);
        } catch (NullPointerException e) {
            System.out.println("Error! The target table or column doesn't exist, "
                    + "please recheck carefully!");
        }
    }

    /**
     * Calculate the moving summation or average value of certain column.
     * @param parser used for parsing the command.
     * @param mode either "avg" or "sum", "avg" means get the moving average
     *             value of the target column, "sum" means get the moving
     *             summation value of the target column.
     */
    public void movAvgOrSum(CommandParser parser, String mode) {
        try {
            // Get the target column name and data.
            String columnName = parser.getArguments().get(1);
            List<Integer> targetColumn = getTargetColumnData(parser);

            // Create a new table, set its column name.
            String newName = parser.getTableName();
            Table newTable = new Table(newName);

            Table targetTable = getTableByName(parser.getArguments().get(0));
            newTable.setColumnNames((List<String>) targetTable.getColumnNames().clone());
            newTable.setColumnData((Map<String, List<Integer>>)
                    targetTable.getColumnData().clone()
            );

            String newColumnName = "mov" + mode + "(" + columnName + ")";
//            newTable.getColumnNames().add(newColumnName);

            // Get the step length.
            int k = Integer.parseInt(parser.getArguments().get(2));

            // Calculate the moving average and the moving summation.
            ArrayList<Integer> newColumnData = new ArrayList<>();
            if (mode.equals("avg")) {
                for (int i = 0; i < targetColumn.size(); i++) {
                    int tmp = 0;
                    if (i < k - 1) {
                        for (int j = 0; j <= i; j++) {
                            tmp += targetColumn.get(j);
                        }
                        newColumnData.add(tmp / (i + 1));
                    } else {
                        for (int j = i - k + 1; j <= i; j++) {
                            tmp += targetColumn.get(j);
                        }
                        newColumnData.add(tmp / k);
                    }
                }
            } else if (mode.equals("sum")) {
                for (int i = 0; i < targetColumn.size(); i++) {
                    int tmp = 0;
                    if (i < k - 1) {
                        for (int j = 0; j <= i; j++) {
                            tmp += targetColumn.get(j);
                        }
                    } else {
                        for (int j = i - k + 1; j <= i; j++) {
                            tmp += targetColumn.get(j);
                        }
                    }
                    newColumnData.add(tmp);
                }
            }

            newTable.getColumnData().put(columnName, newColumnData);

//            newTable.getColumnData().put(newColumnName, newColumnData);
            newTable.updateRowData(null);
            getTables().put(newName, newTable);
        } catch (NullPointerException e1) {
            System.out.println("Moving command Error! The target table or column "
                    + "doesn't exist, please recheck carefully!");
        } catch (NumberFormatException e2) {
            System.out.println("Moving command Error! The step length of the mov "
                    + "commands must be integer, please recheck carefully!");
        }
    }

    private List<Integer> getTargetColumnData(CommandParser parser) {
        String targetTableName = parser.getArguments().get(0);
        Table targetTable = getTableByName(targetTableName);
        String targetColumnName = parser.getArguments().get(1);
        assert targetTable != null;
        return targetTable.getColumnData().get(targetColumnName);
    }

    /**
     * Join two tables based on the join condition.
     * @param parser
     * @throws NullPointerException
     */
    public void join(CommandParser parser) throws NullPointerException {
        try {
            // Get essential message.
            String newName = parser.getTableName();
            Table newTable = new Table(newName);
            String targetTable1Name = parser.getArguments().get(0);
            String targetTable2Name = parser.getArguments().get(1);
            Table targetTable1 = getTableByName(targetTable1Name);
            Table targetTable2 = getTableByName(targetTable2Name);

            // Update the column names of the new table.
            assert targetTable1 != null;
            assert targetTable2 != null;
            for (String name : targetTable1.getColumnNames()) {
                newTable.getColumnNames().add(targetTable1Name + "_" + name);
            }
            for (String name : targetTable2.getColumnNames()) {
                newTable.getColumnNames().add(targetTable2Name + "_" + name);
            }

            // Get the join condition.
            OperationExpression condition = parser.getOperationExpression();
            String table1 = condition.getOperand1().split("\\.")[0];
            String column1 = condition.getOperand1().split("\\.")[1];
            String table2 = condition.getOperand2().split("\\.")[0];
            String column2 = condition.getOperand2().split("\\.")[1];
            String operator = condition.getOperator();
            List<Integer> columnData1 = getTableByName(table1).getColumnData().get(column1);
            List<Integer> columnData2 = getTableByName(table2).getColumnData().get(column2);

            // Parse the operator
            List<List<Integer>> newRowData = new ArrayList<>();
            List<List<Integer>> oldRowData1 = targetTable1.getRowData();
            List<List<Integer>> oldRowData2 = targetTable2.getRowData();

            // Get cartesian product of the two tables.
            for (int i = 0; i < columnData1.size(); i++) {
                for (int j = 0; j < columnData2.size(); j++) {
                    switch (operator) {
                        case "<":
                            if (columnData1.get(i) < columnData2.get(j)) {
                                updateNewRowData(newRowData, oldRowData1, oldRowData2, i, j);
                            }
                            break;
                        case ">":
                            if (columnData1.get(i) > columnData2.get(j)) {
                                updateNewRowData(newRowData, oldRowData1,
                                        oldRowData2, i, j);
                            }
                            break;
                        case "=":
                            if (columnData1.get(i) == columnData2.get(j)) {
                                updateNewRowData(newRowData, oldRowData1,
                                        oldRowData2, i, j);
                            }
                            break;
                        case "<=":
                            if (columnData1.get(i) <= columnData2.get(i)) {
                                updateNewRowData(newRowData, oldRowData1,
                                        oldRowData2, i, j);
                            }
                            break;
                        case ">=":
                            if (columnData1.get(i) >= columnData2.get(i)) {
                                updateNewRowData(newRowData, oldRowData1,
                                        oldRowData2, i, j);
                            }
                            break;
                        case "!=":
                            if (columnData1.get(i) != columnData2.get(i)) {
                                updateNewRowData(newRowData, oldRowData1,
                                        oldRowData2, i, j);
                            }
                            break;
                        default:
                            System.out.println("Error! The operator can only be '>', '<', "
                                    + "'=', '>=', '<=', or '!='. Please recheck.");
                            return;
                    }
                }
            }

            newTable.setRowData(newRowData);
            newTable.updateColumnData();
            getTables().put(newName, newTable);
        } catch (NullPointerException e) {
            System.out.println("Join Error! No such table, please recheck.");
        }
    }

    /**
     * First organize the ith row of oldRowData1 and the jth row of oldRowData2
     * into a new row, and then add the new row to the newRowData.
     *
     * @param newRowData the result new rows.
     * @param oldRowData1 target rows1
     * @param oldRowData2 target rows2
     * @param i column index of target rows1
     * @param j column index of target row2
     */
    private void updateNewRowData(
            List<List<Integer>> newRowData, List<List<Integer>> oldRowData1,
            List<List<Integer>> oldRowData2, int i, int j) {
        ArrayList<Integer> currentRow = new ArrayList<>();
        currentRow.addAll(oldRowData1.get(i));
        currentRow.addAll(oldRowData2.get(j));
        newRowData.add(currentRow);
    }


    /**
     * Select certain columns from the target table under the restriction of
     * some conditions.
     * @param parser used for parsing the command.
     */
    public void select(CommandParser parser) {
        Table targetTable = getTableByName(parser.getArguments().get(0));

        // Check if the target table exists.
        if (targetTable == null) {
            throw new IllegalArgumentException(
                    "The target table doesn't exist, please recheck carefully!");
        }

        // Create a new table and copy all the column names.
        String newName = parser.getTableName();
        Table newTable = new Table(newName);
        newTable.setColumnNames(targetTable.getColumnNames());

        // Get the select conditions.
        OperationExpression condition = parser.getOperationExpression();
        // Operand 1 is an integer
        if (condition.isOperand1Int()) {
            int constant = Integer.parseInt(condition.getOperand1());
            String columnName = condition.getOperand2();
            selectByConstant(targetTable, newTable, columnName,
                    condition.getOperator(), constant);
        } else if (condition.isOperand2Int()) {
            // Operand 2 is an integer
            int constant = Integer.parseInt(condition.getOperand2());
            String columnName = condition.getOperand1();
            selectByConstant(targetTable, newTable, columnName,
                    condition.getOperator(), constant);
        } else {
            // Operand 1 and operand 2 are two column names
            String column1 = condition.getOperand1();
            String column2 = condition.getOperand2();
            selectByColumn(targetTable, newTable, column1,
                    condition.getOperator(), column2);
        }
        getTables().put(newName, newTable);
    }

    private void selectByConstant(Table targetTable, Table newTable,
                                  String columnName, String operator,
                                  int constant)
            throws NullPointerException {
        try {
            List<Integer> columnData = targetTable.getColumnData().get(columnName);
            List<List<Integer>> oldRowData = targetTable.getRowData();
            List<List<Integer>> newRowData = new ArrayList<>();

            for (int i = 0; i < columnData.size(); i++) {
                switch (operator) {
                    case "<":
                        if (columnData.get(i) < constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case ">":
                        if (columnData.get(i) > constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "=":
//                        System.out.println("No index.");
                        if (columnData.get(i) == constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "<=":
                        if (columnData.get(i) <= constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case ">=":
                        if (columnData.get(i) >= constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "!=":
                        if (columnData.get(i) != constant) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Error! The operator can only be '>', '<', '=', "
                                        + "'>=', '<=', or '!='. Please recheck.");
                }
            }
            newTable.setRowData(newRowData);
            newTable.updateColumnData();
        } catch (NullPointerException e) {
            System.out.println("Error! The column name doesn't exist! Please recheck.");
        }
    }

    private void selectByColumn(Table targetTable, Table newTable,
                                  String column1, String operator,
                                  String column2) throws NullPointerException {
        try {
            List<Integer> columnData1 = targetTable.getColumnData().get(column1);
            List<Integer> columnData2 = targetTable.getColumnData().get(column2);
            List<List<Integer>> oldRowData = targetTable.getRowData();
            List<List<Integer>> newRowData = new ArrayList<>();

            assert columnData1.size() == columnData2.size();

            for (int i = 0; i < columnData1.size(); i++) {
                switch (operator) {
                    case "<":
                        if (columnData1.get(i) < columnData2.get(i)) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case ">":
                        if (columnData1.get(i) > columnData2.get(i)) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "=":
                        if (columnData1.get(i).equals(columnData2.get(i))) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "<=":
                        if (columnData1.get(i) <= columnData2.get(i)) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case ">=":
                        if (columnData1.get(i) >= columnData2.get(i)) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    case "!=":
                        if (!columnData1.get(i).equals(columnData2.get(i))) {
                            newRowData.add(oldRowData.get(i));
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Error! The operator can only be '>', '<', '=', "
                                        + "'>=', '<=', or '!='. Please recheck.");
                }
            }
            newTable.setRowData(newRowData);
            newTable.updateColumnData();
        } catch (NullPointerException e) {
            System.out.println("Error! The column name doesn't exist! Please recheck.");
        }
    }

    /**
     * Sort the table based on the natural order of its certain column data.
     *
     * @param parser used for parsing the command.
     * @throws NullPointerException
     */
    public void sort(CommandParser parser) throws NullPointerException {
        try {
            // Get the target column name and data.
            String columnName = parser.getArguments().get(1);
            Table targetTable = getTableByName(parser.getArguments().get(0));

            assert targetTable != null;
            List<Integer> targetColumn = targetTable.getColumnData().get(columnName);

            // Create a new table, set its column names.
            String newName = parser.getTableName();
            Table newTable = new Table(newName);
            newTable.setColumnNames(targetTable.getColumnNames());

            // Selection sort in the natural order.
            ArrayList<ArrayList<Integer>> tmpRows =
                    (ArrayList<ArrayList<Integer>>) targetTable.getRowData().clone();
            ArrayList<Integer> tmpColumn = (ArrayList<Integer>) targetColumn.clone();
            while (!tmpColumn.isEmpty()) {
                int minIndex = tmpColumn.indexOf(getMin(tmpColumn));
                newTable.getRowData().add(tmpRows.get(minIndex));
                tmpColumn.remove(minIndex);
                tmpRows.remove(minIndex);
            }

            // Update corresponding column data.
            newTable.updateColumnData();

            // Add the new table to the current DB.
            getTables().put(parser.getTableName(), newTable);
        } catch (NullPointerException e) {
            System.out.println("Sort command Error! The target tables "
                    + "don't exist, please recheck carefully!");
        }
    }

    private int getMin(ArrayList<Integer> a) {
        double min = Double.POSITIVE_INFINITY;
        for (int data : a) {
            if (data < min) {
                min = data;
            }
        }
        return (int) min;
    }

    /**
     * Concatenate two tables in the order of columns.
     *
     * @param parser used for parsing the command.
     */
    public void concat(CommandParser parser) throws NullPointerException {
        try {
            // Get two target tables.
            Table targetTable1 = getTableByName(parser.getArguments().get(0));
            Table targetTable2 = getTableByName(parser.getArguments().get(1));
            assert targetTable1 != null;
            assert targetTable2 != null;

            // Generate a new table and initialize its table name and
            // the column names.
            Table newTable = new Table(parser.getTableName());
            newTable.setColumnNames(targetTable1.getColumnNames());

            // Append rows into the new table.
            for (List<Integer> row : targetTable1.getRowData()) {
                newTable.getRowData().add(row);
            }
            for (List<Integer> row : targetTable2.getRowData()) {
                newTable.getRowData().add(row);
            }

            // Update corresponding columns.
            newTable.updateColumnData();

            // Add the new table to the current DB.
            getTables().put(parser.getTableName(), newTable);
        } catch (NullPointerException e) {
            System.out.println("Concat command Error! The target tables "
                    + "don't exist, please recheck carefully!");
        }
    }

    /**
     *
     * @param parser
     * @param mode
     * @throws NullPointerException
     */
    public void sumOrAvgGroup(CommandParser parser, String mode) throws NullPointerException {
        try {
            // Get the target column name and data.
            String columnName = parser.getArguments().get(1);
            Table targetTable = getTableByName(parser.getArguments().get(0));

            assert targetTable != null;
            int targetColumnIndex = targetTable.getColumnNames().indexOf(columnName);
            List<Integer> targetColumnData = targetTable.getColumnData().get(columnName);

            // Get the column messages that will be grouped.
            Table tmpGroupTable = new Table();

            for (int i = 2; i < parser.getArguments().size(); i++) {

                String groupColumnName = parser.getArguments().get(i);
                List<Integer> groupColumnData = targetTable.getColumnData().get(groupColumnName);

                tmpGroupTable.getColumnNames().add(groupColumnName);
                tmpGroupTable.getColumnData().put(groupColumnName, groupColumnData);
            }

            // Update the row data of the temp group table.
            tmpGroupTable.updateRowData(null);

            // To organize the data into groups, we use LinkedHashSet to
            // eliminate the duplicate column data.
            Set<List<Integer>> groupsWithoutDuplicate = new LinkedHashSet<>(tmpGroupTable.getRowData());

            // The HashMap represents for each group and the corresponding target column values.
            Map<List<Integer>, List<Integer>> groupDivision = new LinkedHashMap<>();
            for (List<Integer> group : groupsWithoutDuplicate) {
                groupDivision.put(group, new ArrayList<>());
            }

            for (List<Integer> row : targetTable.getRowData()) {
                List<Integer> currentRowGroup = new ArrayList<>();

                // For current row, we organize the certain columns' data into a group.
                for (String groupColumnName : tmpGroupTable.getColumnNames()) {
                    int columnIndex = targetTable.getColumnNames().indexOf(groupColumnName);
                    currentRowGroup.add(row.get(columnIndex));
                }

                // The currentRowGroup is a key, and we add corresponding column value
                // to the right group.
                groupDivision.get(currentRowGroup).add(row.get(targetColumnIndex));
            }

            // Create a new table.
            Table newTable = new Table(parser.getTableName());

            // Set the column name. (CList + sum/avg)
            for (String name : tmpGroupTable.getColumnNames())
                newTable.getColumnNames().add(name);
            String firstColumnName = mode + "(" + columnName + ")";
            newTable.getColumnNames().add(firstColumnName);

            // Set the data.
            int count = 0;
            for (List<Integer> group : groupDivision.keySet()) {
                ArrayList<Integer> tmpRow = (ArrayList<Integer>) group.clone();
                if (mode.equals("sum")) {
                    tmpRow.add(sumArrayList(groupDivision.get(group)));
                } else if (mode.equals("avg")) {
                    int length = groupDivision.get(group).size();
                    tmpRow.add(sumArrayList(groupDivision.get(group)) / length);
                }
                newTable.getRowData().add(tmpRow);
            }

            // Add the new table to the current DB.
            getTables().put(parser.getTableName(), newTable);

        } catch (NullPointerException e) {
            System.out.println("SumGroup or AvgGroup Error!");
        }

    }

    private int sumArrayList(ArrayList<Integer> array) {
        int result = 0;
        for (int i : array) {
            result += i;
        }
        return result;
    }


//    // TODO: Implementation of hash index.
//    public void hash(CommandParser parser, HashTable hashTable) throws
//            NullPointerException, IOException {
//
//    }
//
//    // TODO: Implementation of bTree index.
//    public void bTree(CommandParser parser) throws NullPointerException {
//        return;
//    }

}
