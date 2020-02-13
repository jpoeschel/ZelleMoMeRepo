public class UserInfo {
    final private String name;
    final private String password;
    private double balance;

    UserInfo(String n, String p, double b){
        name = n;
        password = p;
        balance = b;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

}
