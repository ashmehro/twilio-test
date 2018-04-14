package twilio;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PaymentUser {

    //package private variables so once the API is exposed, nobody should be able to add code in the package
    List<String> sendTxns;
    List<String> receiveTxns;


    //balance and bankBalance are updated with real txns
    AtomicInteger balance;
    AtomicInteger bankBalance;
    int id;
    String userName;

    //these are all for pending txns
    AtomicInteger toSend;
    AtomicInteger toReceive;
    Map<Integer, Integer> pendingPayments;
    Map<Integer, Integer> pendingReceipts;

    Lock lock;

    public PaymentUser(String name) {
        pendingPayments = Collections.synchronizedMap(new LinkedHashMap<Integer, Integer>());
        pendingReceipts = Collections.synchronizedMap(new LinkedHashMap<Integer, Integer>());

        sendTxns = new ArrayList<>();
        receiveTxns = new ArrayList<>();
        id = UserIdGenerator.getUserId();
        userName = name;

        balance = new AtomicInteger(0);
        bankBalance = new AtomicInteger(0);

        toSend = new AtomicInteger(0);
        toReceive = new AtomicInteger(0);
        lock = new ReentrantLock();
    }

    //one transaction adding current user as well as second user's maps too
    //dont update balance
    public void toSend(int amount, PaymentUser user) {
        synchronized (lock) {
            synchronized (user.lock) {
                pendingPayments.put(user.id, pendingPayments.getOrDefault(user.id, 0) + amount);
                user.pendingReceipts.put(this.id, user.pendingReceipts.getOrDefault(this.id, 0) + amount);
                toSend.getAndAdd(amount);
                user.toReceive.getAndAdd(amount);
            }
        }
    }

    public void toReceive(int amount, PaymentUser user) {
        synchronized (lock) {
            synchronized (user.lock) {
                pendingReceipts.put(user.id, pendingReceipts.getOrDefault(user.id, 0) + amount);
                user.pendingPayments.put(this.id, user.pendingPayments.getOrDefault(this.id, 0) + amount);
                toReceive.getAndAdd(amount);
                user.toSend.getAndAdd(amount);
            }
        }
    }

    public void sendMoney(int amount, PaymentUser user) {
        synchronized (lock) {
            synchronized (user.lock) {
                if (pendingPayments.containsKey(user.id)) {
                    if (pendingPayments.get(user.id) == amount) {
                        pendingPayments.remove(user.id);
                    } else
                        pendingPayments.put(user.id, pendingPayments.get(user.id) - amount); //can send money more than due
                }
                //real txn, update the balance
                balance.getAndAdd(-amount);
                user.balance.getAndAdd(amount);
            }
        }
        sendTxns.add("Amount " + amount + " sent to user " + user.userName + " on " + TimeProvider.getInstance().getDateAndTime());
    }

    public void receiveMoney(int amount, PaymentUser user) {
        synchronized (lock) {
            synchronized (user.lock) {
                if (pendingPayments.containsKey(user.id)) {
                    if (pendingPayments.get(user.id) == amount) {
                        pendingPayments.remove(user.id);
                    } else
                        pendingPayments.put(user.id, pendingPayments.get(user.id) - amount); //can send money more than due
                }

                balance.getAndAdd(amount);
                user.balance.getAndAdd(-amount);
            }
        }
        receiveTxns.add("Amount " + amount + " received from user " + user.userName + " on " + TimeProvider.getInstance().getDateAndTime());
    }

    public void showNPayments(int n) {
        int min = Math.min(sendTxns.size(), n);
        for (int i=min-1; i >=0; i++) {
            System.out.println(sendTxns.get(i));
        }
    }

    public void showNReceipts(int n) {
        int min = Math.min(receiveTxns.size(), n);
        for (int i=min-1; i >=0; i++) {
            System.out.println(receiveTxns.get(i));
        }
    }

    public void getFromBank(int amount) {
        synchronized (lock) {
            if (amount < bankBalance.get()) {
                balance.getAndAdd(amount);
                bankBalance.getAndAdd(-amount);
            }
        }
    }

    public  void showBalance() {
        int total = 0;
        synchronized (this.lock) {
            total = balance.get() + bankBalance.get() + toReceive.get() - toSend.get();
        }
        System.out.println("Total: " + total);
    }

    public void sendToBank(int amount) {
        synchronized (lock) {
            if (amount < balance.get()) {
                balance.getAndAdd(-amount);
                bankBalance.getAndAdd(amount);
            }
        }
    }

    public static void main(String[] args) {

        PaymentUser a = new PaymentUser("A");
        a.balance.getAndAdd(5000);
        PaymentUser b = new PaymentUser("B");
        b.balance.getAndAdd(2000);

        a.sendMoney(100, b);
        a.showBalance();
        b.showBalance();

    }


}
