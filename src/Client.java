// Fig. 27.7: Client.java
// Client portion of a stream-socket connection between client and server.

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends JFrame {
    private JTextField enterField = new JTextField(); // enters information from user
    private JPasswordField passwordField = new JPasswordField();
    private JTextField balance = new JTextField();
    private JPanel panel = new JPanel();
    private JButton logIn = new JButton("Log In");
    private JButton newUser = new JButton();
    private JLabel addMoney = new JLabel("Deposit Current Balance:");
    private String information;
    private String host;
    private String message = ""; // message from server
    private String chatServer; // host server for this application
    private Socket client; // socket to communicate with server
    private JTextArea displayArea = new JTextArea(); // display information to user
    private ObjectOutputStream output; // output stream to server
    private ObjectInputStream input; // input stream from server
    private String currentBalence;
    private boolean paymentUpdate;

    // initialize chatServer and set up GUI
    public Client(String host) {
        super("Client");

        this.host=host;

        chatServer = host; // set server to which this client connects

        // enterField = new JTextField(); // create enterField
        enterField.setPreferredSize(new Dimension(300,25));
        enterField.setEditable(false);
        passwordField.setPreferredSize(new Dimension(300,25));
        passwordField.setEditable(false);

        logIn.setPreferredSize(new Dimension(150,25));
        newUser.setPreferredSize(new Dimension(150,25));
        newUser.setText("Create Account");
        JLabel title = new JLabel("                                          ZelleMoMe Login                                               ");

        logIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                information = "";
                information = enterField.getText() + ","+ passwordField.getText() + "," + "Log In";
                sendData(information);
            }
        });
        newUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                information = "";
                information = enterField.getText() + ","+ passwordField.getText() + "," + "New User";
                newUserBalance();
            }
        });

        panel.add(title,BorderLayout.NORTH); //adds title label to the top
        panel.add(new JLabel("Username:")); // adds label for username text  box
        panel.add(enterField); // username text box
        panel.add(new JLabel("Password: ")); // adds label for password text box
        panel.add(passwordField); //adds password text box
        panel.add(newUser); // adds new Account button
        panel.add(logIn); // adds log in button
        panel.add(addMoney); // adds Jlabel for money text box
        balance.setPreferredSize(new Dimension(100, 25)); //sets size for balence text box
        balance.setEditable(false); // sets it to not editable
        panel.add(balance); //adds balence text box

        displayArea = new JTextArea(); // create displayArea
        displayArea.setPreferredSize(new Dimension(400,100)); //set size of displayArea
        add(new JScrollPane(displayArea), BorderLayout.SOUTH); // adds it to the bottom
        add(panel);

        setSize(400, 400); // set size of window
        setVisible(true); // show window
        setResizable(false);
        playMusic();
    } // end Client constructor

    // connect to server and process messages from server
    public void runClient() {
        try // connect to server, get streams, process connection
        {
            connectToServer(); // create a Socket to make connection
            getStreams(); // get the input and output streams
            processConnection(); // process connection
        } // end try
        catch (EOFException eofException) {
            displayMessage("\nClient terminated connection");
        } // end catch
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
        finally {
            closeConnection(); // close connection
        } // end finally
    } // end method runClient

    // connect to server
    private void connectToServer() throws IOException {
        displayMessage("Attempting connection\n");

        // create Socket to make connection to server
        client = new Socket(InetAddress.getByName(chatServer), 23555);

        // display connection information
        displayMessage("Connected to: " +
                client.getInetAddress().getHostName());
    } // end method connectToServer

    // get streams to send and receive data
    private void getStreams() throws IOException {
        // set up output stream for objects
        output = new ObjectOutputStream(client.getOutputStream());
        output.flush(); // flush output buffer to send header information

        // set up input stream for objects
        input = new ObjectInputStream(client.getInputStream());

        displayMessage("\nGot I/O streams\n");
    } // end method getStreams

    // process connection with server
    private void processConnection() throws IOException {
        // enable enterField so client user can send messages
        setTextFieldEditable(true);

        do // process messages sent from server
        {
            try // read message and display it
            {
                message = (String) input.readObject(); // read new message.
                if(message.contains("Valid Log In")){
                    String[] nameBalance;
                    nameBalance = message.split(",");
                    currentBalence = nameBalance[1];
                    new HomeScreen(nameBalance[0]);
                    dispose();
                }
                if(message.contains("User Does Exist")){
                    //currentBalence = String.valueOf(Double.valueOf(currentBalence) - paymentUpdate);
                    System.out.println("user exists");
                    paymentUpdate = true;
                    playChaChing();
                }
                displayMessage("\n" + message); // display message
            } // end try
            catch (ClassNotFoundException classNotFoundException) {
                displayMessage("\nUnknown object type received");
            } // end catch

        } while (!message.equals("SERVER>>> TERMINATE"));
    } // end method processConnection

    private void newUserBalance(){
        newUser.setEnabled(false);
        logIn.setEnabled(false);
        balance.setEditable(true);
        balance.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(!balance.getText().equals("")){
                    information = information + "," + balance.getText();
                    sendData(information);
                }
                newUser.setEnabled(true);
                logIn.setEnabled(true);
                balance.setEditable(false);
                balance.setText("");
                enterField.setText("");
                passwordField.setText("");
            }
        });
    }

    // close streams and socket
    private void closeConnection() {
        displayMessage("\nClosing connection");
        setTextFieldEditable(false); // disable enterField

        try {
            output.close(); // close output stream
            input.close(); // close input stream
            client.close(); // close socket
        } // end try
        catch (IOException ioException) {
            ioException.printStackTrace();
        } // end catch
    } // end method closeConnection

    // send message to server
    private void sendData(String message) {
        try // send object to server
        {
            output.writeObject(message);
            output.flush(); // flush data to output
            displayMessage("\n" + message);
        } // end try
        catch (IOException ioException) {
            displayArea.append("\nError writing object");
        } // end catch
    } // end method sendData

    // manipulates displayArea in the event-dispatch thread
    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // updates displayArea
                    {
                        displayArea.append(messageToDisplay);
                    } // end method run
                }  // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    // manipulates enterField in the event-dispatch thread
    private void setTextFieldEditable(final boolean editable) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() // sets enterField's editability
                    {
                        enterField.setEditable(editable);
                        passwordField.setEditable(editable);
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method setTextFieldEditable

    private void playChaChing()
    {
        try
        {
            File music = new File("C:\\softwareTeam18git\\ZelleMoMe\\src\\Money Sound Effect (Cha Ching).wav");
            displayMessage("playing");
            if(music.exists())
            {
                AudioInputStream input = AudioSystem.getAudioInputStream(music);
                Clip clip = AudioSystem.getClip();
                clip.open(input);
                clip.start();
            }
            else
            {
                displayMessage("Can't find file");
            }
        }
        catch (Exception e)
        {

        }
    }
    private void playMusic()
    {
        try
        {
            File music = new File("C:\\softwareTeam18git\\ZelleMoMe\\src\\Jingle Bells (instrumental - lyrics).wav");
            displayMessage("playing");
            if(music.exists())
            {
                AudioInputStream input = AudioSystem.getAudioInputStream(music);
                Clip clip = AudioSystem.getClip();
                clip.open(input);
                clip.start();
            }
            else
            {
                displayMessage("Can't find file");
            }
        }
        catch (Exception e)
        {

        }
    }

    private class PayScreen extends JFrame{
        private JTextField recipientField = new JTextField();
        private JTextField memoField = new JTextField();
        private JTextField amountField = new JTextField();
        Double balance;
        JLabel userBalance;

        public PayScreen(String name) {
            super("Account Details");
            this.balance = Double.valueOf(currentBalence);

            setSize(300, 300);
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridLayout(6, 1));

            JLabel username = new JLabel("User Name: " + name);
            userBalance = new JLabel("Balance: " + currentBalence);
            JLabel recipient = new JLabel("Recipient: ");
            recipientField.setPreferredSize(new Dimension(100, 25));
            JLabel memo = new JLabel("Memo: ");
            memoField.setPreferredSize(new Dimension(100, 25));
            JLabel amount = new JLabel("Amount: ");
            amountField.setPreferredSize(new Dimension(100, 25));
            JButton payButton = new JButton("Pay");
            JButton homeButton = new JButton("home");

            homeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new HomeScreen(name);
                    dispose();
                }
            });

            payButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (checkTextFields() && checkBalance()) {
                        String info = recipientField.getText() + "," + memoField.getText() + "," + amountField.getText() + "," + name;
                        sendData(info);
                        updateBalance(); //updates label
                        amountField.setText("");
                        recipientField.setText("");
                        memoField.setText("");
                    }
                }
            });

            mainPanel.add(username);
            mainPanel.add(userBalance);
            mainPanel.add(recipient);
            mainPanel.add(recipientField);
            mainPanel.add(memo);
            mainPanel.add(memoField);
            mainPanel.add(amount);
            mainPanel.add(amountField);
            mainPanel.add(payButton);
            mainPanel.add(homeButton);

            add(mainPanel);
            setVisible(true);
            setResizable(false);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        public String[] getInfo() {
            String[] info = new String[3];
            info[0] = recipientField.getText();
            info[1] = amountField.getText();
            info[2] = memoField.getText();
            return info;
        }

        public boolean checkTextFields() {
            if (recipientField.getText() != null && memoField.getText() != null && amountField.getText() != null) {
                return true;
            }
            return false;
        }

        public boolean checkBalance() {
            if(!amountField.getText().equals("")){
                if (Double.valueOf(amountField.getText()) <= Double.valueOf(currentBalence)) {
                    return true;
                }
            }
            return false;
        }

        public void updateBalance() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(paymentUpdate){
                currentBalence = String.valueOf(Double.valueOf(currentBalence) - Double.valueOf(amountField.getText()));
                paymentUpdate = false;
            }
            userBalance.setText("Balance: " + currentBalence);
            validate();
            repaint();
        }
    }

    private class HomeScreen extends JFrame
    {
        private JButton pay = new JButton("Money Transfer");
        private JPanel panel = new JPanel();
        private JButton deposit = new JButton("Deposit");
        private JButton transaction = new JButton("Transaction History");
        private JLabel zelleMoMe = new JLabel("ZelleMoMe Home");
        private JButton logout = new JButton("Logout");

        public HomeScreen(String name)
        {
            setSize(300, 300);
            panel.add(zelleMoMe);
            pay.setPreferredSize(new Dimension(200,50));
            deposit.setPreferredSize(new Dimension(200,50));
            transaction.setPreferredSize(new Dimension(200,50));
            logout.setPreferredSize(new Dimension(200,50));

            pay.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    PayScreen ms = new PayScreen(name);
                    ms.getInfo();
                    dispose();
                }
            });
            deposit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Deposit(name);
                    dispose();
                }
            });

            transaction.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendData("getTransactions,"+name);
                    new TransactionScreen(name);
                    dispose();
                }
            });

            logout.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

            panel.add(pay);
            panel.add(deposit);
            panel.add(transaction);
            panel.add(logout);
            add(panel);
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        }
    }

    private class Deposit extends JFrame
    {
        private JPanel panel = new JPanel();
        private JButton depositButton = new JButton("Deposit");
        private JButton homeButton = new JButton("Home");
        private JLabel currentBal = new JLabel();
        private JTextField depositField = new JTextField("");

        public Deposit(String name)
        {
            setSize(300, 300);
            depositButton.setPreferredSize(new Dimension(100,50));
            depositField.setPreferredSize(new Dimension(200,25));
            homeButton.setPreferredSize(new Dimension(100,50));
            currentBal.setPreferredSize(new Dimension(200,50));
            currentBal.setText("Current Balence: " + currentBalence);

            homeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    new HomeScreen(name);
                    dispose();
                }
            });
            depositButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(depositField.getText().matches("[0-9]+"))
                    {
                        double money = Double.valueOf(depositField.getText()) + Double.valueOf(currentBalence);
                        String info = name + "," + depositField.getText() + "," + "deposit";
                        sendData(info);
                        currentBal.setText("Current Balence: " + String.valueOf(money));
                        depositField.setText("");
                        playMusic();
                    }
                    else
                    {
                        depositField.setText("Please Enter Number");
                    }
                }
            });

            panel.add(currentBal);
            panel.add(depositField);
            panel.add(depositButton);
            panel.add(homeButton);
            add(panel);
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }
    private class TransactionScreen extends JFrame{
        private JPanel panel = new JPanel();
        private JLabel header = new JLabel("Transactions");
        private JButton homeButton = new JButton("Home");

        public TransactionScreen(String name){
            setSize(300, 300);
            displayArea = new JTextArea();
            panel.add(header);
            String[] transactionsSplit = message.split("\n");
            for (int i=1;i<transactionsSplit.length;i++)
            {
                displayMessage(transactionsSplit[i]);
            }
            displayArea.setPreferredSize(new Dimension(300,300));

            homeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    new HomeScreen(name);
                    dispose();
                }
            });
            panel.add(homeButton);
            panel.add(displayArea);
            add(panel);
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setResizable(false);
        }

    }
} // end class Client

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
