package device.sdk.sample.scanner;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;


import java.sql.*;
import java.lang.*;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;


/**
 * Created by Nathan Shih on 2016/10/27.
 */

public class MSSQL {
    // 在這裡定義各個參數的屬性

    public static int[] columnMaxLength;
    public static String[] columnTypeName;
    public static ArrayList<Integer> RowMaxLength = new ArrayList<>();
    public static ArrayList<ArrayList<String>> SQLdata = new ArrayList<ArrayList<String>>();

    private boolean _isOpened = false;
    private Connection connect;

    public boolean isOpened() {
        return _isOpened;
    }

    public void CloseConnect() {
        try {
            if (_isOpened == true) {
                connect.close();
                _isOpened = false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    //  user ID  ,  user password,    table,  ip,  command
    private Connection ConnectionHelper(String user, String password, String database, String server) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String ConnectionURL = null;
        try {

            // connect to MySQL
            /*
            Class.forName("com.mysql.jdbc.Driver");
            ConnectionURL = "jdbc:mysql://" + server + "/" + database;
            connect = DriverManager.getConnection(ConnectionURL, user, password);*/

            // connect to MSSQL(SQL server)

            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            ConnectionURL = "jdbc:jtds:sqlserver://" + server + ";" +
                    "databaseName=" + database + ";user=" + user +
                    ";password=" + password + ";";
            connect = DriverManager.getConnection(ConnectionURL);

        } catch (SQLException se) {
            Log.e("ERROR", se.getMessage());
            //  do the error message output to textview
        } catch (ClassNotFoundException e) {
            Log.e("ERROR", e.getMessage());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }
        return connect;
    }

    private String getDataHelper(String Command) {
        String Success = "";
        try {

            Statement stmt = connect.createStatement();// create the Statement
            ResultSet rs = stmt.executeQuery(Command);  // 0rder the command to sql and return the result
            ResultSetMetaData metadata = rs.getMetaData(); //the result of metata, used in get the data type
            Success = "OK";

            SQLdata.clear();
            RowMaxLength.clear();
            RowMaxLength.add(0);

            columnMaxLength = new int[rs.getMetaData().getColumnCount()];
            columnTypeName = new String[rs.getMetaData().getColumnCount()]; // initial the String array all TypeName

            ArrayList<String> inSideList = new ArrayList<String>();

            // get the sql column Name and data type, and the data transfer to string into Result
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                inSideList.add(rs.getMetaData().getColumnName(i + 1));
                columnTypeName[i] = rs.getMetaData().getColumnTypeName(i + 1);

                if (inSideList.get(inSideList.size() - 1).toString().length() > columnMaxLength[i]) // get the Max of column length
                    columnMaxLength[i] = inSideList.get(inSideList.size() - 1).toString().length();

                if (inSideList.get(inSideList.size() - 1).toString().length() > RowMaxLength.get(RowMaxLength.size() - 1)) // get the Max of row length
                    RowMaxLength.set(RowMaxLength.size() - 1, inSideList.get(inSideList.size() - 1).toString().length());

            }
            SQLdata.add(inSideList);


            // Row by Row read sql datum, and the data transfer to string into Result
            while (rs.next()) {
                inSideList = new ArrayList<String>();
                RowMaxLength.add(0);
                for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                    if (rs.getString(rs.getMetaData().getColumnName(i + 1)) != null) {  // the data is not empty ...

                        if (metadata.getColumnTypeName(i + 1).equals("bit")) // if the data type is bit (tboolean) ...
                        {
                            if (rs.getString(rs.getMetaData().getColumnName(i + 1)) == "0")
                                // the sql data into the Result (ArrayList)
                                inSideList.add("False");
                            else if (rs.getString(rs.getMetaData().getColumnName(i + 1)) == "1")
                                // the sql data into the Result (ArrayList)
                                inSideList.add("True");
                        } else   // if the data type isn't bit (boolean) ...
                            // the sql data into the Result (ArrayList)
                            inSideList.add(rs.getString(rs.getMetaData().getColumnName(i + 1)));
                    } else { // if the data is empty, give the string "Null" ...
                        inSideList.add("Null");
                    }

                    if (inSideList.get(inSideList.size() - 1).toString().length() > columnMaxLength[i]) // get the Max of column length
                        columnMaxLength[i] = inSideList.get(inSideList.size() - 1).toString().length();
                    if (inSideList.get(inSideList.size() - 1).toString().length() > RowMaxLength.get(RowMaxLength.size() - 1)) // get the Max of row length
                        RowMaxLength.set(RowMaxLength.size() - 1, inSideList.get(inSideList.size() - 1).toString().length());
                }
                SQLdata.add(inSideList);
            }
        } catch (SQLException se) {
            Log.e("ERROR", se.getMessage());
            Success = se.getMessage();
            //  do the error message output to textview
            //MainActivity.say(se.getMessage().toString());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
            Success = e.getMessage();
            //MainActivity.say(e.getMessage().toString());
        }
        return Success;
    }


    public MSSQL(String ipaddressA, String dbA, String usernameA, String passwordA) {
        try {
            connect = ConnectionHelper(usernameA, passwordA, dbA, ipaddressA);

            if (connect.isClosed() == false)
                _isOpened = true;
            else
                _isOpened = false;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getData(String Command) {
        String sccess = "";
        try {
            sccess = getDataHelper(Command);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sccess;
    }
}