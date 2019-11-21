package cs363;

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

public class graph {

    private String username;
    private String password;
    private String userInput;

    graph() {
        username = "";
        password = "";
        userInput = "";
    }

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
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbServer, userName, password);
            stmt = conn.createStatement();
            String sqlQuery = "";
            String ProcQuery = "";
            String option = "";

            String instruction = "1: Insert new graph node" + "\n"
                    + "2: Insert new edge"
                    + "\n" + "3: Find all food without green onion as ingredient." + "\n"
                    + "4: Find all ingredients and amount of each ingredient of Pad Thai" + "\n"
                    + "5: Quit Program";

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

                    sqlQuery =
                            "CREATE procedure edge_test(in startnode varchar(10), in endnode varchar(10), in cost int, out failure int(1)) " +
                            "this_proc: BEGIN " +
                            "Declare total_rows int DEFAULT 0; " +
                            "Declare row_a varchar(10); " +
                            "Declare row_b varchar(10); " +
                            "Select nodename From nodes where nodes.nodename = startnode into row_a; " +
                            "Select nodename From nodes where nodes.nodename = endnode into row_b; " +
                            "if(row_a = startnode and row_b = endnode) THEN " +
                            "insert ignore into edges values (startnode, endnode, cost); " +
                            "LEAVE this_proc; " +
                            "elseif (row_a != startnode) then " +
                            "set failure = 1; " +
                            "LEAVE this_proc; " +
                            "elseif (row_b != endnode) then " +
                            "set failure = 2; " +
                            "LEAVE this_proc; " +
                            "END IF; " +
                            "END";

                           ProcQuery = "{CALL edge_test(?,?,?,?)}";

                    runQuery(stmt, sqlQuery, 1);
                    cs = conn.prepareCall(ProcQuery);


                    //failure = runQuery(cs, ProcQuery, 2);

                    cs.setString("startnode", startnode);
                    cs.setString("endnode", endnode);
                    cs.setString("cost", cost);
                    cs.registerOutParameter(4, java.sql.Types.INTEGER);

                    cs.execute();
                    rs = cs.getResultSet();
                    rs.next();
                    failure = rs.next();
                    System.out.println(IsResultSet);
                    System.out.println("failure: " + failure);

                    if(failure == 1){
                        String newImportance = "Startnode doesn't exist, input node's importance" ;
                        String importance = JOptionPane.showInputDialog(newImportance);
                        String newQuery = "insert into nodes values (\'" + startnode + "\', \'" + importance + "\')";
                        failure = runQuery(stmt, sqlQuery, 0);
                    }
                    if(failure == 2){
                        String newImportance = "Startnode doesn't exist, input node's importance" ;
                        String importance = JOptionPane.showInputDialog(newImportance);
                        String newQuery = "insert into nodes values (\'" + endnode + "\', \'" + importance + "\')";
                        failure = runQuery(stmt, sqlQuery, 0);
                    }
                } else if (option.equals("3")) {
                    sqlQuery = "";
                    runQuery(stmt, sqlQuery, 1);
                } else if (option.equals("4")) {
                    sqlQuery = "";
                    runQuery(stmt, sqlQuery, 1);
                } else {
                    break;
                }
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Program terminates due to errors");
            e.printStackTrace(); // for debugging}
        }


    }

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



