// Modified Fig. 27.5: Multi-threaded Chat Server.java
// Server portion of a client/server stream-socket connection. 

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends JFrame {
    private ArrayList<UserInfo> users = new ArrayList<>();
    private ArrayList<String> transactions = new ArrayList<>();
    private JTextField enterField; // inputs message from user
    private JTextArea displayArea; // display information to user
    private ExecutorService executor; // will run players
    private ServerSocket server; // server socket
    private SockServer[] sockServer; // Array of objects to be threaded
    private int counter = 1; // counter of number of connections
    private int nClientsActive = 0;


    // set up GUI
    public Server() {
        super("Server");

        sockServer = new SockServer[100]; // allocate array for up to 100 server threads
        executor = Executors.newFixedThreadPool(100); // create thread pool

        enterField = new JTextField(); // create enterField
        enterField.setEditable(false);
        enterField.addActionListener(
                new ActionListener() {
                    // send message to client
                    public void actionPerformed(ActionEvent event) {
                        // Just got text from Server GUI Textfield
                        // Now send this to each client -- broadcast mode
                        for (int i = 1; i <= counter; i++) {
                            if (sockServer[i].alive)
                                sockServer[i].sendData(event.getActionCommand());
                        }
                        enterField.setText("");
                    } // end method actionPerformed
                } // end anonymous inner class
        ); // end call to addActionListener

        add(enterField, BorderLayout.NORTH);

        displayArea = new JTextArea(); // create displayArea
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        setSize(300, 150); // set size of window
        setVisible(true); // show window

        readData();
    } // end Server constructor

    // set up and run server
    public void runServer() {
        try // set up server to receive connections; process connections
        {
            server = new ServerSocket(23555, 100); // create ServerSocket
            while (true) {
                try {
                    //create a new runnable object to serve the next client to call in
                    sockServer[counter] = new SockServer(counter);
                    // make that new object wait for a connection on that new server object
                    sockServer[counter].waitForConnection();
                    nClientsActive++;
                    // launch that server object into its own new thread
                    executor.execute(sockServer[counter]);
                    // then, continue to create another object and wait (loop)

                } // end try
                catch (EOFException eofException) {
                    displayMessage("\nServer terminated connection");
                } // end catch
                finally {
                    ++counter;
                } // end finally
            } // end while
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    } // end method runServer

    // manipulates displayArea in the event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // updates displayArea
                    {
                        displayArea.append(messageToDisplay); // append message
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    // manipulates enterField in the event-dispatch thread
    private void setTextFieldEditable(final boolean editable) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // sets enterField's editability
                    {
                        enterField.setEditable(editable);
                    } // end method run
                }  // end inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method setTextFieldEditable

    private void readData()
    {

        try {
            String dataPath = "ZelleMoMe" + File.separator + "src" + File.separator + "data";
            String transactionPath = "ZelleMoMe" + File.separator + "src" + File.separator + "transaction";
            File file = new File(dataPath);
            File tranactionFile = new File(transactionPath);
            Scanner iostream = new Scanner(file);
            Scanner transactionStream = new Scanner(tranactionFile);
            while (transactionStream.hasNextLine())
            {
                String data = transactionStream.nextLine();
                transactions.add(data);
            }
            while (iostream.hasNext())
            {
                String data = iostream.next();
                while(data.contains(","))
                {
                    String [] words = new String[3];
                    words = data.split(",");
                    users.add(new UserInfo(words[0],words[1],Double.valueOf(words[2])));
                    data = "";
                    System.out.println(data);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            displayMessage("File not Found");
        }

    }

    /* This new Inner Class implements Runnable and objects instantiated from this.
     * class will become server threads each serving a different client
     */
    private class SockServer implements Runnable {
        private ObjectOutputStream output; // output stream to client
        private ObjectInputStream input; // input stream from client
        private Socket connection; // connection to client
        private int myConID;
        private boolean alive = false;

        public SockServer(int counterIn) {
            myConID = counterIn;
        }

        public void run() {
            try {
                alive = true;
                try {
                    getStreams(); // get input & output streams
                    processConnection(); // process connection
                    nClientsActive--;

                } // end try
                catch (EOFException eofException) {
                    displayMessage("\nServer" + myConID + " terminated connection");
                } finally {
                    closeConnection(); //  close connection
                }// end catch
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end try

        // wait for connection to arrive, then display connection info
        private void waitForConnection() throws IOException {
            displayMessage("Waiting for connection" + myConID + "\n");
            connection = server.accept(); // allow server to accept connection
            displayMessage("Connection " + myConID + " received from: " +
                    connection.getInetAddress().getHostName());
        } // end method waitForConnection

        private void getStreams() throws IOException {
            // set up output stream for objects
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush(); // flush output buffer to send header information

            // set up input stream for objects
            input = new ObjectInputStream(connection.getInputStream());

            displayMessage("\nGot I/O streams\n");
        } // end method getStreams

        // process connection with client
        private void processConnection() throws IOException {
            String message = "Connection " + myConID + " successful";
            sendData(message); // send connection successful message

            // enable enterField so server user can send messages
            setTextFieldEditable(true);

            do // process messages sent from client
            {
                try // read message and display it
                {
                    message = (String) input.readObject(); // read new message

                    String[] inputArray = message.split(",");
                    if(message.contains("getTransactions")){
                        String[] transactionName = message.split(",");
                        String usersTransactions = "";
                        for(String s : transactions){
                            if(s.contains(transactionName[1])){
                                usersTransactions = usersTransactions + s + "\n";
                            }
                        }
                        sendData(usersTransactions);

                    }

                    // if login or new user
                    if(inputArray[2].matches("Log In") || inputArray[2].matches("New User")){
                        userExists(message);
                    }
                    else if(message.equals("Logout")){
                        sendData("Logged Out");
                    }
                    // if the string coming in has deposit
                    else if(inputArray[2].equals("deposit"))
                    {
                        System.out.println("hello ");
                        //search users
                        for (int i=0;i<users.size();i++)
                        {
                            // find the username
                            if(users.get(i).getName().equals(inputArray[0]))
                            {
                                double balence = users.get(i).getBalance();
                                double updatedBalence = balence + Double.valueOf(inputArray[1]);
                                users.get(i).setBalance(updatedBalence);
                                System.out.println(users.get(i).getBalance());
                            }
                        }
                    }
                    //else use pay function
                    else
                    {
                        pay(message);
                    }
                    displayMessage("\n" + message); // display message.
                } // end try
                catch (ClassNotFoundException classNotFoundException) {
                    displayMessage("\nUnknown object type received");
                } // end catch

            } while (!message.equals("CLIENT>>> TERMINATE"));
        } // end method processConnection

        // close streams and socket
        private void closeConnection() {
            displayMessage("\nTerminating connection " + myConID + "\n");
            displayMessage("\nNumber of connections = " + nClientsActive + "\n");
            alive = false;
            if (nClientsActive == 0) {
                setTextFieldEditable(false); // disable enterField
            }

            try {
                outPutFile();
                output.close(); // close output stream
                input.close(); // close input stream
                connection.close(); // close socket
            } // end try
            catch (IOException ioException) {
                ioException.printStackTrace();
            } // end catch
        } // end method closeConnection

        private void sendData(String message) {
            try // send object to client
            {
                output.writeObject(message);
                output.flush(); // flush output to client
                displayMessage("\nSERVER" + myConID + ">>> " + message);
            } // end try
            catch (IOException ioException) {
                displayArea.append("\nError writing object");
            } // end catch
        } // end method sendData

        private void userExists(String message){
            System.out.println(message);
            String u;
            String p;
            String[] words = message.split(",");
            u = words[0];
            p = words[1];
            System.out.println(u);
            System.out.println(p);
            boolean exists =false;
            if(words[2].equals("New User"))
            {
                exists = checkUserExists(u);
                if(!exists)
                {
                    users.add(new UserInfo(u,p,Double.valueOf(words[3])));
                    sendData("New Account Created");
                }
            }
            if(words[2].equals("Log In")){
                for(UserInfo user : users){
                    if(user.getName().equals(u)){
                        if(user.getPassword().equals(p)){
                            sendData(user.getName()+","+user.getBalance()+",Valid Log In");
                        }else{
                            sendData("Incorrect Password");
                        }
                    }
                }
            }
        }
        private boolean checkUserExists(String u)
        {
            boolean exists = false;
            for(UserInfo user : users){
                if(user.getName().equals(u)){
                    sendData("Username already exists");
                    exists = true;
                }
            }
            return exists;
        }

        public void pay(String message){
            String[] words = message.split(",");
            String r = words[0];
            String m = words[1];
            String a = words[2];
            String u = words[3];

            int ct = 0; //keep track if recipient is in users
            UserInfo tempUser = null;
            for (UserInfo user : users) {
                if (user.getName().equals(u)) {
                    tempUser = user;
                    System.out.println(u);
                    user.setBalance(user.getBalance() - Double.valueOf(a));
                }
                if (user.getName().equals(r)) {
                    sendData("User Does Exist");
                    user.setBalance(user.getBalance() + Double.valueOf(a));
                    ct++;
                }
            }
            if(ct == 0){
                tempUser.setBalance(tempUser.getBalance() + Double.valueOf(a));
            }

            String transaction = u + " Paid " + r + ": " + a + " Memo: " + m;
            transactions.add(transaction);
            // put string for transaction in list

        }


        private void outPutFile() throws IOException
        {
            String content = "";
            for (int i=0;i<users.size();i++)
            {
                UserInfo temp = users.get(i);
                content = content + temp.getName() + "," +temp.getPassword() + "," + temp.getBalance() +"\n";
            }
            String transactionContent = "";
            for (int i=0;i<transactions.size();i++){
                transactionContent = transactionContent + transactions.get(i) + "\n";
            }
            String dataPath = "ZelleMoMe" + File.separator + "src" + File.separator + "data";
            String transactionPath = "ZelleMoMe" + File.separator + "src" + File.separator + "transaction";
            FileOutputStream outputStream = new FileOutputStream(dataPath);
            FileOutputStream outputTransaction = new FileOutputStream(transactionPath);
            byte[] strToBytes = content.getBytes();
            byte[] transactionBytes = transactionContent.getBytes();
            outputStream.write(strToBytes);
            outputTransaction.write(transactionBytes);

            outputStream.close();
            outputTransaction.close();
        }

    } // end class SockServer
} // end class Server

/**************************************************************************
 * (C) Copyright 1992-2012 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/