package Example;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;

public class CopyFileToTable extends JFrame {
  // Text file info
  private JTextField jtfFilename = new JTextField();
  private JTextArea jtaFile = new JTextArea();

  // JDBC and table info
  private JComboBox jcboDriver = new JComboBox(new String[] {
    "com.mysql.jdbc.Driver", "sun.jdbc.odbc.JdbcOdbcDriver",
    "oracle.jdbc.driver.OracleDriver"});
  private JComboBox jcboURL = new JComboBox(new String[] {
    "jdbc:mysql://localhost/test", "jdbc:odbc:exampleMDBDataSource",
    "jdbc:oracle:thin:@liang.armstrong.edu:1521:ora9i"});
  private JTextField jtfUsername = new JTextField();
  private JPasswordField jtfPassword = new JPasswordField();
  private JTextField jtfTableName = new JTextField();

  private JButton jbtViewFile = new JButton("View File");
  private JButton jbtCopy = new JButton("Copy");
  private JLabel jlblStatus = new JLabel();

  public CopyFileToTable() {
    JPanel jPane1 = new JPanel();
    jPane1.setLayout(new BorderLayout());
    jPane1.add(new JLabel("Filename"), BorderLayout.WEST);
    jPane1.add(jbtViewFile, BorderLayout.EAST);
    jPane1.add(jtfFilename, BorderLayout.CENTER);

    JPanel jPane2 = new JPanel();
    jPane2.setLayout(new BorderLayout());
    jPane2.setBorder(new TitledBorder("Source Text File"));
    jPane2.add(jPane1, BorderLayout.NORTH);
    jPane2.add(new JScrollPane(jtaFile), BorderLayout.CENTER);

    JPanel jPane3 = new JPanel();
    jPane3.setLayout(new GridLayout(5, 0));
    jPane3.add(new JLabel("JDBC Driver"));
    jPane3.add(new JLabel("Database URL"));
    jPane3.add(new JLabel("Username"));
    jPane3.add(new JLabel("Password"));
    jPane3.add(new JLabel("Table Name"));

    JPanel jPane4 = new JPanel();
    jPane4.setLayout(new GridLayout(5, 0));
    jcboDriver.setEditable(true);
    jPane4.add(jcboDriver);
    jcboURL.setEditable(true);
    jPane4.add(jcboURL);
    jPane4.add(jtfUsername);
    jPane4.add(jtfPassword);
    jPane4.add(jtfTableName);

    JPanel jPane5 = new JPanel();
    jPane5.setLayout(new BorderLayout());
    jPane5.setBorder(new TitledBorder("Target Database Table"));
    jPane5.add(jbtCopy, BorderLayout.SOUTH);
    jPane5.add(jPane3, BorderLayout.WEST);
    jPane5.add(jPane4, BorderLayout.CENTER);

    getContentPane().add(jlblStatus, BorderLayout.SOUTH);
    getContentPane().add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
      jPane2, jPane5), BorderLayout.CENTER);

    jbtViewFile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        showFile();
      }
    });

    jbtCopy.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        try {
          copyFile();
        }
        catch (Exception ex) {
          jlblStatus.setText(ex.toString());
        }
      }
    });
  }

  /** Display the file in the text area */
  private void showFile() {
    // Use a BufferedReader to read text from the file
    BufferedReader infile = null;

    // Get file name from the text field
    String filename = jtfFilename.getText().trim();

    String inLine;

    try {
      // Create a buffered stream
      infile = new BufferedReader(new FileReader(filename));

      // Read a line and append the line to the text area
      while ((inLine = infile.readLine()) != null) {
        jtaFile.append(inLine + '\n');
      }
    }
    catch (FileNotFoundException ex) {
      System.out.println("File not found: " + filename);
    }
    catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
    finally {
      try {
        if (infile != null) infile.close();
      }
      catch (IOException ex) {
        System.out.println(ex.getMessage());
      }
    }
  }

  private void copyFile() throws Exception {
    // Load the JDBC driver
    System.out.println((String)jcboDriver.getSelectedItem());
    Class.forName(((String)jcboDriver.getSelectedItem()).trim());
    System.out.println("Driver loaded");

    // Establish a connection
    Connection conn = DriverManager.getConnection
      (((String)jcboURL.getSelectedItem()).trim(),
      jtfUsername.getText().trim(),
      String.valueOf(jtfPassword.getPassword()).trim());
    System.out.println("Database connected");

    // Read each line from the text file and insert it to the table
    insertRows(conn);
  }

  private void insertRows(Connection connection) {
    // Build the INSERT statement
    String sqlInsert = "insert into " + jtfTableName.getText()
      + " values (";

    // Use a BufferedReader to read text from the file
    BufferedReader infile = null;

    // Get file name from the text field
    String filename = jtfFilename.getText().trim();

    String inLine;

    try {
      // Create a buffered stream
      infile = new BufferedReader(new FileReader(filename));

      // Create a statement
      Statement statement = connection.createStatement();

      System.out.println("Driver major version? " +
      connection.getMetaData().getDriverMajorVersion());

      // Determine if the supportsBatchUpdates method supported in
      // DatabaseMetaData
      boolean batchUpdatesSupported = false;

      try {
        if (connection.getMetaData().supportsBatchUpdates()) {
          batchUpdatesSupported = true;
          System.out.println("batch updates supported");
        }
        else {
          System.out.println("The driver is of JDBC 2 type, but " +
          "does not support batch updates");
        }
      }
      catch (UnsupportedOperationException ex) {
        System.out.println("The driver does not support JDBC 2");
      }

      // Determine if the driver is capable of batch updates
      if (batchUpdatesSupported) {
        // Read a line and add the insert table command to the batch
        while ((inLine = infile.readLine()) != null) {
          statement.addBatch(sqlInsert + inLine + ")");
        }

        statement.executeBatch();

        jlblStatus.setText("Batch updates completed");
      }
      else {
        // Read a line and execute insert table command
        while ((inLine = infile.readLine()) != null) {
          statement.executeUpdate(sqlInsert + inLine + ")");
        }

        jlblStatus.setText("Single row update completed");
      }
    }
    catch (SQLException ex) {
      System.out.println(ex);
    }
    catch (FileNotFoundException ex) {
      System.out.println("File not found: " + filename);
    }
    catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
    finally {
      try {
        if (infile != null) infile.close();
      }
      catch (IOException ex) {
        System.out.println(ex.getMessage());
      }
    }
  }

  public static void main(String args[]) {
    JFrame frame = new CopyFileToTable();
    frame.setTitle("CopyFileToTable");
    frame.setSize(700, 200);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
