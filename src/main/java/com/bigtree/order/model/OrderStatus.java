package com.bigtree.order.model;

public interface OrderStatus {
    public static final String Unpaid ="Unpaid";
    public static final String Open ="Open";
    public static final String In_Progress ="In Progress";
    public static final String Ready ="Ready";
    public static final String Out_For_Delivery ="Out for delivery";
    public static final String Collected ="Collected";
    public static final String Delivered ="Delivered";
    public static final String Refund_Started ="Refund Started";
    public static final String Refunded ="Refunded";
    public static final String Declined ="Declined";
    public static final String Cancelled ="Cancelled";
    public static final String Payment_Error ="Payment Error";

}
