package info.blockchain.merchant.service;

public interface WebSocketListener {
    void onIncomingPayment(String addr, long paymentAmount);
}
