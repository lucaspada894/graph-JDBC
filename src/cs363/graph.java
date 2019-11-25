package cs363;

import com.mysql.cj.xdevapi.UpdateStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * @author Gian Spadafora
 */
public class graph {

    private String username;
    private String password;
    private String userInput;

    graph() {
        username = "";
        password = "";
        userInput = "";
    }

    /**
     * runs the main program and takes inputs from the user. The user can select options 1, 2, 3, or 4 to insert a node,
     * insert an edge, delete a node, or search for reachable nodes.
     *
     */

    public static void run() {
        String dbServer = "jdbc:mysql://localhost:3306/graph";
        String userName = "";
        String password = "";
        String result[] = loginDialog();
        userName = result[0];
        password = result[1];
        Connection conn;
        Statement stmt;
        ResultSet rs;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(dbServer, userName, password);
            stmt = conn.createStatement();
            String sqlQuery = "";
            String ProcQuery = "";
            String option = "";

            String instruction = "1: Insert new graph node" + "\n"
                    + "2: Insert new edge"
                    + "\n" + "3: Remove a node" + "\n"
                    + "4: Find reachable nodes";

            while (true) {
                option = JOptionPane.showInputDialog(instruction);

                if (option.equals("1")) {
                    String checkResults = null;
                    String opt1 = "insert name";
                    String opt2 = "Insert importance of node";
                    String name = JOptionPane.showInputDialog(opt1);
                    String importance = JOptionPane.showInputDialog(opt2);
                    sqlQuery = "insert ignore into nodes values (\'" + name + "\', \'" + importance + "\')";
                    runQuery(stmt, sqlQuery, 1);

                } else if (option.equalsIgnoreCase("2")) {
                    int failure = 0;
                    CallableStatement cs;
                    String opt1 = "What is the startnode?";
                    String opt2 = "What is the endnode?";
                    String opt3 = "What is the cost of this edge?";
                    String startnode = JOptionPane.showInputDialog(opt1);
                    String endnode = JOptionPane.showInputDialog(opt2);
                    String cost = JOptionPane.showInputDialog(opt3);

                    String dropQuery = "drop procedure if exists edge_test;";
                    runQuery(stmt, dropQuery, 1);

                    String sqlQuery1 = "Select * from nodes where nodename = \'" + startnode + "\'";
                    String sqlQuery2 = "Select * from nodes where nodename = \'" + endnode + "\'";

                    stmt.executeQuery(sqlQuery1);
                    rs = stmt.getResultSet();

                    if(rs.next() == false){
                        String askImportance = "startnode doesn't exist. Give an importance value for it";
                        String importance = JOptionPane.showInputDialog(askImportance);
                        String insertA = "insert into nodes values (\'" + startnode + "\',\'" + importance + "\')";
                        stmt.executeUpdate(insertA);
                    }

                    stmt.executeQuery(sqlQuery2);
                    rs = stmt.getResultSet();

                    if(rs.next() == false){
                        String askImportance = "endnode doesn't exist. Give an importance value for it";
                        String importance = JOptionPane.showInputDialog(askImportance);
                        String insertB = "insert into nodes values (\'" + endnode + "\',\'" + importance + "\')";
                        stmt.executeUpdate(insertB);
                    }

                    String sqlQueryInsertEdge = "insert into edges values (\'" + startnode + "\'," + "\'" + endnode + "\'," + "\'" + cost + "\')";
                    stmt.executeUpdate(sqlQueryInsertEdge);


                } else if (option.equals("3")) {
                    String deleteString = "Select node to delete";
                    String node = JOptionPane.showInputDialog(deleteString);
                    String deleteFromEdges = "Delete from edges where startnode = \'" + node + "\'" + "OR endnode = \'" + node + "\'";
                    stmt.executeUpdate(deleteFromEdges);
                    String deleteQuery = "Delete from nodes where nodename = \'" + node + "\'";
                    stmt.executeUpdate(deleteQuery);


                } else if (option.equals("4")) {

                    CallableStatement cs;
                    sqlQuery = "Find reachable nodes from";
                    String node = JOptionPane.showInputDialog(sqlQuery);

                    String dropQuery1 = "drop procedure if exists reachable";
                    String dropQuery2 = "drop procedure if exists reach";

                    stmt.executeUpdate(dropQuery1);
                    stmt.executeUpdate(dropQuery2);

                    String setRecursion = "SET @@Session.max_sp_recursion_depth = 255;";
                    stmt.executeUpdate(setRecursion);

                    String createReach =

                    "create  procedure reach() " +
                    "BEGIN " +
                    "Truncate table newneighbors; " +
                    "insert into results(Select temp.startnode, temp.endnode from temp); " +
                    "Insert into newneighbors (Select t.startnode, e.endnode from temp t inner join edges e on t.endnode = e.startnode); " +

                    "IF((Select count(*) from newneighbors) > 0) Then " +
                        "truncate table temp; " +
                        "insert into temp(Select * from newneighbors); " +
                        "call reach(); " +
                    "END IF; " +

                    "END ";

                    stmt.executeUpdate(createReach);

                    String createReachable =
                    "create procedure reachable(in origin varchar(10)) " +
                    "BEGIN " +
                    "drop table if exists results; " +
                    "drop table if exists temp; " +
                    "drop table if exists newneighbors; " +

                    "Create table results(startnode varchar(10), endnode varchar(10)); " +
                    "Create table temp(startnode varchar(10), endnode varchar(10)); " +
                    "Create table newneighbors(startnode varchar(10), endnode varchar(10)); " +

                    "Insert into temp(Select startnode, endnode from edges where startnode = origin); " +

                    "call reach(); " +

                    "Select * from results; " +

                    "END ";

                    stmt.executeUpdate(createReachable);

                    cs = conn.prepareCall("{call reachable(?)}");
                    cs.setString(1, node);
                    cs.executeQuery();
                    rs = cs.getResultSet();
                    ResultSetMetaData rsmd = rs.getMetaData();

                    int columnsNumber = rsmd.getColumnCount();

                    System.out.println("Original node on the left");
                    while(rs.next()){

                        for (int i = 1; i <= columnsNumber; i++) {
                            if (i > 1) System.out.print(",  ");
                            String columnValue = rs.getString(i).substring(0, 1);
                            System.out.print(columnValue);
                        }
                        System.out.println("");
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Program terminates due to errors");
            e.printStackTrace(); // for debugging}
        }


    }

    /**
     * Asks user for DB username and password.
     * @return String with username and password.
     */
    public static String[] loginDialog() {
        String result[] = new String[2];
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        JLabel lbUsername = new JLabel("Username: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);
        JTextField tfUsername = new JTextField(20);
        cs.gridx = 1;cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);
        JLabel lbPassword = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);
        JPasswordField pfPassword = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(pfPassword, cs);
        panel.setBorder(new LineBorder(Color.GRAY));
        String[] options = new String[] { "OK", "Cancel" };
        int ioption = JOptionPane.showOptionDialog(null, panel, "Login", JOptionPane.OK_OPTION,JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (ioption == 0) // pressing OK button
        {
            result[0] = tfUsername.getText();result[1] = new String(pfPassword.getPassword());
        }
        return result;
    }

    private static int runQuery(Statement stmt, String sqlQuery, int type) throws SQLException {
        ResultSet rs;
        boolean isResultSet = false;
        int us = 0;
        int failure = 0;
        ResultSetMetaData rsMetaData;
        String toShow;
        if (type == 0) {
            rs = stmt.executeQuery(sqlQuery);
            rsMetaData = rs.getMetaData();
            failure = rs.getInt(0);
            System.out.println(sqlQuery);
            toShow = "";
            while (rs.next()) {
                for (int i = 0; i < rsMetaData.getColumnCount(); i++) {
                    toShow += rs.getString(i + 1) + ", ";
                }
                toShow += "\n";
            }
            JOptionPane.showMessageDialog(null, toShow);
        } else if(type == 1) {
            us = stmt.executeUpdate(sqlQuery);
            System.out.println(us);
        } else if(type == 2){
            isResultSet = stmt.execute(sqlQuery);
            System.out.println("result " + isResultSet);
            //failure = stmt.getResultSet();
        }
        return failure;
    }
}



